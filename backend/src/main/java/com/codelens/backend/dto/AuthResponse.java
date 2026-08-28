package com.codelens.backend.dto;

public record AuthResponse(
        String token,
        String fullName,
        String email
) {}