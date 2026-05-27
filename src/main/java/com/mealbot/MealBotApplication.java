package com.mealbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
// [codex] 비활성 만료시간이 지난 게스트 임시 채팅을 주기적으로 삭제하기 위해 스케줄링을 활성화한다.
@EnableScheduling
public class MealBotApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(MealBotApplication.class, args);
    }
}
