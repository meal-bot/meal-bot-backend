package com.mealbot.controller;

import com.mealbot.dto.FridgeDto;
import com.mealbot.entity.User;
import com.mealbot.service.FridgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Fridge", description = "냉장고 식재료 기반 메뉴 추천 API")
@RestController
@RequestMapping("/api/fridge")
@RequiredArgsConstructor
public class FridgeController {

    private final FridgeService fridgeService;

    /** POST /api/fridge/recommend — 식재료 리스트로 메뉴 추천 */
    @Operation(
            summary = "냉장고 식재료 기반 메뉴 추천",
            description = "프론트에서 선택한 ingredients와 직접 입력한 extras를 받아 count개의 메뉴를 추천합니다."
    )
    @ApiResponse(responseCode = "200", description = "추천 결과 리스트 반환")
    @ApiResponse(responseCode = "400", description = "검증 실패 (ingredients 누락/초과 등)")
    @PostMapping("/recommend")
    public ResponseEntity<FridgeDto.RecommendResponse> recommend(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FridgeDto.RecommendRequest request) {
        return ResponseEntity.ok(fridgeService.recommend(user, request));
    }
}