package com.codelens.backend.ai;

import com.codelens.backend.analysis.RawIssue;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiReviewService {

    private final ChatClient chatClient;

    private final JsonMapper lenientMapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();

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

                Respond with ONLY a single valid JSON object, no markdown code fences, no extra text before or after, exactly matching this shape:
                {
                  "summary": "string",
                  "additionalIssues": [
                    {"severity": "LOW|MEDIUM|HIGH|CRITICAL", "category": "BUG|CODE_SMELL|SECURITY|PERFORMANCE|STYLE", "description": "string", "lineNumber": number or null}
                  ],
                  "optimizedCode": "the full optimized code as one string, with real newlines properly escaped as \\n"
                }

                Code to review:
            %s
                """.formatted(issuesSummary.isBlank() ? "(none)" : issuesSummary, code);

        String raw = chatClient.prompt().user(prompt).call().content();
        String cleaned = stripCodeFences(raw);

        try {
            return lenientMapper.readValue(cleaned, AiReviewResult.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }

    private String stripCodeFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\n", "");
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}