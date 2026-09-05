package com.codelens.backend.ai;

public record AiIssueDto(
        String severity,
        String category,
        String description,
        Integer lineNumber
) {}