package com.codelens.backend.dto;

public record ExecuteResponse(String output, boolean timedOut, boolean compileFailed, Integer exitCode) {}