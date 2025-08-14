package com.giguniverse.backend.Chat.Controller;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import com.giguniverse.backend.Auth.JWT.JwtUserPrincipal;
// Changed import
import com.giguniverse.backend.Chat.Model.Base64DecodingMultipartFile;
import com.giguniverse.backend.Chat.Model.ChatMessage;
import com.giguniverse.backend.Chat.Model.ChatSession;
import com.giguniverse.backend.Chat.Model.FileRequest;
import com.giguniverse.backend.Chat.Model.MessageRequest;
import com.giguniverse.backend.Chat.Service.ChatService;

@Controller
public class ChatWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    @MessageMapping("/chat/{chatId}/send")
    public void sendMessage(@DestinationVariable String chatId, @Payload MessageRequest messageRequest, Principal principal) 
            throws IOException {

        JwtUserPrincipal user = (JwtUserPrincipal) ((Authentication) principal).getPrincipal();
        String userId = user.getUserId();
        
        ChatMessage message = chatService.sendMessage(
            chatId,
            messageRequest.getTextContent(),
            convertToMultipartFiles(messageRequest.getFiles()),
            userId
        );

        // Send to all participants except sender
        ChatSession session = chatService.getChatSession(chatId);
        for (String participant : session.getParticipants()) {
            if (!participant.equals(userId)) {
                messagingTemplate.convertAndSendToUser(
                    participant,
                    "/queue/messages",
                    message
                );
            }
        }

        // Update chat list for all participants
        ChatSession updatedSession = chatService.getChatSession(chatId);
        for (String participant : session.getParticipants()) {
            messagingTemplate.convertAndSendToUser(
                participant,
                "/queue/chat-updates",
                chatService.convertToDto(updatedSession, participant)
            );
        }
    }

    @MessageMapping("/chat/{chatId}/read")
    public void markAsRead(@DestinationVariable String chatId, Principal principal) {
        JwtUserPrincipal user = (JwtUserPrincipal) ((Authentication) principal).getPrincipal();
        String userId = user.getUserId();
        chatService.markMessagesAsRead(chatId, userId);
        
        // Update chat list
        ChatSession updatedSession = chatService.getChatSession(chatId);
        messagingTemplate.convertAndSendToUser(
            userId,
            "/queue/chat-updates",
            chatService.convertToDto(updatedSession, userId)
        );
    }

    private List<MultipartFile> convertToMultipartFiles(List<FileRequest> files) {
        if (files == null) return new ArrayList<>();
        
        return files.stream()
            .filter(Objects::nonNull)
            .map(file -> {
                try {
                    return new Base64DecodingMultipartFile(
                        file.getContent(),
                        file.getName(),
                        file.getType()
                    );
                } catch (Exception e) {
                    logger.error("Error decoding file content", e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}