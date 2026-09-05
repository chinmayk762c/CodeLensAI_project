package com.codelens.backend.ai;

import java.util.List;

public record AiReviewResult(
        String summary,
        List<AiIssueDto> additionalIssues,
        String optimizedCode
) {}