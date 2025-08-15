package com.giguniverse.backend.Chat.Controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Chat.Model.ChatMessage;
import com.giguniverse.backend.Chat.Model.ChatSession;
import com.giguniverse.backend.Chat.Model.ChatSessionDTO;
import com.giguniverse.backend.Chat.Model.ChatUserDTO;
import com.giguniverse.backend.Chat.Service.ChatService;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/users")
    public ResponseEntity<List<ChatUserDTO>> getAllChatUsers() {
        List<ChatUserDTO> users = chatService.getAllChatUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/sessions")
    public ResponseEntity<ChatSession> createChatSession(@RequestBody ChatSession chatSession) {
        ChatSession created = chatService.createChatSession(chatSession);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/mychat")
    public ResponseEntity<List<ChatSessionDTO>> getMyChatSessions() {
        List<ChatSessionDTO> sessions = chatService.getSessionsForUser();
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/messages/{chatId}")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable String chatId) {
        List<ChatMessage> messages = chatService.getMessagesByChatId(chatId);
        return ResponseEntity.ok(messages);
    }

    // Send a new message
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatMessage> sendMessage(
            @RequestParam String chatId,
            @RequestParam(required = false) String textContent,
            @RequestParam(required = false) List<MultipartFile> files) throws IOException {

        String userId = AuthUtil.getUserId();
        ChatMessage message = chatService.sendMessage(chatId, textContent, files, userId);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/me")
    public Map<String, String> checkUser() {
        Map<String, String> userInfo = chatService.getCurrentUser();

        Map<String, String> result = new HashMap<>();
        result.put("id", userInfo.getOrDefault("id", ""));
        result.put("email", userInfo.getOrDefault("email", ""));

        return result;
    }

    @DeleteMapping("/{chatId}/participants/{userId}")
    public ResponseEntity<?> removeParticipant(
            @PathVariable String chatId,
            @PathVariable String userId) {
        try {
            chatService.removeParticipant(chatId, userId);
            return ResponseEntity.ok().body(Map.of("message", "Successfully left the group"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to leave group"));
        }
    }

    @PatchMapping("/{chatId}/participants/{userId}/make-admin")
    public ResponseEntity<?> makeAdmin(
            @PathVariable String chatId,
            @PathVariable String userId) {
        try {
            chatService.makeAdmin(chatId, userId);
            return ResponseEntity.ok(Map.of("message", "User promoted to admin"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to promote user"));
        }
    }

    public static class AddParticipantsRequest {
        private List<String> userIds;
        public List<String> getUserIds() { return userIds; }
        public void setUserIds(List<String> userIds) { this.userIds = userIds; }
    }

    @PostMapping("/{chatId}/participants")
    public ResponseEntity<?> addParticipants(
            @PathVariable String chatId,
            @RequestBody AddParticipantsRequest request
    ) {
        return ResponseEntity.ok(chatService.addParticipants(chatId, request.getUserIds()));
    }

    @PutMapping("/{chatId}/rename")
    public ResponseEntity<ChatSession> renameGroup(
            @PathVariable String chatId,
            @RequestBody Map<String, String> body
    ) {
        String newName = body.get("name");
        if (newName == null || newName.isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }

        ChatSession updatedChat = chatService.renameGroup(chatId, newName);
        return ResponseEntity.ok(updatedChat);
    }
}
