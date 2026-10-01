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
                You are reviewing a piece of code the way a sharp senior engineer reviews a solution: you judge whether it correctly and efficiently does what it's trying to do.

                Static analysis tools already found these cosmetic/style issues (ignore these, do not repeat them):
                %s

                Your job:
                1. In 2-3 sentences, say what the code appears to be trying to accomplish, and whether it succeeds.
                2. State the code's time and space complexity (Big-O), and briefly say whether that's reasonable for the problem or could be better.
                3. List any additional problems: logic bugs, incorrect edge-case handling, inefficient approach, unnecessary complexity. Describe ONLY the problem and why it's a problem — never suggest the fix or the correct approach. No hints, no solutions, just diagnosis.
                4. Provide an optimized version of the code: same scale and style as the original (do not add package declarations, class-level Javadoc, or enterprise structure unless the original already had it), focused specifically on correctness and improving time/space complexity where possible. If the original is already optimal, say so in the summary and return the original code with only real bugs fixed.

                Respond with ONLY a single valid JSON object, no markdown code fences, no extra text before or after, exactly matching this shape:
                {
                  "summary": "string",
                  "complexity": "e.g. Time: O(n), Space: O(1) — one sentence on whether that's good enough",
                  "additionalIssues": [
                    {"severity": "LOW|MEDIUM|HIGH|CRITICAL", "category": "BUG|CODE_SMELL|SECURITY|PERFORMANCE|STYLE", "description": "string, diagnosis only, no fix suggested", "lineNumber": number or null}
                  ],
                  "optimizedCode": "the full optimized code as one string, with real newlines properly escaped as \\n"
                }

                Code to review:
            %s
                                """.formatted(issuesSummary.isBlank() ? "(none)" : issuesSummary, code);

        return callWithRetry(prompt, 2);
    }

    private AiReviewResult callWithRetry(String prompt, int attemptsLeft) {
        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            String cleaned = stripCodeFences(raw);
            return lenientMapper.readValue(cleaned, AiReviewResult.class);
        } catch (Exception e) {
            if (attemptsLeft > 0) {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                return callWithRetry(prompt, attemptsLeft - 1);
            }
            throw new RuntimeException("Failed to get AI response after retries: " + e.getMessage(), e);
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