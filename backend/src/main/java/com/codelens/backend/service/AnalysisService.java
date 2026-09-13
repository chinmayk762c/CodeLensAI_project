package com.codelens.backend.service;

import com.codelens.backend.ai.AiIssueDto;
import com.codelens.backend.ai.AiReviewResult;
import com.codelens.backend.ai.AiReviewService;
import com.codelens.backend.analysis.CheckstyleRunner;
import com.codelens.backend.analysis.CodeFileWriter;
import com.codelens.backend.analysis.JavaCompilerService;
import com.codelens.backend.analysis.PmdRunner;
import com.codelens.backend.analysis.RawIssue;
import com.codelens.backend.analysis.SpotBugsRunner;
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
    private final ScoringService scoringService;
    private final CheckstyleRunner checkstyleRunner;
    private final JavaCompilerService javaCompilerService;
    private final SpotBugsRunner spotBugsRunner;
    private final AiReviewService aiReviewService;

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

            try {
                rawIssues.addAll(pmdRunner.run(written.javaFile()));
            } catch (Exception pmdException) {
                rawIssues.add(new RawIssue(
                        IssueSeverity.HIGH,
                        IssueCategory.BUG,
                        "PMD could not parse this code — it likely contains a syntax error. PMD error: " + pmdException.getMessage(),
                        null,
                        null
                ));
            }

            try {
                rawIssues.addAll(checkstyleRunner.run(written.javaFile()));
            } catch (Exception checkstyleException) {
                rawIssues.add(new RawIssue(
                        IssueSeverity.HIGH,
                        IssueCategory.BUG,
                        "Checkstyle could not parse this code — it likely contains a syntax error. Checkstyle error: " + checkstyleException.getMessage(),
                        null,
                        null
                ));
            }

            JavaCompilerService.CompileResult compileResult = javaCompilerService.compile(written.javaFile());
            if (compileResult.success()) {
                try {
                    rawIssues.addAll(spotBugsRunner.run(compileResult.classOutputDir()));
                } finally {
                    codeFileWriter.cleanup(compileResult.classOutputDir());
                }
            } else {
                rawIssues.add(new RawIssue(
                        IssueSeverity.HIGH,
                        IssueCategory.BUG,
                        "Code does not compile — SpotBugs analysis skipped. Compiler output: "
                                + compileResult.errorOutput().trim(),
                        null,
                        null
                ));
            }

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

            try {
                AiReviewResult aiResult = aiReviewService.review(submission.getCode(), rawIssues);
                report.setAiSummary(aiResult.summary());
                report.setOptimizedCode(aiResult.optimizedCode());

                if (aiResult.additionalIssues() != null) {
                    for (AiIssueDto aiIssue : aiResult.additionalIssues()) {
                        Issue issue = new Issue();
                        issue.setReport(report);
                        issue.setSeverity(parseSeverityOrDefault(aiIssue.severity()));
                        issue.setCategory(parseCategoryOrDefault(aiIssue.category()));
                        issue.setDescription("[AI] " + aiIssue.description());
                        issue.setLineNumber(aiIssue.lineNumber());
                        savedIssues.add(issueRepository.save(issue));
                    }
                }
            } catch (Exception aiException) {
                aiException.printStackTrace();
                report.setAiSummary("AI review unavailable: " + aiException.getMessage());
            }

            int score = scoringService.calculateScore(savedIssues);
            report.setOverallScore(score);
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

    private IssueSeverity parseSeverityOrDefault(String value) {
        try {
            return IssueSeverity.valueOf(value);
        } catch (Exception e) {
            return IssueSeverity.MEDIUM;
        }
    }

    private IssueCategory parseCategoryOrDefault(String value) {
        try {
            return IssueCategory.valueOf(value);
        } catch (Exception e) {
            return IssueCategory.BUG;
        }
    }

    private AnalysisReportResponse toResponse(AnalysisReport report, List<Issue> issues) {
        List<IssueResponse> issueResponses = issues.stream()
                .map(i -> new IssueResponse(
                        i.getId(), i.getSeverity(), i.getCategory(),
                        i.getDescription(), i.getLineNumber(), i.getSuggestion()
                ))
                .toList();

                boolean qualityGatePassed = report.getOverallScore() != null
                && scoringService.passesQualityGate(report.getOverallScore());

        return new AnalysisReportResponse(
                report.getId(),
                report.getSubmission().getId(),
                report.getStatus(),
                report.getOverallScore(),
                qualityGatePassed,
                report.getAiSummary(),
                report.getOptimizedCode(),
                report.getCreatedAt(),
                issueResponses
        );
    }
    public AnalysisReportResponse getLatestReport(String userEmail, Long submissionId) {
        CodeSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found"));

        if (!submission.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You do not have access to this submission");
        }

        AnalysisReport report = reportRepository.findTopBySubmission_IdOrderByCreatedAtDesc(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("No analysis report found for this submission"));

        List<Issue> issues = issueRepository.findByReport_Id(report.getId());

        return toResponse(report, issues);
    }
}