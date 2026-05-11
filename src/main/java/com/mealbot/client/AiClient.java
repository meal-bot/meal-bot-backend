package com.mealbot.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.mealbot.dto.AiDto;
import java.util.List;

@Component
public class AiClient {

    private static final String RECOMMEND_PATH = "/recommend";

    private final RestClient restClient;
    private final int recommendationLimit;
    private final String recommendationMode;

    public AiClient(
            @Value("${ai.server.url}") String aiServerUrl,
            @Value("${ai.recommendation.limit}") int recommendationLimit,
            @Value("${ai.recommendation.mode}") String recommendationMode) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);

        this.restClient = RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(factory)
                .build();
        this.recommendationLimit = recommendationLimit;
        this.recommendationMode = recommendationMode;
    }

    /**
     * Python AI 서버에 레시피 추천 요청을 전송하고 응답을 반환한다.
     *
     * @param query    현재 사용자 메시지 (RAG 검색 키워드로 사용)
     * @param messages 최근 대화 히스토리 (Python이 대화 맥락 유지에 활용)
     */
    public AiDto.Response ask(String query, List<AiDto.MessageDto> messages) {
        return restClient.post()
                .uri(RECOMMEND_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AiDto.Request(query, recommendationLimit, recommendationMode, messages))
                .retrieve()
                .body(AiDto.Response.class);
    }
}
