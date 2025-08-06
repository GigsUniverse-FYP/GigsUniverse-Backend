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
import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;

import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class FreelancerOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FreelancerRepository freelancerRepo;

    @Autowired
    private JwtConfig jwtConfig;

    @Value("${frontend.url}")
    private String frontendURL;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();

        Optional<Freelancer> freelancerOpt = freelancerRepo.findByEmail(email);

        // Check if user exists
        if (freelancerOpt.isEmpty()) {
            response.sendRedirect(frontendURL + "/login/freelancer/oauth-fail?reason=user_not_registered");
            return;
        }

        Freelancer freelancer = freelancerOpt.get();

        // Check if the account is registered with Google
        if (!"google".equalsIgnoreCase(freelancer.getRegistrationProvider())) {
            response.sendRedirect(frontendURL + "/login/freelancer/oauth-fail?reason=not_registered_with_google");
            return;
        }

        // Generate JWT
        String jwt = jwtUtil.generateJwtToken(
            freelancer.getFreelancerUserId(),
            freelancer.getEmail(),
            freelancer.getRole()
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

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        
        // Disposing server side JSESSION 
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Disposing client side JSESSION (Performance Improvement)
        ResponseCookie jsessionClear = ResponseCookie.from("JSESSIONID", "")
            .path("/")                      
            .maxAge(0)                      
            .httpOnly(true)                
            .sameSite("Lax")               
            .secure(false)          
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jsessionClear.toString());

        freelancer.setLastLoginDate(LocalDateTime.now());
        freelancerRepo.save(freelancer);
        
        // Redirect to dashboard
        response.sendRedirect(frontendURL + "/dashboard/freelancer");
    }
}
