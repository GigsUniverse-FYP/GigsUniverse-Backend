package com.giguniverse.backend.WebsocketConfiguration;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import com.giguniverse.backend.Auth.JWT.JwtUtil;

import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest req = servletRequest.getServletRequest();

            String token = null;
            if (req.getCookies() != null) {
                for (Cookie cookie : req.getCookies()) {
                    if ("jwt".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }

            if (token != null && jwtUtil.validateToken(token)) {
                String userId = jwtUtil.getUserIdFromToken(token);  
                String role = jwtUtil.getRoleFromToken(token);      

                attributes.put("userId", userId);
                attributes.put("role", role);

                logger.info("WebSocket handshake success for userId: {} role: {}", userId, role);

                return true;
            } else {
                logger.warn("Invalid or missing JWT during WebSocket handshake");
            }
        }

        return false; 
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            logger.error("WebSocket handshake failed with exception: {}", exception.getMessage());
        } else {
            logger.debug("WebSocket handshake completed");
        }

    }
}
