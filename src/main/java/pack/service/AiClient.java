package pack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pack.dto.AiDto;

@Component
public class AiClient {

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

    public AiDto.Response ask(String query) {
        return restClient.post()
                .uri("/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AiDto.Request(query, 5, "v4-lite"))
                .retrieve()
                .body(AiDto.Response.class);
    }
}