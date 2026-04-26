package com.trackit.business.dto.auth;

public record AuthValidationResponse(
        boolean valid,
        Long userId,
        String username,
        String role,
        Long expiresAt
) {}
