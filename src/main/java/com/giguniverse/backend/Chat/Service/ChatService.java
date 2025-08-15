package com.giguniverse.backend.Chat.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.giguniverse.backend.Auth.Model.Employer;
import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Repository.AdminRepository;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Chat.Model.ChatMessage;
import com.giguniverse.backend.Chat.Model.ChatSession;
import com.giguniverse.backend.Chat.Model.ChatSessionDTO;
import com.giguniverse.backend.Chat.Model.ChatUserDTO;
import com.giguniverse.backend.Chat.Model.ChatUserInfoDTO;
import com.giguniverse.backend.Chat.Model.LastMessageInfo;
import com.giguniverse.backend.Chat.Repository.ChatMessageRepository;
import com.giguniverse.backend.Chat.Repository.ChatSessionRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
public class ChatService {

    private final EmployerRepository employerRepository;
    private final FreelancerRepository freelancerRepository;
    private final AdminRepository adminRepository;

    public ChatService(
            EmployerRepository employerRepository,
            FreelancerRepository freelancerRepository,
            AdminRepository adminRepository) {
        this.employerRepository = employerRepository;
        this.freelancerRepository = freelancerRepository;
        this.adminRepository = adminRepository;
    }

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<ChatUserDTO> getAllChatUsers() {
        List<ChatUserDTO> allUsers = new ArrayList<>();

        String currentUserId = AuthUtil.getUserId();

        employerRepository.findAll().stream()
                .filter(Employer::isCompletedOnboarding)
                .filter(emp -> !emp.getEmployerUserId().equals(currentUserId))
                .forEach(emp -> {
                    String fullName = emp.getProfile() != null ? emp.getProfile().getFullName() : null;
                    String username = emp.getProfile() != null ? emp.getProfile().getUsername() : null;
                    String base64Image = emp.getProfile() != null ? encodeImage(emp.getProfile().getProfilePicture())
                            : null;
                    String imageMimeType = emp.getProfile() != null ? emp.getProfile().getProfilePictureMimeType()
                            : null;

                    allUsers.add(new ChatUserDTO(
                            emp.getEmployerUserId(),
                            fullName,
                            username,
                            "Employer",
                            base64Image,
                            imageMimeType));
                });

        freelancerRepository.findAll().stream()
                .filter(Freelancer::isCompletedOnboarding)
                .filter(free -> !free.getFreelancerUserId().equals(currentUserId))
                .forEach(free -> {
                    String fullName = free.getProfile() != null ? free.getProfile().getFullName() : null;
                    String username = free.getProfile() != null ? free.getProfile().getUsername() : null;
                    String base64Image = free.getProfile() != null ? encodeImage(free.getProfile().getProfilePicture())
                            : null;
                    String imageMimeType = free.getProfile() != null ? free.getProfile().getProfilePictureMimeType()
                            : null;

                    allUsers.add(new ChatUserDTO(
                            free.getFreelancerUserId(),
                            fullName,
                            username,
                            "Freelancer",
                            base64Image,
                            imageMimeType));
                });

        adminRepository.findAll().stream()
                .filter(admin -> !admin.getAdminUserId().equals(currentUserId)) // exclude current user
                .forEach(admin -> {
                    String fullName = admin.getProfile() != null ? admin.getProfile().getFullName() : null;
                    String username = admin.getProfile() != null ? admin.getProfile().getUsername() : null;
                    String base64Image = admin.getProfile() != null
                            ? encodeImage(admin.getProfile().getProfilePicture())
                            : null;
                    String imageMimeType = admin.getProfile() != null ? admin.getProfile().getProfilePictureMimeType()
                            : null;

                    allUsers.add(new ChatUserDTO(
                            admin.getAdminUserId(),
                            fullName,
                            username,
                            "Admin",
                            base64Image,
                            imageMimeType));
                });
        return allUsers;
    }

