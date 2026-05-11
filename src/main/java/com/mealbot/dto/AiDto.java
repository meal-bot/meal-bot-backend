package com.mealbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class AiDto {

    /** 대화 히스토리 단건 메시지 (role: "user" | "assistant") */
    @Getter
    @AllArgsConstructor
    public static class MessageDto {
        private String role;
        private String content;
    }

    /** Python AI 서버 /recommend 요청 객체 */
    @Getter
    @AllArgsConstructor
    public static class Request {
        private String query;              // 현재 사용자 메시지 (RAG 검색 키워드)
        @JsonProperty("top_k")
        private int topK;                  // 반환할 추천 레시피 최대 개수
        private String mode;               // Python 내부 추천 알고리즘 버전
        private List<MessageDto> messages; // 최근 대화 히스토리 (최대 10개)
    }

    /** Python AI 서버 /recommend 응답 객체 */
    @Getter
    @NoArgsConstructor
    public static class Response {
        private String answer;              // AI 생성 답변 텍스트
        private List<RecipeResult> results; // 추천 레시피 목록
    }

    /** 추천 레시피 단건 */
    @Getter
    @NoArgsConstructor
    public static class RecipeResult {
        private int rank;
        @JsonProperty("recipe_id")
        private int recipeId;
        private String name;
        private String category;
        @JsonProperty("cooking_way")
        private String cookingWay;
        private double score;
        @JsonProperty("image_url")
        private String imageUrl;
        @JsonProperty("thumbnail_url")
        private String thumbnailUrl;
    }
}
