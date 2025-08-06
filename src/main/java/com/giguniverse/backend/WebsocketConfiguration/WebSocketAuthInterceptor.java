package com.giguniverse.backend.WebsocketConfiguration;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import com.giguniverse.backend.Auth.JWT.JwtUtil;

import java.security.Principal;
import java.util.List;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearerToken = authHeaders.get(0).replace("Bearer ", "").trim();
            System.out.println("🔐 Received WebSocket Authorization token: " + bearerToken);
            try {
                String userEmail = jwtUtil.getEmailFromToken(bearerToken); 
                System.out.println("✅ Parsed email from token: " + userEmail);

                accessor.setUser(new Principal() {
                    @Override
                    public String getName() {
                        return userEmail;
                    }
                });
                System.out.println("✅ WebSocket connected with principal: " + accessor.getUser().getName());

            } catch (Exception e) {
                System.out.println("JWT parse failed: " + e.getMessage());
            }
        }

        return message;
    }
}
