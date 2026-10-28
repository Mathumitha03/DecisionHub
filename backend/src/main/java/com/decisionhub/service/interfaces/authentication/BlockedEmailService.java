package com.decisionhub.service.interfaces.authentication;

import com.decisionhub.entity.authentication.BlockedEmail;

public interface BlockedEmailService {

    BlockedEmail blockEmail(String email, String reason, String blockedBy);

    boolean isBlocked(String email);

    void validateNotBlocked(String email);
}
