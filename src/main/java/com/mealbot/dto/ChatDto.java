package com.mealbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 클라이언트(웹/앱) 노출용 채팅 DTO.
 *
 * AI 통신용 DTO(AiDto)와 분리. 레이어 책임:
 * - AiDto: Spring ↔ FastAPI 내부 통신
 * - ChatDto: Spring ↔ 클라이언트 노출
 *
 * v0.3 변경 사항:
 * - 게스트 모드 비활성화 (GuestSendRequest, GuestSendResponse 주석 처리, 추후 보완 예정)
 * - SendResponse를 record로 교체, v0.3 응답 구조(intent/answer/recommendations/flags) 반영
 * - Recommendation, Flags record 신규
 */
public class ChatDto {

    private ChatDto() {
        // 유틸 컨테이너 — 인스턴스화 금지
    }

    // ── 요청 ────────────────────────────────────────────────

    /** POST /api/chat/{chatId}/sendMessage 요청 바디. */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRequest {
        private String content;
    }

    // ── 응답: 메시지 전송 (v0.3) ──────────────────────────────

    /**
     * 추천 결과 단건. AI 응답을 클라이언트에 그대로 노출.
     *
     * @param recipeId        정규화된 recipe id (예: "42")
     * @param name            메뉴명
     * @param cookingTime     조리 시간 (분)
     * @param summary         메뉴 요약
     * @param mainIngredients 주재료
     * @param reason          추천 이유 (LLM 생성)
     */
    public record Recommendation(
            String recipeId,
            String name,
            Integer cookingTime,
            String summary,
            List<String> mainIngredients,
            String reason
    ) {}

    /**
     * 응답 플래그 3종.
     *
     * @param needsMoreSlots true이면 추가 슬롯 입력 필요 UX
     * @param outOfScope     true이면 서비스 범위 밖 안내
     * @param isFallback     true이면 내부 fallback 경로
     */
    public record Flags(
            boolean needsMoreSlots,
            boolean outOfScope,
            boolean isFallback
    ) {}

    /**
     * 메시지 전송 응답 (v0.3).
     *
     * @param messageId       Spring DB에 저장된 assistant 메시지 PK
     * @param intent          최종 분류 의도 (recommend|slot_fill|refine|ask)
     * @param answer          사용자에게 보여줄 응답 텍스트
     * @param recommendations 추천 결과 (0개 또는 2개)
     * @param flags           응답 플래그 3종
     */
    public record SendResponse(
            Long messageId,
            String intent,
            String answer,
            List<Recommendation> recommendations,
            Flags flags
    ) {}

    // ── 응답: 채팅 목록/상세 (기존 유지) ──────────────────────

    /** 채팅 생성/목록 응답 (사이드바용). */
    @Getter
    @AllArgsConstructor
    public static class ChatResponse {
        private Long chatId;
        private String title;
        private LocalDateTime createdAt;
    }

    /** 메시지 단건 응답. */
    @Getter
    @AllArgsConstructor
    public static class ChatMessageResponse {
        private Long messageId;
        private String role;
        private String content;
        private LocalDateTime createdAt;
    }

    /** 채팅 상세 조회 응답 (이전 대화 복원용). */
    @Getter
    @AllArgsConstructor
    public static class ChatDetailResponse {
        private Long chatId;
        private String title;
        private LocalDateTime createdAt;
        private List<ChatMessageResponse> messages;
    }

    // ── 게스트 모드 (v0.3 비활성화, 추후 보완 예정) ──────────────

    // /** 게스트 메시지 단건 (role + content). */
    // @Getter @NoArgsConstructor @AllArgsConstructor
    // public static class ChatMessageRequest {
    //     private String role;
    //     private String content;
    // }

    // /** POST /api/chat/guest/sendMessage 요청 바디. */
    // @Getter @NoArgsConstructor @AllArgsConstructor
    // public static class GuestSendRequest {
    //     private List<ChatMessageRequest> messages;
    // }

    // /** POST /api/chat/guest/sendMessage 응답 바디. */
    // @Getter @AllArgsConstructor
    // public static class GuestSendResponse {
    //     private String reply;
    //     private List<AiDto.RecipeResult> results;
    // }
}