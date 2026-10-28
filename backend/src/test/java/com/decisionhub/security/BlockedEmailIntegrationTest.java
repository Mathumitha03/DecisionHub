package com.decisionhub.security;

import com.decisionhub.config.JwtService;
import com.decisionhub.dto.request.authentication.LoginRequest;
import com.decisionhub.dto.request.authentication.RegisterRequest;
import com.decisionhub.dto.request.community.CreateCommunityRequest;
import com.decisionhub.entity.authentication.BlockedEmail;
import com.decisionhub.entity.authentication.User;
import com.decisionhub.entity.community.Category;
import com.decisionhub.entity.community.Community;
import com.decisionhub.enums.authentication.PlatformRole;
import com.decisionhub.enums.authentication.UserStatus;
import com.decisionhub.enums.community.CommunityVisibility;
import com.decisionhub.repository.authentication.BlockedEmailRepository;
import com.decisionhub.repository.authentication.UserRepository;
import com.decisionhub.repository.community.CategoryRepository;
import com.decisionhub.repository.community.CommunityRepository;
import com.decisionhub.service.interfaces.authentication.BlockedEmailService;
import com.decisionhub.service.interfaces.authentication.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlockedEmailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BlockedEmailRepository blockedEmailRepository;

    @Autowired
    private BlockedEmailService blockedEmailService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    private User adminUser;
    private User regularUser;
    private User communityOwner;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Clear previous test data
        communityRepository.findAll().forEach(c -> {
            c.setDeletedAt(null);
            communityRepository.delete(c);
        });
        blockedEmailRepository.deleteAll();

        // Ensure category exists
        testCategory = categoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            Category cat = new Category();
            cat.setName("Tech");
            cat.setSlug("tech");
            cat.setIsActive(true);
            return categoryRepository.save(cat);
        });

        // Seed Admin User
        adminUser = userRepository.findByEmailIgnoreCase("admin_test@decisionhub.com").orElseGet(() -> {
            User admin = new User();
            admin.setUsername("admin_test");
            admin.setEmail("admin_test@decisionhub.com");
            admin.setPasswordHash(passwordEncoder.encode("Password123!"));
            admin.setRole(PlatformRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            return userRepository.save(admin);
        });

        // Seed Regular User
        regularUser = userRepository.findByEmailIgnoreCase("regular_user@decisionhub.com").orElseGet(() -> {
            User user = new User();
            user.setUsername("regular_user");
            user.setEmail("regular_user@decisionhub.com");
            user.setPasswordHash(passwordEncoder.encode("Password123!"));
            user.setRole(PlatformRole.USER);
            user.setStatus(UserStatus.ACTIVE);
            return userRepository.save(user);
        });

        // Seed Community Owner User
        communityOwner = userRepository.findByEmailIgnoreCase("owner_user@decisionhub.com").orElseGet(() -> {
            User owner = new User();
            owner.setUsername("owner_user");
            owner.setEmail("owner_user@decisionhub.com");
            owner.setPasswordHash(passwordEncoder.encode("Password123!"));
            owner.setRole(PlatformRole.MODERATOR);
            owner.setStatus(UserStatus.ACTIVE);
            return userRepository.save(owner);
        });

        // Make sure none of these test users are marked deleted
        adminUser.setDeletedAt(null);
        adminUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(adminUser);

        regularUser.setDeletedAt(null);
        regularUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(regularUser);

        communityOwner.setDeletedAt(null);
        communityOwner.setStatus(UserStatus.ACTIVE);
        userRepository.save(communityOwner);
    }

    private String generateToken(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("role", user.getRole().name());
        return jwtService.generateToken(claims, userDetails);
    }

    @Test
    @DisplayName("1. Admin deletes a community -> its owner's email is permanently blocked")
    void testAdminDeletesCommunity_BlocksOwnerEmail() throws Exception {
        // Create community owned by communityOwner
        Community community = new Community();
        community.setName("Owner Community 1");
        community.setSlug("owner-comm-1");
        community.setDescription("Test community for deletion");
        community.setCategory(testCategory);
        community.setOwner(communityOwner);
        community.setVisibility(CommunityVisibility.PUBLIC);
        community.setMemberCount(1);
        community = communityRepository.save(community);

        String adminToken = generateToken(adminUser);

        // Admin deletes the community
        mockMvc.perform(delete("/api/communities/" + community.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify community is soft-deleted
        Community updatedComm = communityRepository.findById(community.getId()).orElseThrow();
        assertNotNull(updatedComm.getDeletedAt());

        // Verify owner's email is blocked
        assertTrue(blockedEmailRepository.existsByEmailIgnoreCase("owner_user@decisionhub.com"));
        assertTrue(blockedEmailService.isBlocked("owner_user@decisionhub.com"));

        // Verify admin's email is NOT blocked
        assertFalse(blockedEmailService.isBlocked("admin_test@decisionhub.com"));
    }

    @Test
    @DisplayName("2. The blocked owner cannot create another community")
    void testBlockedOwnerCannotCreateCommunity() throws Exception {
        // Generate token before blocking
        String ownerToken = generateToken(communityOwner);

        // Block the owner's email
        blockedEmailService.blockEmail("owner_user@decisionhub.com", "COMMUNITY_DELETED", "admin");

        CreateCommunityRequest request = new CreateCommunityRequest(
                "New Blocked Community",
                "new-blocked-community",
                "Description",
                testCategory.getId(),
                CommunityVisibility.PUBLIC
        );

        mockMvc.perform(post("/api/communities")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. The blocked owner cannot register again using the same email")
    void testBlockedOwnerCannotRegisterAgain() throws Exception {
        // Block email
        blockedEmailService.blockEmail("owner_user@decisionhub.com", "COMMUNITY_DELETED", "admin");

        RegisterRequest request = new RegisterRequest(
                "new_owner_alias",
                "owner_user@decisionhub.com",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This email address has been permanently blocked from the platform"));
    }

    @Test
    @DisplayName("4. The blocked owner cannot log in")
    void testBlockedOwnerCannotLogin() throws Exception {
        // Block email
        blockedEmailService.blockEmail("owner_user@decisionhub.com", "COMMUNITY_DELETED", "admin");

        LoginRequest request = new LoginRequest("owner_user@decisionhub.com", "Password123!", "USER");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This email address has been permanently blocked from the platform"));
    }

    @Test
    @DisplayName("5. Admin removes a user -> that user's email is permanently blocked")
    void testAdminRemovesUser_BlocksUserEmail() throws Exception {
        String adminToken = generateToken(adminUser);

        // Admin removes regularUser
        mockMvc.perform(delete("/api/users/" + regularUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify user email is blocked
        assertTrue(blockedEmailRepository.existsByEmailIgnoreCase("regular_user@decisionhub.com"));
        assertTrue(blockedEmailService.isBlocked("regular_user@decisionhub.com"));

        // Verify user account status is SUSPENDED
        User updatedUser = userRepository.findById(regularUser.getId()).orElseThrow();
        assertEquals(UserStatus.SUSPENDED, updatedUser.getStatus());
        assertNotNull(updatedUser.getDeletedAt());
    }

    @Test
    @DisplayName("6. A blocked user cannot register or log in again")
    void testBlockedUserCannotRegisterOrLoginAgain() throws Exception {
        String adminToken = generateToken(adminUser);

        // Remove user
        mockMvc.perform(delete("/api/users/" + regularUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Attempt login -> forbidden
        LoginRequest loginRequest = new LoginRequest("regular_user@decisionhub.com", "Password123!", "USER");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden());

        // Attempt registration -> forbidden
        RegisterRequest regRequest = new RegisterRequest("another_username", "regular_user@decisionhub.com", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Email case variations cannot bypass the restriction")
    void testEmailCaseVariationsCannotBypassRestriction() throws Exception {
        // Block lower case
        blockedEmailService.blockEmail("case_test@decisionhub.com", "TEST_BLOCK", "admin");

        // Try mixed case login
        LoginRequest loginRequest = new LoginRequest("Case_Test@DecisionHub.com", "Password123!", "USER");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This email address has been permanently blocked from the platform"));

        // Try upper case registration
        RegisterRequest regRequest = new RegisterRequest("test_user_case", "CASE_TEST@DECISIONHUB.COM", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This email address has been permanently blocked from the platform"));

        // Verify case-insensitive check in service
        assertTrue(blockedEmailService.isBlocked("CASE_TEST@decisionhub.com"));
        assertTrue(blockedEmailService.isBlocked("case_test@DECISIONHUB.COM"));
    }

    @Test
    @DisplayName("8. Existing blocked records survive user/community deletion")
    void testBlockedRecordsSurviveRecordDeletion() {
        // Block email
        BlockedEmail blocked = blockedEmailService.blockEmail("survivor@decisionhub.com", "TEST", "admin");
        assertNotNull(blocked);
        Long blockedId = blocked.getId();

        // Create and delete user with this email
        User tempUser = new User();
        tempUser.setUsername("survivor_user");
        tempUser.setEmail("survivor@decisionhub.com");
        tempUser.setPasswordHash(passwordEncoder.encode("Pass123!"));
        tempUser.setRole(PlatformRole.USER);
        tempUser.setStatus(UserStatus.ACTIVE);
        tempUser = userRepository.save(tempUser);

        // Hard-delete the user
        userRepository.delete(tempUser);

        // Verify blocked email record is still present and valid
        assertTrue(blockedEmailRepository.findById(blockedId).isPresent());
        assertTrue(blockedEmailService.isBlocked("survivor@decisionhub.com"));
    }

    @Test
    @DisplayName("9. Unauthorized users cannot trigger admin actions")
    void testUnauthorizedUsersCannotTriggerAdminActions() throws Exception {
        String userToken = generateToken(regularUser);

        // Regular user attempts to remove another user -> 403 Forbidden
        mockMvc.perform(delete("/api/users/" + communityOwner.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Verify communityOwner's email was NOT blocked
        assertFalse(blockedEmailService.isBlocked("owner_user@decisionhub.com"));

        // Create a community owned by communityOwner
        Community community = new Community();
        community.setName("Protected Community");
        community.setSlug("protected-comm");
        community.setDescription("Should not be deleted by unauthorized user");
        community.setCategory(testCategory);
        community.setOwner(communityOwner);
        community.setVisibility(CommunityVisibility.PUBLIC);
        community.setMemberCount(1);
        community = communityRepository.save(community);

        // Regular user attempts to delete someone else's community -> 403 Forbidden
        mockMvc.perform(delete("/api/communities/" + community.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Verify community was not deleted and owner was not blocked
        Community checkComm = communityRepository.findById(community.getId()).orElseThrow();
        assertNull(checkComm.getDeletedAt());
        assertFalse(blockedEmailService.isBlocked("owner_user@decisionhub.com"));
    }

    @Test
    @DisplayName("10. Active JWT session is invalidated when email is blocked")
    void testActiveJwtSession_InvalidatedWhenEmailBlocked() throws Exception {
        // Generate valid token while user is active
        String userToken = generateToken(regularUser);

        // Request before block succeeds
        mockMvc.perform(get("/api/communities")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Now block the user's email
        blockedEmailService.blockEmail(regularUser.getEmail(), "ADMIN_BLOCKED", "admin");

        // Next request with same JWT token is rejected with 403 Forbidden
        mockMvc.perform(get("/api/communities")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
