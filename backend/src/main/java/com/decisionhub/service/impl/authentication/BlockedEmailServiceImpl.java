package com.decisionhub.service.impl.authentication;

import com.decisionhub.entity.authentication.BlockedEmail;
import com.decisionhub.exception.BlockedEmailException;
import com.decisionhub.repository.authentication.BlockedEmailRepository;
import com.decisionhub.service.interfaces.authentication.BlockedEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BlockedEmailServiceImpl implements BlockedEmailService {

    private final BlockedEmailRepository blockedEmailRepository;

    @Override
    @Transactional
    public BlockedEmail blockEmail(String email, String reason, String blockedBy) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        String normalizedEmail = email.trim().toLowerCase();
        return blockedEmailRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> {
                    BlockedEmail blocked = new BlockedEmail();
                    blocked.setEmail(normalizedEmail);
                    blocked.setReason(reason != null ? reason : "ADMIN_ACTION");
                    blocked.setBlockedBy(blockedBy != null ? blockedBy : "ADMIN");
                    blocked.setBlockedAt(LocalDateTime.now());
                    return blockedEmailRepository.save(blocked);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return blockedEmailRepository.existsByEmailIgnoreCase(email.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public void validateNotBlocked(String email) {
        if (isBlocked(email)) {
            throw new BlockedEmailException("This email address has been permanently blocked from the platform");
        }
    }

    @Override
    @Transactional
    public void unblockEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            blockedEmailRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                    .ifPresent(blockedEmailRepository::delete);
        }
    }
}
