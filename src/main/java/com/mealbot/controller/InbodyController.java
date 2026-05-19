package com.mealbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mealbot.dto.InbodyDto;
import com.mealbot.entity.User;
import com.mealbot.service.InbodyService;

import java.util.List;
import java.util.Map;

@Tag(name = "Inbody", description = "인바디 데이터 관련 API")
@RestController
@RequestMapping("/api/inbody")
@RequiredArgsConstructor
public class InbodyController {

    private final InbodyService inbodyService;

    /** POST /api/inbody — 1. 인바디 데이터 저장 */
    @Operation(summary = "인바디 데이터 저장", description = "사용자의 인바디 측정 데이터를 저장합니다.")
    @ApiResponse(responseCode = "200", description = "저장된 인바디 데이터 반환")
    @PostMapping
    public ResponseEntity<InbodyDto.InbodyResponse> save(
            @AuthenticationPrincipal User user,
            @RequestBody InbodyDto.SaveRequest request) {
        return ResponseEntity.ok(inbodyService.save(user, request));
    }

    /** GET /api/inbody — 2. 내 인바디 기록 목록 조회 (최신순) */
    @Operation(summary = "인바디 기록 목록 조회", description = "사용자의 인바디 기록을 최신순으로 반환합니다.")
    @ApiResponse(responseCode = "200", description = "인바디 기록 목록 반환")
    @GetMapping
    public ResponseEntity<List<InbodyDto.InbodyResponse>> getList(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(inbodyService.getList(user));
    }

    /** DELETE /api/inbody/{inbodyId} — 3. 인바디 기록 삭제 */
    @Operation(summary = "인바디 기록 삭제", description = "특정 인바디 기록을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "삭제 성공 여부 반환")
    @DeleteMapping("/{inbodyId}")
    public ResponseEntity<Map<String, Boolean>> delete(
            @AuthenticationPrincipal User user,
            @Parameter(description = "인바디 기록 ID") @PathVariable Long inbodyId) {
        inbodyService.delete(user, inbodyId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
