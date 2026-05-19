package com.mealbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

public class CalendarDto {

    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long chatId;
        private String title;
        private String lastRecommendation;
        private LocalDateTime createdAt;
    }
}
