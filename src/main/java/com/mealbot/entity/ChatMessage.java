package com.mealbot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 채팅 세션(Chat) 내 단건 메시지. role로 발화자(user/assistant)를 구분한다.
 *
 * v0.3 변경: assistant 메시지에 그 턴의 추천 결과를 JSON으로 저장하는 컬럼 추가.
 * ChatService가 다음 턴 AI 호출 시 직전 assistant 메시지의 추천을 lastRecommendations로 전달.
 */
@Entity
@Table(name = "chat_messages")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @Column(nullable = false)
    private String role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ── v0.3 추천 결과 ──────────────────────────────────────────

    /**
     * v0.3 assistant 메시지의 추천 결과 (JSON 직렬화 문자열).
     *
     * 형식: List<Recommendation>을 ObjectMapper로 JSON 직렬화한 문자열.
     * 예: '[{"recipeId":"42","name":"된장찌개",...},{"recipeId":"43",...}]'
     *
     * 저장 정책:
     * - user 메시지: 항상 null
     * - assistant 메시지 (recommend/refine intent): 2개 추천 JSON
     * - assistant 메시지 (slot_fill/ask intent): 추천 없음 → null
     *
     * 다음 턴 AI 호출 시 ChatService가 직전 assistant 메시지에서 이 값을 꺼내
     * AiDto.Request.lastRecommendations로 변환해 전달.
     */
    @Column(name = "recommendations_json", columnDefinition = "TEXT")
    private String recommendationsJson;
}
