package com.codelens.backend.dto;

import com.codelens.backend.entity.AnalysisStatus;
import com.codelens.backend.entity.Language;

import java.time.Instant;

public record SubmissionSummaryResponse(
        Long id,
        Language language,
        String codePreview,
        Instant createdAt,
        Integer latestScore,
        AnalysisStatus latestStatus
) {}