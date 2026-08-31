package com.codelens.backend.service;

import com.codelens.backend.dto.SubmissionRequest;
import com.codelens.backend.dto.SubmissionResponse;
import com.codelens.backend.entity.CodeSubmission;
import com.codelens.backend.entity.User;
import com.codelens.backend.repository.CodeSubmissionRepository;
import com.codelens.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final CodeSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public SubmissionResponse createSubmission(String userEmail, SubmissionRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        CodeSubmission submission = new CodeSubmission();
        submission.setUser(user);
        submission.setLanguage(request.language());
        submission.setCode(request.code());

        submissionRepository.save(submission);

        return toResponse(submission);
    }

    public SubmissionResponse getSubmission(String userEmail, Long id) {
        CodeSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found"));

        if (!submission.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You do not have access to this submission");
        }

        return toResponse(submission);
    }

    private SubmissionResponse toResponse(CodeSubmission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getLanguage(),
                submission.getCode(),
                submission.getCreatedAt()
        );
    }
}