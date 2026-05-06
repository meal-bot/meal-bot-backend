package com.mealbot.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.mealbot.dto.AiDto;

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

    public AiDto.Response ask(String query) {
        return restClient.post()
                .uri(RECOMMEND_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AiDto.Request(query, recommendationLimit, recommendationMode))
                .retrieve()
                .body(AiDto.Response.class);
    }
}
