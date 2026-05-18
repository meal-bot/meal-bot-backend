package com.mealbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mealbot.entity.Chat;
import com.mealbot.entity.ChatMessage;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 채팅의 모든 메시지를 생성일 오름차순으로 조회.
     * 채팅 상세 조회(getChat) 등에서 사용.
     */
    List<ChatMessage> findByChatOrderByCreatedAt(Chat chat);

    /**
     * 채팅의 최근 메시지 N개를 생성일 내림차순으로 조회.
     *
     * v0.3 슬라이딩 윈도우용. ChatService에서 사용 시 reverse 처리 필요.
     * 예: chatMessageRepository.findTop6ByChatOrderByCreatedAtDesc(chat)
     *     → 시간순(오름차순)으로 사용하려면 호출 후 Collections.reverse 또는
     *       stream.sorted(Comparator.comparing(ChatMessage::getCreatedAt))
     *
     * 메서드명 규약 (Spring Data JPA):
     * - findTop{N}By...OrderBy{Field}Desc: 상위 N개를 {Field} 기준 내림차순
     * - findTop6: LIMIT 6
     * - ByChat: WHERE chat = ?
     * - OrderByCreatedAtDesc: ORDER BY created_at DESC
     */
    List<ChatMessage> findTop6ByChatOrderByCreatedAtDesc(Chat chat);
}