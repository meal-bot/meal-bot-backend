package com.mealbot.client;

import com.mealbot.dto.AiDto;
import com.mealbot.dto.AiRecipeDto;
import com.mealbot.exception.RecipeNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Python AI 서버 호출 클라이언트.
 *
 * 지원 엔드포인트:
 * - POST /chat            : 대화/추천 (v0.3 단일 엔드포인트로 통합, /recommend는 폐기)
 * - GET  /recipes/{id}    : 레시피 상세 조회 (모달용)
 *
 * 흐름 제어와 상태 관리는 AI 서버의 ChatOrchestrator가 담당.
 */
@Component
public class AiClient {

    private static final String CHAT_PATH = "/chat";
    private static final String RECIPES_PATH = "/recipes/{recipeId}";
    private static final String RANDOM_RECIPES_PATH = "/recipes/random";

    private final RestClient restClient;

    public AiClient(@Value("${ai.server.url}") String aiServerUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);

        this.restClient = RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * Python AI 서버에 chat 요청을 전송하고 응답을 반환한다.
     *
     * @param request v0.3 ChatRequest (session_id, turn_id, message, history, slots, last_recommendations)
     * @return v0.3 ChatResponse (turn_id, intent, answer, slots_updated, recommendations, flags)
     */
    public AiDto.Response chat(AiDto.Request request) {
        return restClient.post()
                .uri(CHAT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiDto.Response.class);
    }

    /**
     * AI 서버에서 레시피 상세를 조회한다.
     *
     * 404는 도메인 예외(RecipeNotFoundException)로 변환하여 상위 레이어가 HTTP 라이브러리에
     * 의존하지 않도록 한다. 5xx/타임아웃은 그대로 propagate — 컨트롤러에서 502로 처리.
     *
     * @param recipeId 조회할 레시피 id (AI 추천 응답의 recipe_id 그대로 전달, "28"/"recipe_28" 모두 허용)
     * @return 레시피 상세 응답
     * @throws RecipeNotFoundException recipe_id가 AI 서버에 존재하지 않을 때
     */
    public AiRecipeDto.Response getRecipe(String recipeId) {
        try {
            return restClient.get()
                    .uri(RECIPES_PATH, recipeId)
                    .retrieve()
                    .body(AiRecipeDto.Response.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new RecipeNotFoundException(recipeId);
        }
    }

    public List<AiRecipeDto.Response> getRandomRecipes(int count) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(RANDOM_RECIPES_PATH)
                        .queryParam("count", count)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
