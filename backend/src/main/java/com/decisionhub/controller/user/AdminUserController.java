package com.decisionhub.controller.user;

import com.decisionhub.dto.request.authentication.BlockUserRequest;
import com.decisionhub.dto.response.authentication.BlockedUserResponse;
import com.decisionhub.service.interfaces.authentication.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Users", description = "Endpoints for administrator user management")
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/blocked")
    @Operation(summary = "Get all blocked users", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<BlockedUserResponse>> getBlockedUsers() {
        return ResponseEntity.ok(userService.getBlockedUsers());
    }

    @PutMapping("/{userId}/block")
    @Operation(summary = "Block a user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> blockUser(
            @PathVariable Long userId,
            @RequestBody(required = false) BlockUserRequest request) {
        String reason = request != null ? request.reason() : "Blocked by admin";
        userService.blockUser(userId, reason);
        return ResponseEntity.ok(Map.of("message", "User has been blocked successfully"));
    }

    @PutMapping("/{userId}/unblock")
    @Operation(summary = "Unblock a user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> unblockUser(@PathVariable Long userId) {
        userService.unblockUser(userId);
        return ResponseEntity.ok(Map.of("message", "User has been unblocked successfully"));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Remove user permanently", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> removeUser(@PathVariable Long userId) {
        userService.removeUser(userId);
        return ResponseEntity.ok("User removed and email permanently blocked");
    }
}
