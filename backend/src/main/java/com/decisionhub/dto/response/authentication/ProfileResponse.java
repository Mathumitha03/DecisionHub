package com.decisionhub.dto.response.authentication;

import com.decisionhub.enums.authentication.PlatformRole;
import com.decisionhub.enums.authentication.UserStatus;

public record ProfileResponse(
        Long id,
        String username,
        String email,
        PlatformRole role,
        UserStatus status,
        String avatarUrl
) {
    public ProfileResponse(Long id, String username, String email, PlatformRole role, UserStatus status) {
        this(id, username, email, role, status, null);
    }
}
