package com.giguniverse.backend.Chat.Websocket;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.giguniverse.backend.Auth.JWT.JwtUtil;



@Configuration
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${frontend.url}")
    private String frontendURL;

    private final JwtUtil jwtUtil;

    public ChatWebSocketConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(frontendURL)
                .addInterceptors(new ChatJwtHandshakeInterceptor(jwtUtil))
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
                                                      org.springframework.web.socket.WebSocketHandler wsHandler,
                                                      Map<String, Object> attributes) {
                        String userId = (String) attributes.get("USER_ID");
                        if (userId != null) {
                            return () -> userId; // Lambda Principal
                        }
                        return null;
                    }
                })
                .withSockJS();
    }
}
