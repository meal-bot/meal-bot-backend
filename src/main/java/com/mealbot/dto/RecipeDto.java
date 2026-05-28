package com.mealbot.dto;

import java.util.List;

/**
 * 클라이언트(웹/앱) 노출용 레시피 상세 DTO.
 *
 * AI 통신용 DTO(AiRecipeDto)와 분리. 레이어 책임:
 * - AiRecipeDto: Spring ↔ FastAPI 내부 통신 (snake_case)
 * - RecipeDto:   Spring ↔ 클라이언트 노출 (camelCase)
 *
 * AI 서버 스키마 변경 시 RecipeService 매핑만 수정하면 프론트 영향 없음.
 */
public class RecipeDto {

    private RecipeDto() {
        // 유틸 컨테이너 — 인스턴스화 금지
    }

    /**
     * GET /api/recipes/{recipeId} 응답 바디.
     *
     * @param recipeId             정규화된 recipe id
     * @param name                 메뉴명
     * @param summary              메뉴 한 줄 요약
     * @param category             "반찬" / "국" 등
     * @param mainIngredients      주재료 태그
     * @param tasteTags            맛 태그
     * @param dishTypeTags         요리 분류 태그
     * @param cookingTime          조리 시간(분)
     * @param difficulty           "쉬움" / "보통" / "어려움"
     * @param spicyLevel           매운 정도 (1~4)
     * @param nutrition            영양 정보
     * @param ingredientsStructured 식재료 구조 (main/sauce/garnish)
     * @param manuals              조리법 단계
     * @param imgMain              메인 이미지 URL. null 가능.
     * @param imgThumb             썸네일 URL. null 가능.
     */
    public record Response(
            String recipeId,
            String name,
            String summary,
            String category,
            List<String> mainIngredients,
            List<String> tasteTags,
            List<String> dishTypeTags,
            Integer cookingTime,
            String difficulty,
            Integer spicyLevel,
            Nutrition nutrition,
            IngredientsStructured ingredientsStructured,
            List<Manual> manuals,
            String imgMain,
            String imgThumb
    ) {}

    public record Nutrition(
            Double energyKcal,
            Double proteinG,
            Double carbsG,
            Double fatG,
            Double sodiumMg
    ) {}

    public record IngredientsStructured(
            List<Ingredient> main,
            List<Ingredient> sauce,
            List<Ingredient> garnish
    ) {}

    /**
     * @param name   재료명
     * @param amount 분량 (예: "75g")
     * @param note   부가 설명. null 가능.
     */
    public record Ingredient(
            String name,
            String amount,
            String note
    ) {}

    /**
     * @param step 단계 번호 (1부터 시작)
     * @param desc 단계 설명
     * @param img  단계별 이미지 URL. null 가능.
     */
    public record Manual(
            int step,
            String desc,
            String img
    ) {}
}