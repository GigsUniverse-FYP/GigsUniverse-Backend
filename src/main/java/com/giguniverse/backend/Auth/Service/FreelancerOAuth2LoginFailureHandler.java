package com.giguniverse.backend.Auth.Service;

import java.io.IOException;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FreelancerOAuth2LoginFailureHandler implements AuthenticationFailureHandler{
    @Value("${frontend.url}")
    private String frontendURL;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.sendRedirect(frontendURL + "/login/freelancer/oauth-fail?reason=oauth2_failed");
    }
}
