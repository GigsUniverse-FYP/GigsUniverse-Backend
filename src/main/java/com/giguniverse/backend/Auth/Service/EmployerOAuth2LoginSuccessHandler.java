package com.giguniverse.backend.Auth.Service;

import org.springframework.http.HttpHeaders;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.giguniverse.backend.Auth.JWT.JwtConfig;
import com.giguniverse.backend.Auth.JWT.JwtUtil;
import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;

import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class EmployerOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmployerRepository employerRepo;

    @Autowired
    private JwtConfig jwtConfig;

    @Value("${frontend.url}")
    private String frontendURL;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();

        Optional<Employer> employerOpt = employerRepo.findByEmail(email);

        // Check if user exists
        if (employerOpt.isEmpty()) {
            response.sendRedirect(frontendURL + "/login/employer/oauth-fail?reason=user_not_registered");
            return;
        }

        Employer employer = employerOpt.get();

        // Check if the account is registered with Google
        if (!"google".equalsIgnoreCase(employer.getRegistrationProvider())) {
            response.sendRedirect(frontendURL + "/login/employer/oauth-fail?reason=not_registered_with_google");
            return;
        }

        // Generate JWT
        String jwt = jwtUtil.generateJwtToken(
            employer.getEmployerUserId(),
            employer.getEmail(),
            employer.getRole()
        );

        // Set cookie
        ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
            .httpOnly(true)
            .secure(true) // Set to true in production (HTTPS)
            .path("/")
            .maxAge(jwtConfig.getExpiration())
            .sameSite("None")  
            .domain(".gigsuniverse.studio")    
            .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // Disposing client side JSESSION (Performance Improvement)
        ResponseCookie jsessionClear = ResponseCookie.from("JSESSIONID", "")
            .path("/")                      
            .maxAge(0)                      
            .httpOnly(true)                
            .sameSite("Lax")               
            .secure(false)                   
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jsessionClear.toString());

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        employer.setLastLoginDate(LocalDateTime.now());
        employerRepo.save(employer);

        response.sendRedirect(frontendURL + "/dashboard/employer");
    }
}
