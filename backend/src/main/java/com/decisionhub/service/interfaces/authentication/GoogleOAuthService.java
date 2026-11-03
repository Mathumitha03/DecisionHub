package com.decisionhub.service.interfaces.authentication;

import com.decisionhub.dto.request.authentication.OAuth2CallbackRequest;
import com.decisionhub.dto.response.authentication.LoginResponse;

public interface GoogleOAuthService {

    String getGoogleAuthorizationUrl(String state);

    LoginResponse processOAuth2Callback(OAuth2CallbackRequest request);

    LoginResponse authenticateWithGoogleIdToken(String idToken);

    boolean isGoogleConfigured();
}
