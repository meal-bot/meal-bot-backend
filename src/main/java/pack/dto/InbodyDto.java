package pack.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

        // 상세 측정값 (선택, null 허용)
        private BigDecimal skeletalMuscle;
        private BigDecimal bodyFat;
        private BigDecimal bodyFatPercent;
        private BigDecimal bmi;
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
        private BigDecimal protein;
        private BigDecimal mineral;
        private BigDecimal bodyWater;
        private Integer visceralFat;
        private LocalDateTime measuredAt;
    }

    @Getter
    @AllArgsConstructor
    public static class InbodyListResponse {
        private List<InbodyResponse> records;
    }
}