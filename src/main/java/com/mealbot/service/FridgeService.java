package com.mealbot.service;

import com.mealbot.dto.FridgeDto;
import com.mealbot.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 냉장고 기반 메뉴 추천 서비스.
 *
 * <h3>현재 상태: 스텁(stub)</h3>
 * A안(채팅 API 재사용) vs B안(Direct LLM) 결정이 보류 상태이므로,
 * 실제 AI 호출 없이 placeholder 응답을 반환한다.
 *
 * <h3>교체 시점</h3>
 * A/B안 결정 후, recommend() 내부의 placeholder 생성 부분만 교체하면 된다.
 * Controller/DTO 계약은 그대로 유지된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FridgeService {

    /**
     * 식재료 리스트로 메뉴 추천.
     *
     * @param user    인증된 사용자 (게스트 허용 여부는 추후 결정)
     * @param request ingredients/extras/count
     * @return count 길이의 추천 리스트
     */
    public FridgeDto.RecommendResponse recommend(User user, FridgeDto.RecommendRequest request) {
        log.info("Fridge 추천 요청: userId={} ingredients={} extras={} count={}",
                user != null ? user.getId() : "guest",
                request.getIngredients(),
                request.getExtras(),
                request.getCount());

        // [스텁] A안/B안 결정 후 실제 AI 호출 결과로 교체될 자리.
        // 프론트 통합 테스트용 placeholder 2건 고정 반환.
        List<String> ingredients = request.getIngredients();
        String firstIngredient = ingredients.isEmpty() ? "재료" : ingredients.getFirst();

        FridgeDto.Recommendation r1 = new FridgeDto.Recommendation(
                "stub-1",
                firstIngredient + " 볶음밥",
                15,
                "보내주신 재료로 빠르게 만드는 한 그릇 메뉴",
                ingredients.stream().limit(3).toList(),
                "사용자가 보낸 식재료 중 " + firstIngredient + "을(를) 메인으로 활용"
        );

        FridgeDto.Recommendation r2 = new FridgeDto.Recommendation(
                "stub-2",
                firstIngredient + " 찌개",
                25,
                "국물 요리로 보내주신 재료를 폭넓게 활용",
                ingredients.stream().limit(3).toList(),
                "조리 시간이 조금 더 걸리지만 푸짐한 한 끼"
        );

        List<FridgeDto.Recommendation> placeholders = List.of(r1, r2);

        return new FridgeDto.RecommendResponse(placeholders);
    }
}