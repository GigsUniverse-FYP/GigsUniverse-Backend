package com.giguniverse.backend.WebsocketConfiguration;

import org.springframework.web.socket.WebSocketSession;

public class WebSocketSessionInfo {
    private final String userId;
    private final String role;
    private final WebSocketSession session;

    public WebSocketSessionInfo(String userId, String role, WebSocketSession session) {
        this.userId = userId;
        this.role = role;
        this.session = session;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public WebSocketSession getSession() {
        return session;
    }
}
