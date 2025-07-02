package com.giguniverse.backend.Auth.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2EmployerFailureHandler implements AuthenticationFailureHandler {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        HttpSession session = request.getSession(false);

        String pendingId = session != null ? (String) session.getAttribute("pendingEmployerId") : null;
        String userEmail = session != null ? (String) session.getAttribute("userEmail") : null;
        String error = session != null ? (String) session.getAttribute("oauth2Error") : null;

        // Fallback error
        if (error == null || error.isBlank()) {
            error = "UNKNOWN_ERROR";
        }

        // Clean session
        if (session != null) {
            session.invalidate();
        }

        // Redirect to frontend
        response.sendRedirect(frontendUrl + "/register/employer/google-fail"
            + "?reason=" + error
            + (pendingId != null ? "&id=" + pendingId : "")
            + (userEmail != null ? "&email=" + userEmail : ""));
    }
}
