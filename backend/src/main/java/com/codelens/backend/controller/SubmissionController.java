package com.codelens.backend.controller;

import com.codelens.backend.dto.SubmissionRequest;
import com.codelens.backend.dto.SubmissionResponse;
import com.codelens.backend.service.SubmissionService;
import jakarta.validation.Valid;
import com.codelens.backend.dto.SubmissionSummaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<SubmissionResponse> create(
            @Valid @RequestBody SubmissionRequest request,
            Authentication authentication
    ) {
        SubmissionResponse response = submissionService.createSubmission(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping
    public ResponseEntity<List<SubmissionSummaryResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(submissionService.listSubmissions(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            SubmissionResponse response = submissionService.getSubmission(authentication.getName(), id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        try {
            submissionService.deleteSubmission(authentication.getName(), id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}