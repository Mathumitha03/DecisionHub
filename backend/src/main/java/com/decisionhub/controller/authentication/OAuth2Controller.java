package com.decisionhub.controller.authentication;

import com.decisionhub.dto.request.authentication.GoogleAuthRequest;
import com.decisionhub.dto.request.authentication.OAuth2CallbackRequest;
import com.decisionhub.dto.response.authentication.LoginResponse;
import com.decisionhub.service.interfaces.authentication.GoogleOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OAuth2 Authentication", description = "Endpoints for OAuth2 and Google Sign-In")
public class OAuth2Controller {

    private final GoogleOAuthService googleOAuthService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/config")
    @Operation(summary = "Get Google OAuth configuration status")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "configured", googleOAuthService.isGoogleConfigured()
        ));
    }

    @GetMapping("/google")
    @Operation(summary = "Initiate Google OAuth2 authentication flow")
    public void initiateGoogleOAuth(
            @RequestParam(required = false) String state,
            HttpServletResponse response
    ) throws IOException {
        if (!googleOAuthService.isGoogleConfigured()) {
            log.info("Google OAuth is not configured with external credentials; redirecting to demo callback");
            String safeState = (state != null && !state.trim().isEmpty()) ? state.trim() : "dh_oauth_state";
            String devCallbackUrl = frontendUrl.replaceAll("/+$", "") + "/oauth2/callback?code=mock_google_dev_code&state=" + java.net.URLEncoder.encode(safeState, java.nio.charset.StandardCharsets.UTF_8);
            response.sendRedirect(devCallbackUrl);
            return;
        }

        String authUrl = googleOAuthService.getGoogleAuthorizationUrl(state);
        response.sendRedirect(authUrl);
    }

    @PostMapping("/callback")
    @Operation(summary = "Exchange Google authorization code for JWT token")
    public ResponseEntity<LoginResponse> handleCallback(
            @Valid @RequestBody OAuth2CallbackRequest request
    ) {
        log.info("REST request to handle Google OAuth2 callback");
        LoginResponse response = googleOAuthService.processOAuth2Callback(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    @Operation(summary = "Authenticate with Google ID token")
    public ResponseEntity<LoginResponse> authenticateWithGoogle(
            @Valid @RequestBody GoogleAuthRequest request
    ) {
        log.info("REST request to authenticate with Google ID token");
        LoginResponse response = googleOAuthService.authenticateWithGoogleIdToken(request.idToken());
        return ResponseEntity.ok(response);
    }
}
