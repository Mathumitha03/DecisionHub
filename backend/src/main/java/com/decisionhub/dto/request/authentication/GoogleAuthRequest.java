package com.decisionhub.dto.request.authentication;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank(message = "ID token is required")
        String idToken
) {}
