package com.mealbot.service;

import com.mealbot.client.AiClient;
import com.mealbot.dto.AiDto;
import com.mealbot.dto.FridgeDto;
import com.mealbot.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 냉장고 기반 메뉴 추천 서비스.
 *
 * <h3>현재 구현: 방안 A (기존 /chat 재사용)</h3>
 * 별도 파이썬 엔드포인트를 신설하지 않고, 채팅용 /chat API를 호출한다.
 * 파트너 협의가 끝나면 FridgeService 내부만 새 엔드포인트 호출로 교체.
 * 컨트롤러/DTO/프론트는 손대지 않는 것이 이 클래스의 교체 지점 역할이다.
 *
 * <h3>slot 프리필 전략</h3>
 * 파이썬 _is_slots_sufficient 게이트가 meal_times + purpose 둘 다 요구하므로
 *  - meal_times: 5개 전부 → 시간대 제약을 의미상 무력화
 *  - purpose: "tasty" → 4가지 중 의미 방향성이 가장 약함
 *  - free_text: 재료 공백 결합 → BM25/Dense 검색의 실제 신호
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FridgeService {

    private static final List<String> ALL_MEAL_TIMES =
            List.of("아침", "점심", "저녁", "간식", "야식");

    private static final String DEFAULT_PURPOSE = "tasty";

    private static final String FREE_TEXT_DELIMITER = " ";

    /** 추천이 0건이거나 정상 recommend 흐름이 아닐 때 사용자에게 보여줄 안내 문구. */
    private static final String FALLBACK_MESSAGE =
            "보내주신 재료로 적합한 메뉴를 찾지 못했어요. 재료를 추가하거나 다른 조합으로 시도해보세요.";

    /** 파이썬 ChatOrchestrator가 분류 가능한 intent. 이 집합 외 값이 오면 schema drift 의심. */
    private static final Set<String> ALLOWED_INTENTS =
            Set.of("recommend", "refine", "ask", "slot_fill", "out_of_scope");

    private final AiClient aiClient;

    public FridgeDto.RecommendResponse recommend(User user, FridgeDto.RecommendRequest request) {
        List<String> ingredients = safeList(request.getIngredients());
        List<String> extras = safeList(request.getExtras());

        log.info("Fridge 추천 요청: userId={} ingredients={} extras={} count={}",
                user.getId(), ingredients, extras, request.getCount());

        AiDto.Request aiRequest = buildAiRequest(user, ingredients, extras);
        AiDto.Response aiResponse = aiClient.chat(aiRequest);

        validateResponseSchema(aiRequest, aiResponse);

        List<FridgeDto.Recommendation> recommendations =
                mapRecommendations(aiResponse.recommendations());

        String message = buildMessage(aiResponse);

        return new FridgeDto.RecommendResponse(message, recommendations);
    }

    /**
     * intent별 사용자 노출 문구 결정.
     *
     * 정상 추천(recommend + recs 2개 + is_fallback=false) → AI answer 그대로 노출.
     * 그 외(slot_fill 폴백, refine/ask/out_of_scope 이상 케이스) → 냉장고 페이지용 자체 문구로 교체.
     *
     * 이유: 파이썬 answer는 채팅 페이지 UX 기준으로 작성됨("조건을 풀어볼까요?" 등).
     * 냉장고 화면에서 표시되면 위화감이라 Spring이 메시지를 다시 짠다.
     */
    private String buildMessage(AiDto.Response response) {
        boolean isNormalRecommend = "recommend".equals(response.intent())
                && response.recommendations() != null
                && response.recommendations().size() == 2
                && response.flags() != null
                && !response.flags().isFallback();

        if (isNormalRecommend) {
            return response.answer();
        }
        return FALLBACK_MESSAGE;
    }

    private AiDto.Request buildAiRequest(User user, List<String> ingredients, List<String> extras) {
        List<String> allIngredients = new ArrayList<>(ingredients);
        allIngredients.addAll(extras);

        String freeText = String.join(FREE_TEXT_DELIMITER, allIngredients);
        String message = "냉장고에 " + String.join(", ", ingredients)
                + "이(가) 있어. 이걸로 만들 수 있는 메뉴를 추천해줘.";

        AiDto.Slots slots = new AiDto.Slots(ALL_MEAL_TIMES, DEFAULT_PURPOSE, freeText);

        String sessionId = "fridge-" + user.getId() + "-" + System.currentTimeMillis();
        String turnId = UUID.randomUUID().toString();

        return new AiDto.Request(
                sessionId,
                turnId,
                message,
                List.of(),
                slots,
                List.of()
        );
    }

    private List<FridgeDto.Recommendation> mapRecommendations(List<AiDto.Recommendation> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream()
                .map(r -> new FridgeDto.Recommendation(
                        r.recipeId(),
                        r.name(),
                        r.cookingTime(),
                        r.summary(),
                        r.mainIngredients(),
                        r.reason()
                ))
                .toList();
    }

    private static List<String> safeList(List<String> list) {
        return list == null ? List.of() : list;
    }

    /**
     * AI 응답이 예상 스키마를 따르는지 검사. 위반은 예외가 아닌 WARN 로그로만 처리한다.
     *
     * 예외를 던지지 않는 이유: 파이썬 응답 일부 필드가 예상과 달라도 사용자 응답을 막기보다는
     * 가능한 한 graceful하게 fallback 처리하는 것이 UX에 안전. 회귀는 로그로 조기 감지.
     */
    private void validateResponseSchema(AiDto.Request request, AiDto.Response response) {
        List<AiDto.Recommendation> recs = response.recommendations();
        if (recs != null && recs.size() != 0 && recs.size() != 2) {
            log.warn("Fridge schema drift: recommendations size={} (expected 0 or 2) turnId={}",
                    recs.size(), response.turnId());
        }

        if (response.intent() == null || !ALLOWED_INTENTS.contains(response.intent())) {
            log.warn("Fridge schema drift: unknown intent={} turnId={}",
                    response.intent(), response.turnId());
        }

        if (!Objects.equals(request.turnId(), response.turnId())) {
            log.warn("Fridge turnId mismatch: requestTurnId={} responseTurnId={}",
                    request.turnId(), response.turnId());
        }

        if (response.answer() == null || response.answer().isBlank()) {
            log.warn("Fridge empty answer in AI response: turnId={}", response.turnId());
        }

        if (response.flags() == null) {
            log.warn("Fridge null flags in AI response: turnId={}", response.turnId());
        }
    }
}