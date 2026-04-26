package com.trackit.business.security;

public record AuthenticatedUser(Long id, String username, String role) {}
