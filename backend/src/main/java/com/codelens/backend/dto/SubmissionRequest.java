package com.codelens.backend.dto;

import com.codelens.backend.entity.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmissionRequest(
        @NotNull(message = "Language is required")
        Language language,

        @NotBlank(message = "Code cannot be empty")
        String code
) {}