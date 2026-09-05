package com.codelens.backend.ai;

import com.codelens.backend.dto.DocstringResponse;
import com.codelens.backend.entity.CodeSubmission;
import com.codelens.backend.repository.CodeSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocstringService {

    private final CodeSubmissionRepository submissionRepository;
    private final ChatClient chatClient;

    public DocstringResponse generate(String userEmail, Long submissionId) {
        CodeSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found"));

        if (!submission.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You do not have access to this submission");
        }

        String prompt = """
                You are a Java documentation assistant. Add complete Javadoc comments to the class and every method in the following code, plus brief inline comments for any non-obvious logic.
                Do not change the code's behavior or logic in any way — only add documentation.
                Return ONLY the fully documented Java code, with no explanation, no markdown code fences, and no extra text before or after the code.

                Code:
                %s
                """.formatted(submission.getCode());

        String documentedCode = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return new DocstringResponse(submission.getId(), stripCodeFences(documentedCode));
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