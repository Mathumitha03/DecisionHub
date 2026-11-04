package com.decisionhub.dto.response.authentication;

import com.decisionhub.enums.authentication.PlatformRole;
import com.decisionhub.enums.authentication.UserStatus;

import java.time.LocalDateTime;

public record BlockedUserResponse(
        Long id,
        String username,
        String email,
        String avatarUrl,
        PlatformRole role,
        UserStatus status,
        LocalDateTime blockedAt,
        String blockedBy
) {}
