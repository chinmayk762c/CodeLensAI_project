package com.codelens.backend.analysis;

import com.codelens.backend.entity.IssueCategory;
import com.codelens.backend.entity.IssueSeverity;

public record RawIssue(
        IssueSeverity severity,
        IssueCategory category,
        String description,
        Integer lineNumber,
        String suggestion
) {}