    private String encodeImage(byte[] imageBytes) {
        if (imageBytes == null)
            return null;
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    public ChatSession createChatSession(ChatSession chatSession) {
        String authenticatedUserId = AuthUtil.getUserId();

        if (chatSession.getParticipants() == null) {
            chatSession.setParticipants(new ArrayList<>());
        }

        if (!chatSession.getParticipants().contains(authenticatedUserId)) {
            chatSession.getParticipants().add(authenticatedUserId);
        }

        chatSession.setParticipants(
                chatSession.getParticipants().stream().distinct().collect(Collectors.toList()));

        // Set timestamps
        Instant now = Instant.now();
        chatSession.setCreatedAt(now);
        chatSession.setUpdatedAt(now);

        // Initialize unread count for all participants
        Map<String, Integer> unreadMap = new HashMap<>();
        for (String participantId : chatSession.getParticipants()) {
            String safeKey = participantId.replace(".", "\uFF0E");
            unreadMap.put(safeKey, 0);
        }
        chatSession.setUnreadCount(unreadMap);

        // Assign roles for group chats
        if (chatSession.isGroupChat()) {
            Map<String, String> roles = new HashMap<>();
            for (String participantId : chatSession.getParticipants()) {
                String safeKey = participantId.replace(".", "\uFF0E");
                if (participantId.equals(authenticatedUserId)) {
                    roles.put(safeKey, "admin");
                } else {
                    roles.put(safeKey, "member");
                }
            }
            chatSession.setRoles(roles);
        } else {
            chatSession.setRoles(null);
        }

        return chatSessionRepository.save(chatSession);
    }

    @Autowired
    private ChatProfileService userProfileService;

    public List<ChatSessionDTO> getSessionsForUser() {
        String authUserId = AuthUtil.getUserId();

        List<ChatSession> sessions = chatSessionRepository.findByParticipantsContaining(authUserId);

        return sessions.stream().map(session -> {
            ChatSessionDTO dto = new ChatSessionDTO();

            // Raw fields
            dto.setId(session.getId());
            dto.setGroupChat(session.isGroupChat());
            dto.setGroupName(session.getGroupName());
            dto.setCreatedAt(session.getCreatedAt());
            dto.setUpdatedAt(session.getUpdatedAt());

            // Enrich participants -> ChatUserInfoDTO (must include avatar & online)
            List<ChatUserInfoDTO> enrichedParticipants = session.getParticipants().stream()
                    .map(userProfileService::getUserInfoById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            dto.setParticipants(enrichedParticipants);

            // Restore unread count and roles if needed
            Map<String, Integer> unreadRestored = restoreKeys(session.getUnreadCount());
            Map<String, String> rolesRestored = restoreKeys(session.getRoles());

            dto.setUnreadCount(unreadRestored);
            dto.setRoles(rolesRestored);

            // Last message
            dto.setLastMessage(session.getLastMessage());

            // UI-friendly computed fields
            dto.setCurrentUserId(authUserId);
            dto.setType(session.isGroupChat() ? "group" : "direct");

            dto.setUnreadForCurrentUser(unreadRestored != null ? unreadRestored.getOrDefault(authUserId, 0) : 0);

            // Reset UI fields to null/default
            dto.setDisplayName(null);
            dto.setOtherUserId(null);
            dto.setDisplayAvatar(null);
            dto.setDisplayAvatarInitials(null);
            dto.setOtherUsername(null);
            dto.setOtherIsOnline(null);

            if (session.isGroupChat()) {
                // Group chat display
                String display = session.getGroupName() != null ? session.getGroupName() : "Unnamed Group";
                dto.setDisplayName(display);
                dto.setDisplayAvatar(null);
                dto.setDisplayAvatarInitials(makeInitials(display));

                // No "other user" for group chats
            } else {
                // Direct chat: find the other participant
                String otherId = session.getParticipants().stream()
                        .filter(id -> !id.equals(authUserId))
                        .findFirst()
                        .orElse(authUserId);
                dto.setOtherUserId(otherId);

                ChatUserInfoDTO other = userProfileService.getUserInfoById(otherId);

                String otherFullName = other != null && other.getFullName() != null ? other.getFullName()
                        : "Unknown User";
                String otherUsername = other != null && other.getUsername() != null ? other.getUsername() : "unknown";

                dto.setDisplayName(otherFullName); // full name for display
                dto.setOtherUsername(otherUsername); // separate username field
                dto.setDisplayAvatar(other != null ? other.getAvatar() : null);
                dto.setDisplayAvatarInitials(makeInitials(otherFullName));
                dto.setOtherIsOnline(other != null ? other.isOnline() : false);
            }

            return dto;
        }).collect(Collectors.toList());
    }

    private static <V> Map<String, V> restoreKeys(Map<String, V> src) {
        if (src == null)
            return null;
        return src.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> unescapeKey(e.getKey()),
                        Map.Entry::getValue));
    }

