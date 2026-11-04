package com.decisionhub.service.interfaces.authentication;

import com.decisionhub.dto.response.authentication.BlockedUserResponse;
import com.decisionhub.dto.response.authentication.UserResponse;

import java.util.List;

public interface UserService {

    void removeUser(Long userId);

    UserResponse getUserById(Long userId);

    List<BlockedUserResponse> getBlockedUsers();

    void blockUser(Long userId, String reason);

    void unblockUser(Long userId);
}
