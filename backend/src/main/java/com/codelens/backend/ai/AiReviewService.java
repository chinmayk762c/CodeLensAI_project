package com.codelens.backend.ai;

import com.codelens.backend.analysis.RawIssue;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiReviewService {

    private final ChatClient chatClient;

    public AiReviewResult review(String code, List<RawIssue> staticIssues) {
        String issuesSummary = staticIssues.stream()
                .map(i -> "- [" + i.severity() + "/" + i.category() + "] " + i.description())
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are a senior Java code reviewer. Review the following Java code.

                Static analysis tools already found these issues:
                %s

                Your job:
                1. Write a short (2-3 sentence) plain-English summary of the code's overall quality.
                2. Identify any ADDITIONAL issues the static tools missed above, especially security vulnerabilities or logic bugs. Do not repeat issues already listed.
                3. Provide an optimized, corrected version of the full code.

                For each additional issue: severity must be exactly one of LOW, MEDIUM, HIGH, CRITICAL. Category must be exactly one of BUG, CODE_SMELL, SECURITY, PERFORMANCE, STYLE.

                Code to review:             %s
                """.formatted(issuesSummary.isBlank() ? "(none)" : issuesSummary, code);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(AiReviewResult.class);
    }
}