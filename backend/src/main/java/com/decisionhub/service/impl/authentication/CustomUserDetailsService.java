package com.decisionhub.service.impl.authentication;

import com.decisionhub.entity.authentication.User;
import com.decisionhub.enums.authentication.UserStatus;
import com.decisionhub.repository.authentication.UserRepository;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private com.decisionhub.service.interfaces.authentication.BlockedEmailService blockedEmailService;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setBlockedEmailService(com.decisionhub.service.interfaces.authentication.BlockedEmailService blockedEmailService) {
        this.blockedEmailService = blockedEmailService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        if (blockedEmailService != null && blockedEmailService.isBlocked(email)) {
            throw new org.springframework.security.authentication.DisabledException(
                    "This email address has been permanently blocked from the platform");
        }

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new org.springframework.security.authentication.LockedException(
                    "Your account has been blocked by an administrator");
        }

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null)
                .accountLocked(user.getStatus() == UserStatus.BLOCKED)
                .build();
    }
}
