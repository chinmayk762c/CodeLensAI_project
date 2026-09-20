package com.codelens.backend.dto;

import java.time.Instant;
import java.util.List;

public record RepoAnalysisResponse(
        Long id,
        String repoUrl,
        String status,
        Instant createdAt,
        List<RepoAnalysisFileResponse> files
) {}