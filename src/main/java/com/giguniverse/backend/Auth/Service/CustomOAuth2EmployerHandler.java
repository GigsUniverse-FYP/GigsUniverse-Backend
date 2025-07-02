package com.giguniverse.backend.Auth.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2EmployerHandler implements AuthenticationSuccessHandler {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        HttpSession session = request.getSession(false);
        String userId = session != null ? (String) session.getAttribute("pendingEmployerId") : null;
        String error = session != null ? (String) session.getAttribute("oauth2Error") : null;

        String userEmail = "";
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            userEmail = oauth2User.getAttribute("userEmail");
            if (userEmail == null) {
                userEmail = oauth2User.getAttribute("email");
            }
        }

        if (session != null) {
            session.removeAttribute("pendingEmployerId");
            session.removeAttribute("oauth2Error");
            session.removeAttribute(userEmail);
            session.invalidate();
        }

        if (error != null) {
            response.sendRedirect(frontendUrl + "/register/employer/google-fail" +
                "?reason=" + error +
                (userId != null ? "&id=" + userId : "") +
                "&email=" + userEmail);
        } else {
            response.sendRedirect(frontendUrl + "/register/employer/google-success" +
                "?id=" + userId +
                "&email=" + userEmail);
        }
    }
}
