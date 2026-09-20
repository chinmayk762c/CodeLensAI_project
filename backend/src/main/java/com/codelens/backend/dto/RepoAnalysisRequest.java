package com.codelens.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RepoAnalysisRequest(
        @NotBlank(message = "Repository URL is required")
        String repoUrl
) {}