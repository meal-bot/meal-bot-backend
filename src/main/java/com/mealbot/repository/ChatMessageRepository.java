package com.mealbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mealbot.entity.Chat;
import com.mealbot.entity.ChatMessage;
import com.mealbot.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** 채팅의 모든 메시지를 생성일 오름차순으로 조회. 채팅 상세 조회 등에서 사용. */
    List<ChatMessage> findByChatOrderByCreatedAt(Chat chat);

    /** 슬라이딩 윈도우용. 최근 50개를 DESC로 조회 후 호출부에서 오름차순 정렬 필요. */
    List<ChatMessage> findTop50ByChatOrderByCreatedAtDesc(Chat chat);

    /** Calendar 기능용: 날짜 범위 내 추천 결과가 있는 assistant 메시지 조회. */
    List<ChatMessage> findByChatUserAndRoleAndRecommendationsJsonIsNotNullAndCreatedAtBetweenOrderByCreatedAtDesc(
            User user, String role, LocalDateTime start, LocalDateTime end);
}