package com.decisionhub.service.impl.authentication;

import com.decisionhub.dto.response.authentication.BlockedUserResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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
        userToRemove.setBlockedAt(LocalDateTime.now());
        userToRemove.setBlockedBy(currentUser.getEmail());
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

    @Override
    @Transactional(readOnly = true)
    public List<BlockedUserResponse> getBlockedUsers() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != PlatformRole.ADMIN) {
            throw new UnauthorizedActionException("Only administrators can view blocked users");
        }

        List<User> blockedUsers = userRepository.findByStatusIn(List.of(UserStatus.BLOCKED, UserStatus.SUSPENDED));
        return blockedUsers.stream()
                .map(u -> new BlockedUserResponse(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getAvatarUrl(),
                        u.getRole(),
                        u.getStatus(),
                        u.getBlockedAt() != null ? u.getBlockedAt() : (u.getDeletedAt() != null ? u.getDeletedAt() : u.getUpdatedAt()),
                        u.getBlockedBy() != null ? u.getBlockedBy() : "Admin"
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void blockUser(Long userId, String reason) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != PlatformRole.ADMIN) {
            throw new UnauthorizedActionException("Only administrators can block users");
        }

        if (currentUser.getId().equals(userId)) {
            throw new BadRequestException("Administrators cannot block their own account");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (targetUser.getRole() == PlatformRole.ADMIN) {
            throw new BadRequestException("Administrators cannot be blocked");
        }

        if (targetUser.getStatus() == UserStatus.BLOCKED || targetUser.getStatus() == UserStatus.SUSPENDED) {
            throw new BadRequestException("User is already blocked");
        }

        targetUser.setStatus(UserStatus.BLOCKED);
        targetUser.setBlockedAt(LocalDateTime.now());
        targetUser.setBlockedBy(currentUser.getEmail());
        targetUser.setUpdatedAt(LocalDateTime.now());

        userRepository.save(targetUser);




        if (blockedEmailService != null) {
            blockedEmailService.blockEmail(targetUser.getEmail(), reason != null ? reason : "ACCOUNT_BLOCKED", currentUser.getEmail());
        }

        log.info("User ID '{}' ({}) blocked by admin '{}'", targetUser.getId(), targetUser.getEmail(), currentUser.getEmail());
    }

    @Override
    @Transactional
    public void unblockUser(Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != PlatformRole.ADMIN) {
            throw new UnauthorizedActionException("Only administrators can unblock users");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (targetUser.getStatus() != UserStatus.BLOCKED && targetUser.getStatus() != UserStatus.SUSPENDED) {
            throw new BadRequestException("User is not currently blocked");
        }

        targetUser.setStatus(UserStatus.ACTIVE);
        targetUser.setBlockedAt(null);
        targetUser.setBlockedBy(null);
        targetUser.setDeletedAt(null);
        targetUser.setUpdatedAt(LocalDateTime.now());

        userRepository.save(targetUser);





        // Also ensure any email block is removed so they can authenticate again
        blockedEmailService.unblockEmail(targetUser.getEmail());

        log.info("User ID '{}' ({}) unblocked by admin '{}'", targetUser.getId(), targetUser.getEmail(), currentUser.getEmail());
    }
}
