package com.mealbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Python AI 서버 GET /recipes/{recipe_id} 응답 매핑 DTO.
 *
 * AiDto는 /chat 엔드포인트 전용으로 못 박혀있어 별도 파일로 분리.
 * 스키마는 AI 서버 레시피 상세 명세와 1:1 매핑.
 */
public class AiRecipeDto {

    private AiRecipeDto() {
        // 유틸 컨테이너 — 인스턴스화 금지
    }

    /**
     * GET /recipes/{recipe_id} 응답 바디.
     *
     * @param recipeId             정규화된 recipe id
     * @param name                 메뉴명
     * @param summary              메뉴 한 줄 요약
     * @param category             "반찬" / "국" 등
     * @param mainIngredients      주재료 태그
     * @param tasteTags            맛 태그 (담백한/고소한 등)
     * @param dishTypeTags         요리 분류 태그
     * @param cookingTime          조리 시간(분)
     * @param difficulty           "쉬움" / "보통" / "어려움"
     * @param spicyLevel           매운 정도 (1~4)
     * @param nutrition            영양 정보 5개 키
     * @param ingredientsStructured 식재료 구조 (main/sauce/garnish)
     * @param manuals              조리법 단계 (전체 빈 배열 가능)
     * @param imgMain              메인 이미지 URL. null 허용.
     * @param imgThumb             썸네일 URL. null 허용.
     */
    public record Response(
            @JsonProperty("recipe_id") String recipeId,
            String name,
            String summary,
            String category,
            @JsonProperty("main_ingredients") List<String> mainIngredients,
            @JsonProperty("taste_tags") List<String> tasteTags,
            @JsonProperty("dish_type_tags") List<String> dishTypeTags,
            @JsonProperty("cooking_time") Integer cookingTime,
            String difficulty,
            @JsonProperty("spicy_level") Integer spicyLevel,
            Nutrition nutrition,
            @JsonProperty("ingredients_structured") IngredientsStructured ingredientsStructured,
            List<Manual> manuals,
            @JsonProperty("img_main") String imgMain,
            @JsonProperty("img_thumb") String imgThumb
    ) {}

    /**
     * 영양 정보. 5개 키 모두 명세상 전체 데이터에서 채워져 있어 nullable 처리 불필요하지만,
     * 안전망으로 wrapper 타입(Double) 사용.
     */
    public record Nutrition(
            @JsonProperty("energy_kcal") Double energyKcal,
            @JsonProperty("protein_g") Double proteinG,
            @JsonProperty("carbs_g") Double carbsG,
            @JsonProperty("fat_g") Double fatG,
            @JsonProperty("sodium_mg") Double sodiumMg
    ) {}

    /**
     * 식재료 구조. sauce/garnish는 빈 배열 가능.
     */
    public record IngredientsStructured(
            List<RecipeIngredient> main,
            List<RecipeIngredient> sauce,
            List<RecipeIngredient> garnish
    ) {}

    /**
     * 식재료 단건.
     *
     * @param name   재료명
     * @param amount 분량 (예: "75g")
     * @param note   부가 설명 (예: "3/4모"). null 허용.
     */
    public record RecipeIngredient(
            String name,
            String amount,
            String note
    ) {}

    /**
     * 조리법 단계 단건.
     *
     * @param step 단계 번호 (1부터 시작, 항상 존재)
     * @param desc 단계 설명 (앞에 "1. " 같은 번호가 이미 포함될 수 있음 — 원본 데이터 특성)
     * @param img  단계별 이미지 URL. null 허용.
     */
    public record Manual(
            int step,
            String desc,
            String img
    ) {}
}