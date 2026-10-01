package com.codelens.backend.ai;

import java.util.List;

public record AiReviewResult(
        String summary,
        String complexity,
        List<AiIssueDto> additionalIssues,
        String optimizedCode
) {}