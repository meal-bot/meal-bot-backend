package com.mealbot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자의 채팅 세션 단위. 여러 개의 ChatMessage를 묶는 대화 컨테이너.
 *
 * v0.3 변경: 슬롯 상태(mealTimes, purpose, freeText) 누적 보관 필드 추가.
 * AI 호출 시 ChatRequest.slots로 전달.
 */
@Entity
@Table(name = "chats")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    // ── v0.3 슬롯 누적 상태 ──────────────────────────────────────

    /**
     * v0.3 슬롯: 식사 시간대. 콤마로 join한 문자열로 저장.
     * 예: "저녁", "아침,점심"
     * 후보: 아침/점심/저녁/간식/야식
     * null이면 아직 미입력 상태.
     */
    @Column(name = "meal_times", length = 100)
    private String mealTimes;

    /**
     * v0.3 슬롯: 목적 enum.
     * 후보: light / protein / hearty / tasty
     * null이면 아직 미입력 상태.
     */
    @Column(name = "purpose", length = 20)
    private String purpose;

    /**
     * v0.3 슬롯: 자유 텍스트 누적.
     * Spring DB가 source of truth. 매 턴 사용자 발화에서 추출된 free_text_delta를 append.
     */
    @Column(name = "free_text", columnDefinition = "TEXT")
    private String freeText;
}