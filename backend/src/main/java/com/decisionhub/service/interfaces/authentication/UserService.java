package com.decisionhub.service.interfaces.authentication;

import com.decisionhub.dto.response.authentication.UserResponse;

public interface UserService {

    void removeUser(Long userId);

    UserResponse getUserById(Long userId);
}
