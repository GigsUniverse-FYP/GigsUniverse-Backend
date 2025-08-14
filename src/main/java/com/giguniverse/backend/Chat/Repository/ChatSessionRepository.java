package com.giguniverse.backend.Chat.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Chat.Model.ChatSession;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    List<ChatSession> findByParticipantsContaining(String userId);

    Optional<ChatSession> findByParticipantsIn(List<String> participants);

    
}

