package com.decisionhub.service.impl.authentication;

import com.decisionhub.dto.response.authentication.UserResponse;
import com.decisionhub.entity.authentication.User;
import com.decisionhub.enums.authentication.PlatformRole;
import com.decisionhub.enums.authentication.UserStatus;
import com.decisionhub.exception.BadRequestException;
import com.decisionhub.exception.ResourceNotFoundException;
import com.decisionhub.exception.UnauthorizedActionException;
import com.decisionhub.repository.authentication.UserRepository;
import com.decisionhub.service.interfaces.authentication.BlockedEmailService;
import com.decisionhub.service.interfaces.authentication.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BlockedEmailService blockedEmailService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedActionException("User is not authenticated");
        }
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    @Override
    @Transactional
    public void removeUser(Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != PlatformRole.ADMIN) {
            throw new UnauthorizedActionException("Only administrators can remove users from the platform");
        }

        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (userToRemove.getId().equals(currentUser.getId())) {
            throw new BadRequestException("Administrators cannot remove their own account");
        }

        // 1. Permanently block the target user's email before updating or deleting user records
        blockedEmailService.blockEmail(
                userToRemove.getEmail(),
                "USER_REMOVED_BY_ADMIN",
                currentUser.getEmail()
        );

        // 2. Mark user status as SUSPENDED and set deletedAt
        userToRemove.setStatus(UserStatus.SUSPENDED);
        userToRemove.setDeletedAt(LocalDateTime.now());
        userRepository.save(userToRemove);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );
    }
}
