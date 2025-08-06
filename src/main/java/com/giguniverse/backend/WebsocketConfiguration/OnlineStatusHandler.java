package com.giguniverse.backend.WebsocketConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


@Component
public class OnlineStatusHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSessionInfo> activeSessions = new ConcurrentHashMap<>();
    private final OnlineStatusService onlineStatusService;

    public OnlineStatusHandler(OnlineStatusService onlineStatusService) {
        this.onlineStatusService = onlineStatusService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equals(message.getPayload())) {
            session.sendMessage(new TextMessage("pong")); 
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        String role = (String) session.getAttributes().get("role");

        if (userId != null && role != null) {
            WebSocketSessionInfo info = new WebSocketSessionInfo(userId, role, session);
            activeSessions.put(userId, info);
            onlineStatusService.updateOnlineStatusByRole(userId, role, true);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocketSessionInfo sessionInfo = null;

        for (Map.Entry<String, WebSocketSessionInfo> entry : activeSessions.entrySet()) {
            if (entry.getValue().getSession().equals(session)) {
                sessionInfo = entry.getValue();
                activeSessions.remove(entry.getKey());
                break;
            }
        }

        if (sessionInfo != null) {
            onlineStatusService.updateOnlineStatusByRole(
                sessionInfo.getUserId(),
                sessionInfo.getRole(),
                false
            );
        }
    }

    public boolean isOnline(String userId) {
        return activeSessions.containsKey(userId);
    }
}
