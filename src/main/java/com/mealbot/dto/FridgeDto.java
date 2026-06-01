package com.mealbot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 냉장고 기반 메뉴 추천 DTO.
 *
 * 채팅 DTO와 분리한 이유:
 * - 입력 형태가 다름 (자연어 X, 식재료 리스트 O)
 * - 응답 형태는 채팅과 유사하지만, 향후 냉장고 전용 필드가 붙을 수 있어 독립 유지
 *
 * A안(채팅 API 재사용) / B안(Direct LLM) 결정 보류 상태.
 * Controller~Service 인터페이스(API 계약)는 양쪽 모두에 호환되도록 정의.
 */
public class FridgeDto {

    private FridgeDto() {
        // 유틸 컨테이너 — 인스턴스화 금지
    }

    /**
     * POST /api/fridge/recommend 요청 바디.
     *
     * @param ingredients 프론트 칩으로 선택한 식재료 (1~30개)
     * @param extras      사용자가 직접 입력한 식재료 (0~10개, null 허용)
     * @param count       원하는 추천 개수 (현재 2 고정)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendRequest {

        @NotNull(message = "ingredients는 필수입니다.")
        @Size(min = 1, max = 30, message = "ingredients는 1~30개여야 합니다.")
        private List<String> ingredients;

        @Size(max = 10, message = "extras는 최대 10개까지 허용됩니다.")
        private List<String> extras;

        @NotNull(message = "count는 필수입니다.")
        @Min(value = 1, message = "count는 1 이상이어야 합니다.")
        @Max(value = 5, message = "count는 5 이하여야 합니다.")
        private Integer count;
    }

    /**
     * 응답 단건. ChatDto.Recommendation과 형태는 같지만,
     * 향후 냉장고 전용 필드(usedIngredients 등) 추가 가능성 때문에 분리.
     *
     * @param recipeId        레시피 식별자
     * @param name            메뉴명
     * @param cookingTime     조리 시간 (분)
     * @param summary         메뉴 요약
     * @param mainIngredients 주재료
     * @param reason          추천 이유
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
     * POST /api/fridge/recommend 응답 바디.
     *
     * @param message         사용자에게 노출할 안내 문구. intent별로 Spring이 결정.
     *                        recommend 정상 시 AI answer 그대로, 그 외(폴백/이상 케이스)는
     *                        냉장고 페이지 맥락에 맞는 자체 문구로 교체된 값.
     * @param recommendations 추천 결과 리스트. 0개(빈 상태) 또는 2개.
     */
    public record RecommendResponse(
            String message,
            List<Recommendation> recommendations
    ) {}
}