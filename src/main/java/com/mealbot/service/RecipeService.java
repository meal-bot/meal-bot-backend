package com.mealbot.service;

import com.mealbot.client.AiClient;
import com.mealbot.dto.AiRecipeDto;
import com.mealbot.dto.RecipeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 레시피 상세 조회 서비스.
 *
 * 흐름: 컨트롤러로부터 recipeId를 받아 AI 서버에 위임하고,
 * AiRecipeDto(snake_case)를 클라이언트 노출용 RecipeDto(camelCase)로 매핑하여 반환한다.
 *
 * DB 저장/캐싱 없음 — 단순 pass-through 프록시.
 * 트래픽/지연이 문제되면 Caffeine 캐시 도입 검토.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final AiClient aiClient;

    /**
     * 레시피 상세를 조회한다.
     *
     * @param recipeId AI 추천 응답의 recipe_id 그대로
     * @return 클라이언트 노출용 레시피 상세
     */
    public RecipeDto.Response getRecipe(String recipeId) {
        log.info("recipe 상세 조회 요청: recipeId={}", recipeId);
        AiRecipeDto.Response ai = aiClient.getRecipe(recipeId);
        return toResponseDto(ai);
    }

    private static RecipeDto.Response toResponseDto(AiRecipeDto.Response a) {
        return new RecipeDto.Response(
                a.recipeId(),
                a.name(),
                a.summary(),
                a.category(),
                a.mainIngredients(),
                a.tasteTags(),
                a.dishTypeTags(),
                a.cookingTime(),
                a.difficulty(),
                a.spicyLevel(),
                toNutrition(a.nutrition()),
                toIngredientsStructured(a.ingredientsStructured()),
                a.manuals().stream().map(RecipeService::toManual).toList(),
                normalizeImageUrl(a.imgMain()),
                normalizeImageUrl(a.imgThumb())
        );
    }

    private static RecipeDto.Nutrition toNutrition(AiRecipeDto.Nutrition n) {
        return new RecipeDto.Nutrition(
                n.energyKcal(),
                n.proteinG(),
                n.carbsG(),
                n.fatG(),
                n.sodiumMg()
        );
    }

    private static RecipeDto.IngredientsStructured toIngredientsStructured(AiRecipeDto.IngredientsStructured s) {
        return new RecipeDto.IngredientsStructured(
                s.main().stream().map(RecipeService::toIngredient).toList(),
                s.sauce().stream().map(RecipeService::toIngredient).toList(),
                s.garnish().stream().map(RecipeService::toIngredient).toList()
        );
    }

    private static RecipeDto.Ingredient toIngredient(AiRecipeDto.RecipeIngredient i) {
        return new RecipeDto.Ingredient(i.name(), i.amount(), i.note());
    }

    private static RecipeDto.Manual toManual(AiRecipeDto.Manual m) {
        return new RecipeDto.Manual(m.step(), m.desc(), normalizeImageUrl(m.img()));
    }

    private static String normalizeImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }

        return url.replaceFirst("^http://", "https://");
    }
}
