package com.mealbot.client;

import com.mealbot.dto.AiDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Python AI 서버 v0.3 /chat 엔드포인트 호출 클라이언트.
 *
 * v0.2 /recommend는 폐기. 모든 대화 흐름(추천, 슬롯 채우기, refine, 정보 질문)은 /chat 단일 엔드포인트로 통합.
 * 흐름 제어와 상태 관리는 AI 서버의 ChatOrchestrator가 담당.
 */
@Component
public class AiClient {

    private static final String CHAT_PATH = "/chat";

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
}