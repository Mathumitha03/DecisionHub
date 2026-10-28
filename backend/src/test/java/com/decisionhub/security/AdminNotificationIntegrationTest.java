package com.decisionhub.security;

import com.decisionhub.config.JwtService;
import com.decisionhub.dto.request.discussion.ReportCommentRequest;
import com.decisionhub.dto.request.notification.BroadcastNotificationRequest;
import com.decisionhub.entity.authentication.User;
import com.decisionhub.entity.community.Category;
import com.decisionhub.entity.decision.Decision;
import com.decisionhub.entity.discussion.Comment;
import com.decisionhub.enums.authentication.PlatformRole;
import com.decisionhub.enums.authentication.UserStatus;
import com.decisionhub.enums.decision.DecisionStatus;
import com.decisionhub.enums.decision.DecisionVisibility;
import com.decisionhub.repository.authentication.UserRepository;
import com.decisionhub.repository.community.CategoryRepository;
import com.decisionhub.repository.decision.DecisionRepository;
import com.decisionhub.repository.discussion.CommentRepository;
import com.decisionhub.repository.notification.NotificationRepository;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminNotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DecisionRepository decisionRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private com.decisionhub.repository.reports.CommentReportRepository commentReportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    private User admin;
    private User regularUser;
    private User commentAuthor;
    private Category category;
    private Decision decision;
    private Comment comment;

    private String adminToken;
    private String regularUserToken;
    private String commentAuthorToken;

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        commentReportRepository.deleteAll();
        commentRepository.deleteAll();
        decisionRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        commentReportRepository.deleteAll();
        commentRepository.deleteAll();
        decisionRepository.deleteAll();

        // 1. Create Category
        category = categoryRepository.findBySlug("general").orElseGet(() -> {
            Category c = new Category();
            c.setName("General");
            c.setSlug("general");
            c.setIsActive(true);
            return categoryRepository.save(c);
        });

        // 2. Ensure Admin User
        admin = userRepository.findByEmailIgnoreCase("admin@gmail.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("AdminUser");
            u.setEmail("admin@gmail.com");
            u.setPasswordHash(passwordEncoder.encode("admin123"));
            u.setRole(PlatformRole.ADMIN);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });
        admin.setRole(PlatformRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin = userRepository.save(admin);

        // 3. Regular User
        regularUser = userRepository.findByEmailIgnoreCase("reporter@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("ReporterUser");
            u.setEmail("reporter@test.com");
            u.setPasswordHash(passwordEncoder.encode("password123"));
            u.setRole(PlatformRole.USER);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        // 4. Comment Author / Decision Creator
        commentAuthor = userRepository.findByEmailIgnoreCase("author@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("AuthorUser");
            u.setEmail("author@test.com");
            u.setPasswordHash(passwordEncoder.encode("password123"));
            u.setRole(PlatformRole.USER);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        // 5. Create Decision
        decision = new Decision();
        decision.setTitle("Platform Governance Policy");
        decision.setDescription("Discussion on governance");
        decision.setCreator(commentAuthor);
        decision.setVisibility(DecisionVisibility.PUBLIC);
        decision.setStatus(DecisionStatus.ACTIVE);
        decision.setCreatedAt(LocalDateTime.now());
        decision = decisionRepository.save(decision);

        // 6. Create Comment
        comment = new Comment();
        comment.setContent("This comment might violate rules.");
        comment.setUser(commentAuthor);
        comment.setDecision(decision);
        comment.setCreatedAt(LocalDateTime.now());
        comment = commentRepository.save(comment);

        // Generate tokens
        UserDetails adminDetails = userDetailsService.loadUserByUsername(admin.getEmail());
        adminToken = jwtService.generateToken(adminDetails);

        UserDetails userDetails = userDetailsService.loadUserByUsername(regularUser.getEmail());
        regularUserToken = jwtService.generateToken(userDetails);

        UserDetails authorDetails = userDetailsService.loadUserByUsername(commentAuthor.getEmail());
        commentAuthorToken = jwtService.generateToken(authorDetails);
    }

    @Test
    @DisplayName("User reports comment -> Admin receives REPORT notification")
    void testReportComment_AdminReceivesNotification() throws Exception {
        ReportCommentRequest request = new ReportCommentRequest("Inappropriate language and spam");

        mockMvc.perform(post("/api/comments/" + comment.getId() + "/report")
                        .header("Authorization", "Bearer " + regularUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Admin checks notifications
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("REPORT"))
                .andExpect(jsonPath("$.content[0].title").value("New Comment Report"));

        // Admin checks unread count
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("Admin locks decision -> Decision creator receives DECISION_LOCKED notification")
    void testAdminLocksDecision_CreatorReceivesNotification() throws Exception {
        mockMvc.perform(put("/api/moderation/decisions/" + decision.getId() + "/lock")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Creator checks notifications
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + commentAuthorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("DECISION_LOCKED"))
                .andExpect(jsonPath("$.content[0].title").value("Decision Locked"));
    }

    @Test
    @DisplayName("Admin pins decision -> Decision creator receives DECISION_PINNED notification")
    void testAdminPinsDecision_CreatorReceivesNotification() throws Exception {
        mockMvc.perform(put("/api/moderation/decisions/" + decision.getId() + "/pin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Creator checks notifications
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + commentAuthorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("DECISION_PINNED"))
                .andExpect(jsonPath("$.content[0].title").value("Decision Pinned"));
    }

    @Test
    @DisplayName("Admin deletes comment -> Comment author receives COMMENT_REMOVED notification")
    void testAdminDeletesComment_AuthorReceivesNotification() throws Exception {
        mockMvc.perform(delete("/api/moderation/comments/" + comment.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Author checks notifications
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + commentAuthorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("COMMENT_REMOVED"))
                .andExpect(jsonPath("$.content[0].title").value("Comment Removed"));
    }

    @Test
    @DisplayName("Admin broadcasts notification -> All active users receive SYSTEM notification")
    void testAdminBroadcastsNotification_AllUsersReceive() throws Exception {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                "Maintenance Alert",
                "Platform will be updated at midnight",
                "/status"
        );

        mockMvc.perform(post("/api/notifications/broadcast")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Both regularUser and commentAuthor should receive the notification
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + regularUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("SYSTEM"))
                .andExpect(jsonPath("$.content[0].title").value("Maintenance Alert"));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + commentAuthorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("SYSTEM"))
                .andExpect(jsonPath("$.content[0].title").value("Maintenance Alert"));
    }

    @Test
    @DisplayName("Non-admin broadcast attempt -> 403 Forbidden")
    void testNonAdminBroadcast_Forbidden() throws Exception {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                "Fake Announcement",
                "Spam message",
                null
        );

        mockMvc.perform(post("/api/notifications/broadcast")
                        .header("Authorization", "Bearer " + regularUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
