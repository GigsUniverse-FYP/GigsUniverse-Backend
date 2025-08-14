package com.giguniverse.backend.Chat.Websocket;

import java.security.Principal;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.giguniverse.backend.Auth.JWT.JwtUserPrincipal;

public class ChatAuthUtil {

    public static JwtUserPrincipal getCurrentUser(Principal principal) {
        if (principal == null) return null;

        if (principal instanceof JwtUserPrincipal) {
            return (JwtUserPrincipal) principal;
        }

        if (principal instanceof UsernamePasswordAuthenticationToken) {
            Object inner = ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
            if (inner instanceof JwtUserPrincipal) {
                return (JwtUserPrincipal) inner;
            }
        }

        return null;
    }

    public static JwtUserPrincipal getCurrentUser(StompHeaderAccessor accessor) {
        return getCurrentUser(accessor.getUser());
    }

    public static String getUserId(Principal principal) {
        JwtUserPrincipal user = getCurrentUser(principal);
        return user != null ? user.getUserId() : null;
    }
}
