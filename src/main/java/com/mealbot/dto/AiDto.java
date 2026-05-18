package com.mealbot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Python AI 서버 v0.3 /chat 엔드포인트 통신용 DTO.
 *
 * 기존 v0.2 /recommend API는 폐기되었고 /chat 단일 엔드포인트로 통합됨.
 * 스키마는 FastAPI 서버의 api/schemas.py v0.3와 1:1 매핑.
 */
public class AiDto {

    private AiDto() {
        // 유틸 컨테이너 — 인스턴스화 금지
    }

    /**
     * 대화 히스토리 한 메시지.
     *
     * 클래스명 충돌 회피: Spring 엔티티 ChatMessage와 다름.
     * AI 통신용 DTO는 이 inner record로 한정.
     *
     * @param role    "user" | "assistant"
     * @param content 메시지 본문
     */
    public record Message(String role, String content) {}

    /**
     * 현재 슬롯 스냅샷. 입력/출력 공용.
     *
     * @param mealTimes 식사 시간대 (아침/점심/저녁/간식/야식 중 복수 가능). null 허용.
     * @param purpose   목적 (light/protein/hearty/tasty 중 단일). null 허용.
     * @param freeText  누적된 자유 텍스트. Spring DB가 누적 관리. null 허용.
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Slots(
            @JsonProperty("meal_times") List<String> mealTimes,
            String purpose,
            @JsonProperty("free_text") String freeText
    ) {}

    /**
     * 직전 턴 추천의 최소 정보. refine/ask 분기 판단용.
     *
     * @param recipeId 정규화된 recipe id (예: "42", "recipe_42" 형식 금지)
     * @param name     메뉴명
     */
    public record LastRecommendation(
            @JsonProperty("recipe_id") String recipeId,
            String name
    ) {}

    /**
     * 응답에 들어가는 추천 결과 단건.
     *
     * @param recipeId        정규화된 recipe id
     * @param name            메뉴명
     * @param summary         메뉴 요약
     * @param mainIngredients 주재료 리스트
     * @param cookingTime     조리 시간(분). null 허용.
     * @param reason          이 메뉴를 추천한 이유 (LLM 생성, 10~200자)
     */
    public record Recommendation(
            @JsonProperty("recipe_id") String recipeId,
            String name,
            String summary,
            @JsonProperty("main_ingredients") List<String> mainIngredients,
            @JsonProperty("cooking_time") Integer cookingTime,
            String reason
    ) {}

    /**
     * 응답 플래그 3종. 모두 필수.
     */
    public record Flags(
            @JsonProperty("needs_more_slots") boolean needsMoreSlots,
            @JsonProperty("out_of_scope") boolean outOfScope,
            @JsonProperty("is_fallback") boolean isFallback
    ) {}

    /**
     * POST /chat 요청 바디.
     *
     * Spring이 슬라이딩 윈도우(메시지 6개)로 history 전달.
     * slots.freeText는 Spring 누적. lastRecommendations는 직전 턴만.
     *
     * @param sessionId          세션 식별자 (1~100자)
     * @param turnId             턴 식별자 (1~100자)
     * @param message            이번 턴 사용자 발화 (1~500자)
     * @param history            최근 메시지 6개 (user 3 + assistant 3)
     * @param slots              현재 슬롯 스냅샷
     * @param lastRecommendations 직전 턴 추천 목록 (없으면 빈 리스트)
     */
    public record Request(
            @JsonProperty("session_id") String sessionId,
            @JsonProperty("turn_id") String turnId,
            String message,
            List<Message> history,
            Slots slots,
            @JsonProperty("last_recommendations") List<LastRecommendation> lastRecommendations
    ) {}

    /**
     * POST /chat 응답 바디.
     *
     * recommendations 길이는 0 또는 2.
     * slots_updated는 항상 전체 스냅샷.
     *
     * @param turnId          요청의 turnId와 동일
     * @param intent          최종 분류된 의도 (recommend|slot_fill|refine|ask)
     * @param answer          사용자에게 보여줄 응답 문자열 (필수, 빈 문자열 금지)
     * @param slotsUpdated    갱신 후 전체 슬롯 스냅샷
     * @param recommendations 추천 결과 (0개 또는 2개)
     * @param flags           응답 플래그 3종
     */
    public record Response(
            @JsonProperty("turn_id") String turnId,
            String intent,
            String answer,
            @JsonProperty("slots_updated") Slots slotsUpdated,
            List<Recommendation> recommendations,
            Flags flags
    ) {}
}