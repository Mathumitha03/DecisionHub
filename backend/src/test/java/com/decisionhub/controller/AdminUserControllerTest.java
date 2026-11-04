package com.decisionhub.controller;

import com.decisionhub.config.JwtService;
import com.decisionhub.controller.user.AdminUserController;
import com.decisionhub.dto.response.authentication.BlockedUserResponse;
import com.decisionhub.enums.authentication.PlatformRole;
import com.decisionhub.enums.authentication.UserStatus;
import com.decisionhub.service.impl.authentication.CustomUserDetailsService;
import com.decisionhub.service.interfaces.authentication.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AdminUserController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.decisionhub\\.security\\..*"
    )
)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private BlockedUserResponse blockedUser;

    @BeforeEach
    void setUp() {
        blockedUser = new BlockedUserResponse(
                10L,
                "blockeduser",
                "blocked@example.com",
                "https://example.com/pic.jpg",
                PlatformRole.USER,
                UserStatus.BLOCKED,
                LocalDateTime.now(),
                "admin@decisionhub.com"
        );
    }

    @Test
    void getBlockedUsers_returnsList() throws Exception {
        when(userService.getBlockedUsers()).thenReturn(List.of(blockedUser));

        mockMvc.perform(get("/api/admin/users/blocked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].username").value("blockeduser"))
                .andExpect(jsonPath("$[0].status").value("BLOCKED"))
                .andExpect(jsonPath("$[0].blockedBy").value("admin@decisionhub.com"));
    }

    @Test
    void blockUser_success() throws Exception {
        doNothing().when(userService).blockUser(eq(10L), any());

        mockMvc.perform(put("/api/admin/users/10/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Inappropriate activity\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User has been blocked successfully"));

        verify(userService).blockUser(eq(10L), eq("Inappropriate activity"));
    }

    @Test
    void unblockUser_success() throws Exception {
        doNothing().when(userService).unblockUser(10L);

        mockMvc.perform(put("/api/admin/users/10/unblock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User has been unblocked successfully"));

        verify(userService).unblockUser(10L);
    }
}
