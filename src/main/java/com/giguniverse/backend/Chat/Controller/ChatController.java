package com.giguniverse.backend.Chat.Controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.giguniverse.backend.Chat.Model.ChatMessage;
import com.giguniverse.backend.Chat.Model.ChatSession;
import com.giguniverse.backend.Chat.Model.ChatSessionDTO;
import com.giguniverse.backend.Chat.Model.ChatUserDTO;
import com.giguniverse.backend.Chat.Service.ChatService;

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
        
        ChatMessage message = chatService.sendMessage(chatId, textContent, files);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/me")
    public Map<String, String> checkUser() {
        Map<String, String> userInfo = chatService.getCurrentUser(); // <-- service returns both

        // Make sure nulls are handled
        Map<String, String> result = new HashMap<>();
        result.put("id", userInfo.getOrDefault("id", ""));
        result.put("email", userInfo.getOrDefault("email", ""));
        
        return result;
    }
}
