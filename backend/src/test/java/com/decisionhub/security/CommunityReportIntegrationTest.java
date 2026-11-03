package com.decisionhub.security;

import com.decisionhub.config.JwtService;
import com.decisionhub.dto.request.community.ReportCommunityRequest;
import com.decisionhub.entity.authentication.User;
import com.decisionhub.entity.community.Category;
import com.decisionhub.entity.community.Community;
import com.decisionhub.entity.community.CommunityMember;
import com.decisionhub.entity.notification.Notification;
import com.decisionhub.enums.authentication.PlatformRole;
import com.decisionhub.enums.authentication.UserStatus;
import com.decisionhub.enums.community.CommunityMemberRole;
import com.decisionhub.enums.community.CommunityVisibility;
import com.decisionhub.enums.community.MembershipStatus;
import com.decisionhub.enums.notification.NotificationType;
import com.decisionhub.repository.authentication.UserRepository;
import com.decisionhub.repository.community.CategoryRepository;
import com.decisionhub.repository.community.CommunityMemberRepository;
import com.decisionhub.repository.community.CommunityRepository;
import com.decisionhub.repository.notification.NotificationRepository;
import com.decisionhub.repository.reports.CommunityReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMemberRepository communityMemberRepository;

    @Autowired
    private CommunityReportRepository communityReportRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    private User admin;
    private User memberUser;
    private User nonMemberUser;
    private Community community;
    private Category category;

    private String adminToken;
    private String memberToken;
    private String nonMemberToken;

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        communityReportRepository.deleteAll();
        communityMemberRepository.deleteAll();
        communityRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        communityReportRepository.deleteAll();
        communityMemberRepository.deleteAll();
        communityRepository.deleteAll();

        // 1. Category
        category = categoryRepository.findBySlug("general-cat").orElseGet(() -> {
            Category c = new Category();
            c.setName("General Cat");
            c.setSlug("general-cat");
            c.setIsActive(true);
            return categoryRepository.save(c);
        });

        // 2. Admin User
        admin = userRepository.findByEmailIgnoreCase("admin-test@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("AdminOfficer");
            u.setEmail("admin-test@test.com");
            u.setPasswordHash(passwordEncoder.encode("admin123"));
            u.setRole(PlatformRole.ADMIN);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });
        admin.setRole(PlatformRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin = userRepository.save(admin);

        // 3. Community Member User A
        memberUser = userRepository.findByEmailIgnoreCase("user-a@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("UserA");
            u.setEmail("user-a@test.com");
            u.setPasswordHash(passwordEncoder.encode("password123"));
            u.setRole(PlatformRole.USER);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        // 4. Non-member User B
        nonMemberUser = userRepository.findByEmailIgnoreCase("user-b@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("UserB");
            u.setEmail("user-b@test.com");
            u.setPasswordHash(passwordEncoder.encode("password123"));
            u.setRole(PlatformRole.USER);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        // 5. Community 1
        community = new Community();
        community.setName("Community-1");
        community.setSlug("community-1");
        community.setDescription("First test community");
        community.setCategory(category);
        community.setOwner(admin);
        community.setVisibility(CommunityVisibility.PUBLIC);
        community.setMemberCount(1);
        community = communityRepository.save(community);

        // 6. Member record for User A (Approved)
        CommunityMember member = new CommunityMember();
        member.setCommunity(community);
        member.setUser(memberUser);
        member.setRole(CommunityMemberRole.MEMBER);
        member.setStatus(MembershipStatus.APPROVED);
        member.setJoinedAt(LocalDateTime.now());
        communityMemberRepository.save(member);

        // JWT tokens
        UserDetails adminDetails = userDetailsService.loadUserByUsername(admin.getEmail());
        adminToken = jwtService.generateToken(adminDetails);

        UserDetails memberDetails = userDetailsService.loadUserByUsername(memberUser.getEmail());
        memberToken = jwtService.generateToken(memberDetails);

        UserDetails nonMemberDetails = userDetailsService.loadUserByUsername(nonMemberUser.getEmail());
        nonMemberToken = jwtService.generateToken(nonMemberDetails);
    }

    @Test
    @DisplayName("User A is member of Community-1 -> can report community -> Admin receives notification")
    void testMemberCanReportCommunity_AdminReceivesNotification() throws Exception {
        ReportCommunityRequest request = new ReportCommunityRequest(
                community.getId(),
                "Inappropriate behavior by moderator"
        );

        mockMvc.perform(post("/api/communities/reportCommunity")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.communityId").value(community.getId()))
                .andExpect(jsonPath("$.communityName").value("Community-1"))
                .andExpect(jsonPath("$.reporterUsername").value("UserA"))
                .andExpect(jsonPath("$.reason").value("Inappropriate behavior by moderator"));

        // Verify report persisted in repository
        assertEquals(1, communityReportRepository.count());

        // Verify admin notification
        List<Notification> adminNotifs = notificationRepository.findAll().stream()
                .filter(n -> n.getRecipient().getId().equals(admin.getId()))
                .toList();

        assertFalse(adminNotifs.isEmpty(), "Admin should have received at least one notification");
        Notification reportNotif = adminNotifs.get(0);
        assertEquals(NotificationType.REPORT, reportNotif.getType());
        assertTrue(reportNotif.getMessage().contains("Community-1 is reported by the user 'UserA' for the reason: Inappropriate behavior by moderator"));

        // Admin checks inbox via API
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("REPORT"))
                .andExpect(jsonPath("$.content[0].message", containsString("Community-1 is reported by the user 'UserA' for the reason")));
    }

    @Test
    @DisplayName("User B is NOT a member of Community-1 -> cannot report community -> 403 Forbidden")
    void testNonMemberCannotReportCommunity_Forbidden() throws Exception {
        ReportCommunityRequest request = new ReportCommunityRequest(
                community.getId(),
                "Spam actions"
        );

        mockMvc.perform(post("/api/communities/reportCommunity")
                        .header("Authorization", "Bearer " + nonMemberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Ensure no report created
        assertEquals(0, communityReportRepository.count());
    }

    @Test
    @DisplayName("Report community via /{id}/reportCommunity path variable endpoint")
    void testReportCommunity_PathVariableEndpoint() throws Exception {
        ReportCommunityRequest request = new ReportCommunityRequest(
                null,
                "Inappropriate discussion"
        );

        mockMvc.perform(post("/api/communities/" + community.getId() + "/reportCommunity")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.communityId").value(community.getId()))
                .andExpect(jsonPath("$.communityName").value("Community-1"));

        assertEquals(1, communityReportRepository.count());
    }

    @Test
    @DisplayName("Admin notification endpoint sendAdminNotification works")
    void testSendAdminNotificationEndpoint() throws Exception {
        ReportCommunityRequest request = new ReportCommunityRequest(
                community.getId(),
                "Direct admin notification test"
        );

        mockMvc.perform(post("/api/communities/sendAdminNotification")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification sent to admin"));

        List<Notification> adminNotifs = notificationRepository.findAll().stream()
                .filter(n -> n.getRecipient().getId().equals(admin.getId()))
                .toList();

        assertFalse(adminNotifs.isEmpty());
        assertTrue(adminNotifs.get(0).getMessage().contains("Community-1 is reported by the user 'UserA' for the reason: Direct admin notification test"));
    }

    @Test
    @DisplayName("Get community reports endpoint retrieves reports for community")
    void testGetCommunityReportsEndpoint() throws Exception {
        ReportCommunityRequest request = new ReportCommunityRequest(
                community.getId(),
                "First report"
        );

        mockMvc.perform(post("/api/communities/reportCommunity")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/communities/" + community.getId() + "/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].communityName").value("Community-1"))
                .andExpect(jsonPath("$[0].reason").value("First report"));
    }
}

