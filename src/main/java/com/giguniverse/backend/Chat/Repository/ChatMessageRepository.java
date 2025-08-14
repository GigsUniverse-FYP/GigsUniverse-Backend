package com.giguniverse.backend.Chat.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Chat.Model.ChatMessage;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByChatIdOrderByTimestampAsc(String chatId);

    @Query("{ 'chatId': ?0, 'read': false, 'senderId': { $ne: ?1 } }")
    List<ChatMessage> findUnreadMessages(String chatId, String userId);

}
