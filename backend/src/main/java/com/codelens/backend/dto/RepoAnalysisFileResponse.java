package com.codelens.backend.dto;

public record RepoAnalysisFileResponse(
        String filePath,
        Long submissionId,
        String errorMessage
) {}