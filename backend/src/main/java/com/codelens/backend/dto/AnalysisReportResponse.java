package com.codelens.backend.dto;

import com.codelens.backend.entity.AnalysisStatus;

import java.time.Instant;
import java.util.List;

public record AnalysisReportResponse(
        Long id,
        Long submissionId,
        AnalysisStatus status,
        Integer overallScore,
        boolean qualityGatePassed,
        Double coveragePercentage,
        String aiSummary,
        String optimizedCode,
        Instant createdAt,
        List<IssueResponse> issues
) {}