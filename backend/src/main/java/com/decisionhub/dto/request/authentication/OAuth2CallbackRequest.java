package com.decisionhub.dto.request.authentication;

import jakarta.validation.constraints.NotBlank;

public record OAuth2CallbackRequest(
        @NotBlank(message = "Authorization code is required")
        String code,
        String state,
        String redirectUri
) {}
