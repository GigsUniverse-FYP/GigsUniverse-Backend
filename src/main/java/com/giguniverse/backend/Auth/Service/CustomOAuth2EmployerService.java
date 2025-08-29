package com.giguniverse.backend.Auth.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class CustomOAuth2EmployerService extends OidcUserService {

    @Autowired
    private EmployerRepository employerRepo;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("✅ STARTING LOAD USER FUNCTION");

        HttpServletRequest currentRequest =
            ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        HttpSession session = currentRequest.getSession(false);

        if (session == null || session.getAttribute("pendingEmployerId") == null) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("session_expired_or_invalid", "Session is missing or expired", null));
        }

        String pendingId = (String) session.getAttribute("pendingEmployerId");
        OidcUser oidcUser = super.loadUser(userRequest);
        String email = oidcUser.getEmail();

        try {
            if (email == null) {
                throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_required", "Email not found", null));
            }

            if (employerRepo.existsByEmployerUserId(pendingId)) {
                session.setAttribute("userEmail", email);
                throw new OAuth2AuthenticationException(
                    new OAuth2Error("id_exists", "Employer ID is already taken", null));
            }

            if (employerRepo.findByEmail(email).isPresent()) {
                session.setAttribute("userEmail", email);
                throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_exists", "Email already exists", null));
            }

        } catch (OAuth2AuthenticationException ex) {
            session.setAttribute("oauth2Error", ex.getError().getErrorCode());
            throw ex;
        }

        // Save to DB
        Employer newUser = new Employer();
        newUser.setEmployerUserId(pendingId);
        newUser.setEmail(email);
        newUser.setRole("employer");
        newUser.setRegistrationProvider("google");
        newUser.setEmailConfirmed(true);
        newUser.setRegistrationDate(LocalDateTime.now());
        employerRepo.save(newUser);

        Map<String, Object> newAttributes = new HashMap<>(oidcUser.getClaims());
        newAttributes.put("userEmail", email);
        newAttributes.put("pendingEmployerId", pendingId);

        return new DefaultOidcUser(
            oidcUser.getAuthorities(),
            oidcUser.getIdToken(),
            oidcUser.getUserInfo(),
            "email"
        );
    }
}
