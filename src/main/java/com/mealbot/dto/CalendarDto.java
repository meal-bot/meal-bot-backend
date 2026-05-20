package com.mealbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

public class CalendarDto {

    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long chatId;
        private String title;
        private List<String> recommendations;
        private OffsetDateTime createdAt;
    }
}
