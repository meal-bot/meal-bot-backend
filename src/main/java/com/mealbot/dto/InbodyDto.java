package com.mealbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InbodyDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveRequest {
        // 기본 정보 (필수)
        private BigDecimal height;
        private BigDecimal weight;
        private Integer age;
        private String gender; // "남성" | "여성"
        private Double activityLevel; // 1.2 | 1.375 | 1.55 | 1.725 | 1.9

        // 상세 측정값 (선택, null 허용)
        private BigDecimal skeletalMuscle;
        private BigDecimal bodyFat;
        private BigDecimal bodyFatPercent;
        private BigDecimal protein;
        private BigDecimal mineral;
        private BigDecimal bodyWater;
        private Integer visceralFat;
    }

    @Getter
    @AllArgsConstructor
    public static class InbodyResponse {
        private Long id;
        private BigDecimal height;
        private BigDecimal weight;
        private Integer age;
        private String gender;
        private BigDecimal skeletalMuscle;
        private BigDecimal bodyFat;
        private BigDecimal bodyFatPercent;
        private BigDecimal bmi;
        private Integer bmr;
        private Integer dailyCalories;
        private BigDecimal protein;
        private BigDecimal mineral;
        private BigDecimal bodyWater;
        private Integer visceralFat;
        private LocalDateTime measuredAt;
    }
}