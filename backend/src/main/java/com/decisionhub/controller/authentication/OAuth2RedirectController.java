package com.decisionhub.controller.authentication;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OAuth2RedirectController {

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/login/oauth2/code/google")
    public void handleGoogleRedirect(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription,
            HttpServletResponse response
    ) throws IOException {
        log.info("Received redirect from Google OAuth at /login/oauth2/code/google");
        StringBuilder target = new StringBuilder(frontendUrl.replaceAll("/+$", "")).append("/oauth2/callback?");
        if (code != null) {
            target.append("code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8)).append("&");
        }
        if (state != null) {
            target.append("state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8)).append("&");
        }
        if (error != null) {
            target.append("error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8)).append("&");
        }
        if (errorDescription != null) {
            target.append("error_description=").append(URLEncoder.encode(errorDescription, StandardCharsets.UTF_8));
        }
        response.sendRedirect(target.toString().replaceAll("[?&]$", ""));
    }
}
