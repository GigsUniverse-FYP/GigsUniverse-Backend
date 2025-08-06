package com.giguniverse.backend.WebsocketConfiguration;



import org.springframework.context.annotation.Configuration;

import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OnlineStatusHandler onlineStatusHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor; 

    public WebSocketConfig(
        OnlineStatusHandler onlineStatusHandler,
        JwtHandshakeInterceptor jwtHandshakeInterceptor
    ) {
        this.onlineStatusHandler = onlineStatusHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(onlineStatusHandler, "/ws/online-status")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

}
