package com.codelens.backend.service;

import com.codelens.backend.analysis.CheckstyleRunner;
import com.codelens.backend.analysis.CodeFileWriter;
import com.codelens.backend.analysis.PmdRunner;
import com.codelens.backend.analysis.RawIssue;
import com.codelens.backend.dto.AnalysisReportResponse;
import com.codelens.backend.dto.IssueResponse;
import com.codelens.backend.entity.*;
import com.codelens.backend.repository.AnalysisReportRepository;
import com.codelens.backend.repository.CodeSubmissionRepository;
import com.codelens.backend.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final CodeSubmissionRepository submissionRepository;
    private final AnalysisReportRepository reportRepository;
    private final IssueRepository issueRepository;
    private final CodeFileWriter codeFileWriter;
    private final PmdRunner pmdRunner;
    private final CheckstyleRunner checkstyleRunner;

    public AnalysisReportResponse analyze(String userEmail, Long submissionId) {
        CodeSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found"));

        if (!submission.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You do not have access to this submission");
        }

        AnalysisReport report = new AnalysisReport();
        report.setSubmission(submission);
        report.setStatus(AnalysisStatus.IN_PROGRESS);
        reportRepository.save(report);

        CodeFileWriter.WrittenFile written = null;
        try {
            written = codeFileWriter.write(submission.getCode());

            List<RawIssue> rawIssues = new ArrayList<>();
            rawIssues.addAll(pmdRunner.run(written.javaFile()));
            rawIssues.addAll(checkstyleRunner.run(written.javaFile()));

            List<Issue> savedIssues = new ArrayList<>();
            for (RawIssue raw : rawIssues) {
                Issue issue = new Issue();
                issue.setReport(report);
                issue.setSeverity(raw.severity());
                issue.setCategory(raw.category());
                issue.setDescription(raw.description());
                issue.setLineNumber(raw.lineNumber());
                issue.setSuggestion(raw.suggestion());
                savedIssues.add(issueRepository.save(issue));
            }

            report.setStatus(AnalysisStatus.COMPLETED);
            reportRepository.save(report);

            return toResponse(report, savedIssues);

        } catch (Exception e) {
            report.setStatus(AnalysisStatus.FAILED);
            reportRepository.save(report);
            throw new RuntimeException("Analysis failed: " + e.getMessage(), e);
        } finally {
            if (written != null) {
                codeFileWriter.cleanup(written.directory());
            }
        }
    }

    private AnalysisReportResponse toResponse(AnalysisReport report, List<Issue> issues) {
        List<IssueResponse> issueResponses = issues.stream()
                .map(i -> new IssueResponse(
                        i.getId(), i.getSeverity(), i.getCategory(),
                        i.getDescription(), i.getLineNumber(), i.getSuggestion()
                ))
                .toList();

        return new AnalysisReportResponse(
                report.getId(),
                report.getSubmission().getId(),
                report.getStatus(),
                report.getOverallScore(),
                report.getCreatedAt(),
                issueResponses
        );
    }
}