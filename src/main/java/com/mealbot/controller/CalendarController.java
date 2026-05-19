package com.mealbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mealbot.dto.CalendarDto;
import com.mealbot.entity.User;
import com.mealbot.service.CalendarService;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Calendar", description = "식단 캘린더 관련 API")
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    /** GET /api/calendar/{date} — 1. 날짜별 추천 식단 목록 조회 */
    @Operation(summary = "날짜별 추천 식단 조회", description = "특정 날짜에 AI로부터 추천받은 식단 목록을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "추천 식단 목록 반환 (해당 날짜 추천 없으면 빈 배열)")
    @GetMapping("/{date}")
    public ResponseEntity<List<CalendarDto.Response>> getCalendar(
            @AuthenticationPrincipal User user,
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(calendarService.getCalendar(user, date));
    }
}
