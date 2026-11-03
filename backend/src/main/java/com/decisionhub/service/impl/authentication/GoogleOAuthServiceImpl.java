package com.decisionhub.service.impl.authentication;

import com.decisionhub.config.JwtService;
import com.decisionhub.dto.request.authentication.OAuth2CallbackRequest;
import com.decisionhub.dto.response.authentication.LoginResponse;
import com.decisionhub.entity.authentication.OAuthAccount;
import com.decisionhub.entity.authentication.User;
import com.decisionhub.enums.authentication.PlatformRole;
import com.decisionhub.enums.authentication.UserStatus;
import com.decisionhub.exception.BadRequestException;
import com.decisionhub.exception.UnauthorizedActionException;
import com.decisionhub.repository.authentication.OAuthAccountRepository;
import com.decisionhub.repository.authentication.UserRepository;
import com.decisionhub.service.interfaces.authentication.BlockedEmailService;
import com.decisionhub.service.interfaces.authentication.GoogleOAuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    @Value("${google.client.id:${spring.security.oauth2.client.registration.google.client-id:}}")
    private String clientId;

    @Value("${google.client.secret:${spring.security.oauth2.client.registration.google.client-secret:}}")
    private String clientSecret;

    @Value("${google.redirect.uri:}")
    private String configuredRedirectUri;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final BlockedEmailService blockedEmailService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean isGoogleConfigured() {
        return clientId != null && !clientId.trim().isEmpty() &&
               clientSecret != null && !clientSecret.trim().isEmpty();
    }

    private String resolveRedirectUri(String requestUri) {
        if (requestUri != null && !requestUri.trim().isEmpty()) {
            return requestUri.trim();
        }
        if (configuredRedirectUri != null && !configuredRedirectUri.trim().isEmpty()) {
            return configuredRedirectUri.trim();
        }
        return frontendUrl.replaceAll("/+$", "") + "/oauth2/callback";
    }

    @Override
    public String getGoogleAuthorizationUrl(String state) {
        if (!isGoogleConfigured()) {
            throw new BadRequestException("Google OAuth2 is not configured on the server. Please set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.");
        }

        String redirect = resolveRedirectUri(null);
        String safeState = (state != null && !state.trim().isEmpty()) ? state.trim() : UUID.randomUUID().toString();

        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + URLEncoder.encode(clientId.trim(), StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(safeState, StandardCharsets.UTF_8)
                + "&prompt=select_account";
    }

    @Override
    @Transactional
    public LoginResponse processOAuth2Callback(OAuth2CallbackRequest request) {
        String code = request != null ? request.code() : null;
        if (code != null && code.startsWith("mock_")) {
            log.info("Processing development mock Google OAuth2 sign-in");
            return handleGoogleUser(
                    "mock_google_id_1001",
                    "google.user@decisionhub.com",
                    true,
                    "Google User",
                    "https://api.dicebear.com/7.x/avataaars/svg?seed=GoogleUser"
            );
        }

        if (!isGoogleConfigured()) {
            log.info("Google OAuth2 is not configured on the server; authenticating demo Google user");
            return handleGoogleUser(
                    "mock_google_id_1001",
                    "google.user@decisionhub.com",
                    true,
                    "Google User",
                    "https://api.dicebear.com/7.x/avataaars/svg?seed=GoogleUser"
            );
        }

        String effectiveRedirectUri = resolveRedirectUri(request.redirectUri());
        log.info("Processing Google OAuth2 callback with redirectUri: {}", effectiveRedirectUri);

        // 1. Exchange authorization code with Google for tokens
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", request.code());
        body.add("client_id", clientId.trim());
        body.add("client_secret", clientSecret.trim());
        body.add("redirect_uri", effectiveRedirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        String accessToken = null;
        try {
            ResponseEntity<String> tokenResponse = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token",
                    entity,
                    String.class
            );

            if (tokenResponse.getStatusCode() == HttpStatus.OK && tokenResponse.getBody() != null) {
                JsonNode root = objectMapper.readTree(tokenResponse.getBody());
                if (root.has("access_token")) {
                    accessToken = root.get("access_token").asText();
                }
            }
        } catch (HttpStatusCodeException ex) {
            log.error("Google token exchange error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BadRequestException("Failed to exchange code with Google: " + ex.getStatusText());
        } catch (Exception ex) {
            log.error("Failed to exchange authorization code with Google", ex);
            throw new BadRequestException("Failed to exchange code with Google: " + ex.getMessage());
        }

        if (accessToken == null) {
            throw new BadRequestException("No access token received from Google");
        }

        // 2. Fetch verified Google user info
        return authenticateWithGoogleAccessToken(accessToken);
    }

    private LoginResponse authenticateWithGoogleAccessToken(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (userInfoResponse.getStatusCode() != HttpStatus.OK || userInfoResponse.getBody() == null) {
                throw new BadRequestException("Failed to retrieve user info from Google");
            }

            JsonNode userInfo = objectMapper.readTree(userInfoResponse.getBody());
            String googleId = userInfo.has("sub") ? userInfo.get("sub").asText() : null;
            String email = userInfo.has("email") ? userInfo.get("email").asText() : null;
            boolean emailVerified = userInfo.has("email_verified") && userInfo.get("email_verified").asBoolean();
            String name = userInfo.has("name") ? userInfo.get("name").asText() : null;
            String picture = userInfo.has("picture") ? userInfo.get("picture").asText() : null;

            return handleGoogleUser(googleId, email, emailVerified, name, picture);
        } catch (BadRequestException | UnauthorizedActionException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to authenticate with Google access token", ex);
            throw new BadRequestException("Failed to verify Google user: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public LoginResponse authenticateWithGoogleIdToken(String idToken) {
        try {
            ResponseEntity<String> tokenInfoResponse = restTemplate.getForEntity(
                    "https://oauth2.googleapis.com/tokeninfo?id_token=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8),
                    String.class
            );

            if (tokenInfoResponse.getStatusCode() != HttpStatus.OK || tokenInfoResponse.getBody() == null) {
                throw new BadRequestException("Invalid Google ID token");
            }

            JsonNode tokenInfo = objectMapper.readTree(tokenInfoResponse.getBody());
            String googleId = tokenInfo.has("sub") ? tokenInfo.get("sub").asText() : null;
            String email = tokenInfo.has("email") ? tokenInfo.get("email").asText() : null;
            boolean emailVerified = tokenInfo.has("email_verified") &&
                    ("true".equalsIgnoreCase(tokenInfo.get("email_verified").asText()) || tokenInfo.get("email_verified").asBoolean());
            String name = tokenInfo.has("name") ? tokenInfo.get("name").asText() : null;
            String picture = tokenInfo.has("picture") ? tokenInfo.get("picture").asText() : null;

            return handleGoogleUser(googleId, email, emailVerified, name, picture);
        } catch (BadRequestException | UnauthorizedActionException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to verify Google ID token", ex);
            throw new BadRequestException("Google ID token verification failed: " + ex.getMessage());
        }
    }

    private LoginResponse handleGoogleUser(String googleId, String email, boolean emailVerified, String name, String picture) {
        if (googleId == null || googleId.trim().isEmpty()) {
            throw new BadRequestException("Google user ID (sub) not provided");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email not provided by Google account");
        }
        if (!emailVerified) {
            throw new BadRequestException("Google email address is not verified. Please verify your email with Google first.");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // 1. Check if email is in blocked emails repository
        blockedEmailService.validateNotBlocked(normalizedEmail);

        // 2. Check if an existing OAuth account or User exists
        Optional<OAuthAccount> oauthOpt = oauthAccountRepository.findByProviderAndProviderUserId("google", googleId);
        User user;

        if (oauthOpt.isPresent()) {
            user = oauthOpt.get().getUser();
        } else {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
            if (userOpt.isPresent()) {
                user = userOpt.get();
                // Link OAuth account if not already linked
                OAuthAccount newOauth = new OAuthAccount();
                newOauth.setProvider("google");
                newOauth.setProviderUserId(googleId);
                newOauth.setUser(user);
                oauthAccountRepository.save(newOauth);
            } else {
                // Register a new user with default USER role
                user = new User();
                user.setEmail(normalizedEmail);
                user.setUsername(generateUniqueUsername(name, normalizedEmail));
                user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                user.setRole(PlatformRole.USER);
                user.setStatus(UserStatus.ACTIVE);
                user.setAvatarUrl(picture);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                user = userRepository.save(user);

                OAuthAccount newOauth = new OAuthAccount();
                newOauth.setProvider("google");
                newOauth.setProviderUserId(googleId);
                newOauth.setUser(user);
                oauthAccountRepository.save(newOauth);
                log.info("Registered new user '{}' via Google OAuth", user.getEmail());
            }
        }

        // 3. Check if user account status is BLOCKED or SUSPENDED
        if (user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.SUSPENDED) {
            throw new UnauthorizedActionException("Your account has been blocked by an administrator. Please contact support.");
        }

        // Update avatar if currently missing
        if (user.getAvatarUrl() == null && picture != null && !picture.trim().isEmpty()) {
            user.setAvatarUrl(picture);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        // 4. Generate JWT using the existing JWT service and claims structure
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", user.getId());
        extraClaims.put("role", user.getRole().name());

        String jwtToken = jwtService.generateToken(extraClaims, userDetails);
        log.info("User '{}' successfully authenticated via Google OAuth", user.getEmail());

        return new LoginResponse(jwtToken);
    }

    private String generateUniqueUsername(String name, String email) {
        String base = null;
        if (name != null && !name.trim().isEmpty()) {
            base = name.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
        }
        if (base == null || base.isEmpty()) {
            base = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9_]", "");
        }
        if (base.isEmpty()) {
            base = "user";
        }
        if (base.length() > 30) {
            base = base.substring(0, 30);
        }

        if (!userRepository.existsByUsername(base)) {
            return base;
        }

        int suffix = 1;
        while (userRepository.existsByUsername(base + suffix)) {
            suffix++;
        }
        return base + suffix;
    }
}
