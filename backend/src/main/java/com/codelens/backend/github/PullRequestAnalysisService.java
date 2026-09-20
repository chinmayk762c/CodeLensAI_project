package com.codelens.backend.github;

import com.codelens.backend.dto.SubmissionRequest;
import com.codelens.backend.dto.SubmissionResponse;
import com.codelens.backend.dto.AnalysisReportResponse;
import com.codelens.backend.entity.Language;
import com.codelens.backend.service.AnalysisService;
import com.codelens.backend.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PullRequestAnalysisService {

    @Value("${github.system-user-email}")
    private String systemUserEmail;

    private final GitHubAppAuth appAuth;
    private final GitHubApiClient apiClient;
    private final SubmissionService submissionService;
    private final AnalysisService analysisService;

    @Async
    public void analyzeAndReport(long installationId, String owner, String repo, int prNumber, String headSha) {
        try {
            String token = appAuth.getInstallationToken(installationId);
            long checkRunId = apiClient.createCheckRun(token, owner, repo, headSha);

            List<GitHubApiClient.ChangedFile> files = apiClient.getChangedJavaFiles(token, owner, repo, prNumber);

            if (files.isEmpty()) {
                apiClient.completeCheckRun(token, owner, repo, checkRunId, true, "No Java files changed in this PR.");
                return;
            }

            StringBuilder summary = new StringBuilder();
            boolean overallPassed = true;
            int analyzed = 0;

            for (GitHubApiClient.ChangedFile file : files.stream().limit(10).toList()) {
                try {
                    String content = apiClient.fetchRawContent(token, file.rawUrl());
                    SubmissionResponse submission = submissionService.createSubmission(
                            systemUserEmail, new SubmissionRequest(Language.JAVA, content, null));
                    AnalysisReportResponse report = analysisService.analyze(systemUserEmail, submission.id());

                    analyzed++;
                    if (!report.qualityGatePassed()) overallPassed = false;

                    summary.append("**").append(file.path()).append("** — Score: ")
                            .append(report.overallScore()).append("/100 (")
                            .append(report.qualityGatePassed() ? "PASS" : "FAIL").append(")\n");
                } catch (Exception fileError) {
                    summary.append("**").append(file.path()).append("** — analysis error\n");
                }
            }

            summary.insert(0, "Analyzed " + analyzed + " file(s).\n\n");
            apiClient.completeCheckRun(token, owner, repo, checkRunId, overallPassed, summary.toString());

        } catch (Exception e) {
            System.err.println("[GITHUB] PR analysis failed: " + e.getMessage());
        }
    }
}