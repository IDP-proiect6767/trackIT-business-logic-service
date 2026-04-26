package com.trackit.business.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.business.client.AuthServiceClient;
import com.trackit.business.dto.ErrorResponse;
import com.trackit.business.dto.auth.AuthValidationResponse;
import com.trackit.business.exception.AuthServiceUnavailableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthServiceClient authServiceClient;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(AuthServiceClient authServiceClient, ObjectMapper objectMapper) {
        this.authServiceClient = authServiceClient;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        Optional<AuthValidationResponse> validation;
        try {
            validation = authServiceClient.validate(token);
        } catch (AuthServiceUnavailableException e) {
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorResponse.Codes.AUTH_SERVICE_UNAVAILABLE,
                    "Authentication service is currently unavailable. Please try again.");
            return;
        }

        if (validation.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        AuthValidationResponse v = validation.get();
        AuthenticatedUser principal = new AuthenticatedUser(v.userId(), v.username(), v.role());
        var authority = new SimpleGrantedAuthority("ROLE_" + (v.role() == null ? "USER" : v.role()));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(code, message));
    }
}