    private static String unescapeKey(String key) {
        if (key == null)
            return null;
        return key.replace('\uFF0E', '.'); // restore fullwidth dot to '.'
    }

    private static String makeInitials(String text) {
        if (text == null || text.isBlank())
            return "??";
        String[] parts = text.trim().split("\\s+");
        String first = parts[0].substring(0, 1).toUpperCase();
        String second = parts.length > 1 ? parts[1].substring(0, 1).toUpperCase() : "";
        return (first + second);
    }

    @Autowired
    private ChatMessageRepository messageRepository;
    @Autowired
    private ChatSessionRepository sessionRepository;

    // Get messages for a chat session
    public List<ChatMessage> getMessagesByChatId(String chatId) {
        return messageRepository.findByChatIdOrderByTimestampAsc(chatId);
    }

    // Send a new message
    public ChatMessage sendMessage(String chatId, String textContent, List<MultipartFile> files, String senderId)
            throws IOException {

        ChatSession session = sessionRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));

        // Determine receiverId for private chats
        String receiverId = null;
        if (!session.isGroupChat()) {
            receiverId = session.getParticipants().stream()
                    .filter(participant -> !participant.equals(senderId))
                    .findFirst()
                    .orElse(null);
        }
        // Create new message
        ChatMessage message = new ChatMessage();
        message.setChatId(chatId);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId); // Set receiver
        message.setTextContent(textContent);
        message.setTimestamp(Instant.now());
        message.setRead(false);

        // Process files
        if (files != null && !files.isEmpty()) {
            List<String> fileNames = new ArrayList<>();
            List<String> fileContents = new ArrayList<>();
            List<String> fileTypes = new ArrayList<>();
            List<Long> fileSizes = new ArrayList<>();

            for (MultipartFile file : files) {
                fileNames.add(file.getOriginalFilename());
                fileTypes.add(file.getContentType());
                fileSizes.add(file.getSize());
                fileContents.add(Base64.getEncoder().encodeToString(file.getBytes()));
            }

            message.setFileName(fileNames);
            message.setFileType(fileTypes);
            message.setFileSizes(fileSizes);
            message.setFileContent(fileContents);
        }

        // Save message
        ChatMessage savedMessage = messageRepository.save(message);

        // Update chat session
        updateChatSession(chatId, savedMessage);

        if (!session.isGroupChat() && receiverId != null) {
            messagingTemplate.convertAndSendToUser(
                    receiverId,
                    "/queue/messages",
                    savedMessage);
        } else {
            for (String participant : session.getParticipants()) {
                if (!participant.equals(senderId)) {
                    messagingTemplate.convertAndSendToUser(
                            participant,
                            "/queue/messages",
                            savedMessage);
                }
            }
        }

        return savedMessage;
    }

    private void updateChatSession(String chatId, ChatMessage message) {
        ChatSession session = sessionRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));

        // Update last message info
        LastMessageInfo lastMessage = new LastMessageInfo();
        lastMessage.setMessageId(message.getId());
        lastMessage.setSenderId(message.getSenderId());
        lastMessage.setTimestamp(message.getTimestamp());

        if (message.getTextContent() != null && !message.getTextContent().isEmpty()) {
            lastMessage.setContentPreview(
                    message.getTextContent().length() > 50 ? message.getTextContent().substring(0, 50) + "..."
                            : message.getTextContent());
        } else if (message.getFileName() != null && !message.getFileName().isEmpty()) {
            lastMessage.setContentPreview("[File]");
        } else {
            lastMessage.setContentPreview("");
        }

        session.setLastMessage(lastMessage);

        // Update unread counts
        Map<String, Integer> unreadCount = session.getUnreadCount();
        if (unreadCount == null) {
            unreadCount = new HashMap<>();
        }

        String safeSenderId = escapeKey(message.getSenderId()); // FIX: Escape senderId

        for (String participant : session.getParticipants()) {
            String safeParticipant = escapeKey(participant); // FIX: Escape every participant ID
            if (!safeParticipant.equals(safeSenderId)) {
                unreadCount.compute(safeParticipant, (key, count) -> (count == null) ? 1 : count + 1);
            }
        }

        session.setUnreadCount(unreadCount);
        session.setUpdatedAt(Instant.now());

        sessionRepository.save(session);
    }

    private static String escapeKey(String key) {
        return key == null ? null : key.replace(".", "\uFF0E");
    }

    public Map<String, String> getCurrentUser() {
        String userId = AuthUtil.getUserId();
        String email = AuthUtil.getUserEmail();

        Map<String, String> response = new HashMap<>();
        response.put("id", userId != null ? userId : ""); // CHANGED "userId" -> "id"
        response.put("email", email != null ? email : "");

        return response;
    }

    public ChatSession getChatSession(String chatId) {
        return sessionRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    public void markMessagesAsRead(String chatId, String userId) {
        System.out.println("markMessagesAsRead called: chatId=" + chatId + ", userId=" + userId);

        // Update messages
        Query query = new Query();
        query.addCriteria(Criteria.where("chatId").is(chatId)
                .and("receiverId").is(userId)
                .and("read").is(false));

        Update update = new Update().set("read", true);
        mongoTemplate.updateMulti(query, update, ChatMessage.class);

        // Reset unread count
        ChatSession session = getChatSession(chatId);
        System.out.println("Before updating unread: " + session.getUnreadCount());

        if (session.getUnreadCount() == null) {
            session.setUnreadCount(new HashMap<>());
        }

        Map<String, Integer> unreadCount = session.getUnreadCount();
        if (unreadCount != null) {
            String safeUserId = escapeKey(userId);
            unreadCount.put(safeUserId, 0);
            session.setUnreadCount(unreadCount);
            sessionRepository.save(session);
        }

        System.out.println("After updating unread: " + session.getUnreadCount());
        System.out.println("Unread count after update: " + unreadCount);

        for (String participant : session.getParticipants()) {
            System.out.println("Sending chat update to: " + participant);
            messagingTemplate.convertAndSendToUser(
                    participant,
                    "/queue/chat-updates",
                    convertToDto(session, participant));
        }
    }

    public ChatSessionDTO convertToDto(ChatSession session, String currentUserId) {
        ChatSessionDTO dto = new ChatSessionDTO();
        dto.setId(session.getId());
        dto.setGroupChat(session.isGroupChat());
        dto.setGroupName(session.getGroupName());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        dto.setCurrentUserId(currentUserId);

        List<ChatUserInfoDTO> participants = session.getParticipants().stream()
                .map(userProfileService::getUserInfoById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        dto.setParticipants(participants);

        Map<String, Integer> unreadEscaped = session.getUnreadCount();
        Map<String, Integer> unreadRestored = restoreKeys(unreadEscaped);
        if (unreadRestored == null)
            unreadRestored = new HashMap<>();
        dto.setUnreadCount(unreadRestored);
        dto.setUnreadForCurrentUser(unreadRestored.getOrDefault(currentUserId, 0));

        dto.setRoles(restoreKeys(session.getRoles()));

        dto.setLastMessage(session.getLastMessage());
        dto.setType(session.isGroupChat() ? "group" : "direct");

        if (!session.isGroupChat()) {
            String otherId = session.getParticipants().stream()
                    .filter(id -> !id.equals(currentUserId))
                    .findFirst().orElse(currentUserId);
            dto.setOtherUserId(otherId);
            ChatUserInfoDTO other = userProfileService.getUserInfoById(otherId);
            if (other != null) {
                dto.setDisplayName(other.getFullName());
                dto.setDisplayAvatar(other.getAvatar());
                dto.setOtherUsername(other.getUsername());
                dto.setOtherIsOnline(other.isOnline());
            }
        } else {
            String display = session.getGroupName() != null ? session.getGroupName() : "Unnamed Group";
            dto.setDisplayName(display);
            dto.setDisplayAvatarInitials(makeInitials(display));
        }

        return dto;
    }

    public void removeParticipant(String chatId, String userId) {
        ChatSession chat = chatSessionRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found"));

        // Remove from participants
        boolean removed = chat.getParticipants().removeIf(p -> p.equals(userId));
        if (!removed) {
            throw new EntityNotFoundException("User not in chat");
        }

        // Convert userId to safe key (replace '.' with fullwidth)
        String safeKey = userId.replace(".", "\uFF0E");

        // Remove from roles map
        if (chat.getRoles() != null && chat.getRoles().containsKey(safeKey)) {
            Map<String, String> updatedRoles = new HashMap<>(chat.getRoles());
            updatedRoles.remove(safeKey);
            chat.setRoles(updatedRoles);
        }

        // Remove from unreadCount map
        if (chat.getUnreadCount() != null && chat.getUnreadCount().containsKey(safeKey)) {
            Map<String, Integer> updatedUnread = new HashMap<>(chat.getUnreadCount());
            updatedUnread.remove(safeKey);
            chat.setUnreadCount(updatedUnread);
        }

        // Save updated chat
        chatSessionRepository.save(chat);
    }


    public void makeAdmin(String chatId, String userId) {
        ChatSession chat = chatSessionRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found"));

        if (chat.getParticipants() == null || !chat.getParticipants().contains(userId)) {
            throw new EntityNotFoundException("User not in chat");
        }

        if (chat.getRoles() == null) {
            chat.setRoles(new HashMap<>());
        }

        // Replace '.' with fullwidth character to safely use as map key
        String safeKey = userId.replace(".", "\uFF0E");

        // Check if already admin
        if ("admin".equals(chat.getRoles().get(safeKey))) {
            throw new IllegalStateException("User is already an admin");
        }

        chat.getRoles().put(safeKey, "admin");
        chatSessionRepository.save(chat);
    }

    @Autowired
    private ChatProfileService chatProfileService;

    public ChatSessionDTO addParticipants(String chatId, List<String> userIds) {
        ChatSession chat = chatSessionRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found"));

        if (chat.getParticipants() == null) chat.setParticipants(new ArrayList<>());
        if (chat.getRoles() == null) chat.setRoles(new HashMap<>());
        if (chat.getUnreadCount() == null) chat.setUnreadCount(new HashMap<>());

        for (String userId : userIds) {
            if (!chat.getParticipants().contains(userId)) {
                chat.getParticipants().add(userId);

                String safeKey = userId.replace(".", "\uFF0E");

                // Only add role if it doesn't already exist
                chat.getRoles().putIfAbsent(safeKey, "member");
                chat.getUnreadCount().putIfAbsent(safeKey, 0);
            }
        }

        chatSessionRepository.save(chat);

        // Build participant DTOs with correct chat role
        List<ChatUserInfoDTO> participants = chat.getParticipants().stream()
                .map(chatProfileService::getUserInfoById)
                .map(u -> {
                    String safeKey = u.getUserId().replace(".", "\uFF0E");
                    String chatRole = chat.getRoles().getOrDefault(safeKey, "member");
                    u.setRole(chatRole);
                    return u;
                })
                .toList();

        return buildChatSessionDTO(chat, participants);
    }

    private ChatSessionDTO buildChatSessionDTO(ChatSession chat, List<ChatUserInfoDTO> participants) {
        ChatSessionDTO dto = new ChatSessionDTO();
        dto.setId(chat.getId());
        dto.setLastMessage(chat.getLastMessage());
        dto.setParticipants(participants);
        dto.setRoles(chat.getRoles());
        dto.setUnreadCount(chat.getUnreadCount());
        dto.setGroupChat(chat.isGroupChat());
        dto.setGroupName(chat.getGroupName());
        dto.setCreatedAt(chat.getCreatedAt());
        dto.setUpdatedAt(chat.getUpdatedAt());
        return dto;
    }

}