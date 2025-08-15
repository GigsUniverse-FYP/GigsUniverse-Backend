package com.giguniverse.backend.Chat.Websocket;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.giguniverse.backend.Auth.JWT.JwtUtil;

import jakarta.servlet.http.Cookie;

@Configuration
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    @Value("${frontend.url}")
    private String frontendURL;

    public ChatWebSocketConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(frontendURL)
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request,
                                                   ServerHttpResponse response,
                                                   WebSocketHandler wsHandler,
                                                   Map<String, Object> attributes) {
                        if (request instanceof ServletServerHttpRequest servletRequest) {
                            Cookie[] cookies = servletRequest.getServletRequest().getCookies();
                            if (cookies != null) {
                                for (Cookie cookie : cookies) {
                                    if ("jwt".equals(cookie.getName())) {
                                        String token = cookie.getValue();
                                        if (jwtUtil.validateToken(token)) {
                                            String userId = jwtUtil.getUserIdFromToken(token);
                                            attributes.put("USER_ID", userId);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request,
                                               ServerHttpResponse response,
                                               WebSocketHandler wsHandler,
                                               Exception ex) {}
                })
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request,
                                                      WebSocketHandler wsHandler,
                                                      Map<String, Object> attributes) {
                        Object userId = attributes.get("USER_ID");
                        if (userId != null) {
                            return new JwtHandshakePrincipal(userId.toString());
                        }
                        return null; // prevent anonymous
                    }
                })
                .withSockJS();
    }
}