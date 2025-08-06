package com.giguniverse.backend.Onboarding.Sumsub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;

import java.security.Principal;
import java.util.Map;

@Component
public class SumsubStatusNotifier {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String email = principal.getName();
            System.out.println("Connected WebSocket principal: " + email);

            messagingTemplate.convertAndSendToUser(
                email,
                "/queue/sumsub-status",
                Map.of("status", "test-connected", "isDuplicate", false)
            );
        }
    }

    public void notifyUserStatus(String userEmail, String status, boolean isDuplicate) {
        System.out.println("📡 Notifying via WebSocket to user: " + userEmail);
        messagingTemplate.convertAndSendToUser(
            userEmail,
            "/queue/sumsub-status",
            Map.of(
                "status", status,
                "isDuplicate", isDuplicate
            )
        );
    }
}
