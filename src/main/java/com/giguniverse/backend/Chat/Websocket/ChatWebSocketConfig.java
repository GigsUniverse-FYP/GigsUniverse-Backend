package com.giguniverse.backend.Chat.Websocket;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.giguniverse.backend.Auth.JWT.JwtUserPrincipal;
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
                    protected Principal determineUser(
                        ServerHttpRequest request,
                        WebSocketHandler wsHandler,
                        Map<String, Object> attributes
                    ) {
                        String userId = (String) attributes.get("USER_ID");
                        String email = (String) attributes.get("USER_EMAIL");
                        String role = (String) attributes.get("USER_ROLE");
                        
                        if (userId != null) {
                            // Create authorities from role
                            Collection<SimpleGrantedAuthority> authorities = Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + role)
                            );
                            
                            // Create UserDetails principal
                            JwtUserPrincipal userPrincipal = new JwtUserPrincipal(
                                userId, email, role, authorities
                            );
                            
                            // Create full authentication object
                            Authentication auth = new UsernamePasswordAuthenticationToken(
                                userPrincipal, null, authorities
                            );
                            return auth;
                        }
                        return null;
                    }
                })
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && accessor.getUser() instanceof Authentication) {
                    SecurityContextHolder.getContext().setAuthentication(
                        (Authentication) accessor.getUser()
                    );
                }
                return message;
            }
        });
    }
}
