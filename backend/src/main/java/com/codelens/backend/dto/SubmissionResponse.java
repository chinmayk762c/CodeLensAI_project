package com.codelens.backend.dto;

import com.codelens.backend.entity.Language;

import java.time.Instant;

public record SubmissionResponse(
        Long id,
        Language language,
        String code,
        String testCode,
        Instant createdAt
) {}