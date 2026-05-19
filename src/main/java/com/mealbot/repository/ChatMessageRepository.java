package com.mealbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mealbot.entity.Chat;
import com.mealbot.entity.ChatMessage;
import com.mealbot.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatOrderByCreatedAt(Chat chat);

    List<ChatMessage> findByChatUserAndRoleAndHasRecommendationTrueAndCreatedAtBetweenOrderByCreatedAtDesc(
            User user, String role, LocalDateTime start, LocalDateTime end);
}