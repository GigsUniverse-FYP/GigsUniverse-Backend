package com.giguniverse.backend.Auth.Session;
import org.springframework.security.core.context.SecurityContextHolder;

import com.giguniverse.backend.Auth.JWT.JwtUserPrincipal;


public class AuthUtil {
    public static JwtUserPrincipal getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println("DEBUG principal = " + principal.getClass() + " :: " + principal);
        if (principal instanceof JwtUserPrincipal) {
            return (JwtUserPrincipal) principal;
        }
        return null;
    }

    public static String getUserId() {
        JwtUserPrincipal user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    public static String getUserEmail() {
        JwtUserPrincipal user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public static String getUserRole() {
        JwtUserPrincipal user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }

}
