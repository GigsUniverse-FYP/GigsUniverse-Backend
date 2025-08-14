package com.giguniverse.backend.Chat.Websocket;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.giguniverse.backend.Auth.JWT.JwtUtil;


public class ChatJwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    public ChatJwtHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String token = null;

        // Extract token from query string or headers
        if (request.getURI().getQuery() != null && request.getURI().getQuery().contains("token=")) {
            token = request.getURI().getQuery().replace("token=", "").trim();
        } else if (request.getHeaders().containsKey("Authorization")) {
            token = request.getHeaders().getFirst("Authorization").replace("Bearer ", "");
        }

        if (token != null && jwtUtil.validateToken(token)) {
            String userId = jwtUtil.getUserIdFromToken(token);
            String email = jwtUtil.getEmailFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            attributes.put("USER_ID", userId);
            attributes.put("USER_EMAIL", email);
            attributes.put("USER_ROLE", role);
        }

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // nothing
    }
}
