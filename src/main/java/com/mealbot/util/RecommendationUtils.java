package com.mealbot.util;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.mealbot.dto.AiDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RecommendationUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private RecommendationUtils() {}

    public static List<AiDto.Recommendation> deserialize(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<AiDto.Recommendation>>() {});
        } catch (tools.jackson.core.JacksonException e) {
            log.warn("recommendations 역직렬화 실패, 빈 리스트로 처리. json={}", json, e);
            return List.of();
        }
    }

    public static String serialize(List<AiDto.Recommendation> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (tools.jackson.core.JacksonException e) {
            throw new IllegalStateException("recommendations 직렬화 실패", e);
        }
    }
}
