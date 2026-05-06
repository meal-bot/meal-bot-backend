package com.mealbot.util;

import com.mealbot.entity.Inbody;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InbodyCalculator {

    public static BigDecimal calcBmi(BigDecimal height, BigDecimal weight) {
        BigDecimal heightM = height.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weight.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }

    public static int calcBmr(BigDecimal height, BigDecimal weight, int age, Inbody.Gender gender) {
        double base = 10 * weight.doubleValue() + 6.25 * height.doubleValue() - 5.0 * age;
        return (int) Math.round(gender == Inbody.Gender.MALE ? base + 5 : base - 161);
    }

    public static int calcDailyCalories(int bmr, double activityLevel) {
        return (int) Math.round(bmr * activityLevel);
    }
}