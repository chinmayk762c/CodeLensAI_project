package com.codelens.backend.dto;

public record DocstringResponse(
        Long submissionId,
        String documentedCode
) {}