package com.codelens.backend.dto;

import com.codelens.backend.entity.IssueCategory;
import com.codelens.backend.entity.IssueSeverity;

public record IssueResponse(
        Long id,
        IssueSeverity severity,
        IssueCategory category,
        String description,
        Integer lineNumber,
        String suggestion
) {}