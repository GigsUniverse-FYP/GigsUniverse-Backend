package com.giguniverse.backend.Onboarding.Stripe_Express;

import java.security.Principal;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class StripeStatusNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public StripeStatusNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String email = principal.getName();
            System.out.println("Connected WebSocket principal: " + email);

            messagingTemplate.convertAndSendToUser(
                email,
                "/queue/stripe-status",
                Map.of(
                    "stripeStatus", "testing",      
                    "completedPaymentSetup", false,
                    "payoutsEnabled", false
                )
            );
        }
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String user = (sha.getUser() != null) ? sha.getUser().getName() : "null";
        System.out.println("Client subscribed to " + sha.getDestination() + " as " + user);
    }

    public void notifyUserStatus(
        String userEmail,
        String stripeStatus,
        boolean completedPaymentSetup,
        boolean payoutsEnabled
    ) {
        System.out.println("Sending WebSocket update to " + userEmail);

        messagingTemplate.convertAndSendToUser(
            userEmail,
            "/queue/stripe-status",
            Map.of(
                "stripeStatus", stripeStatus,
                "completedPaymentSetup", completedPaymentSetup,
                "payoutsEnabled", payoutsEnabled
            )
        );
    }
}
