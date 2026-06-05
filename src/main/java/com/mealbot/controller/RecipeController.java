package com.mealbot.controller;

import com.mealbot.dto.ChatDto;
import com.mealbot.dto.RecipeDto;
import com.mealbot.exception.RecipeNotFoundException;
import com.mealbot.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * 레시피 상세 조회 API.
 *
 * AI 서버 GET /recipes/{id}에 대한 단순 프록시.
 * 게스트/로그인 사용자 모두 허용 (SecurityConfig에서 permitAll 처리).
 *
 * 예외 처리는 임시로 컨트롤러 내부 @ExceptionHandler에 둠 — 추후 전역(@ControllerAdvice) 통합 예정.
 */
@Slf4j
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private static final String ERROR_CODE_NOT_FOUND = "RECIPE_NOT_FOUND";
    private static final String ERROR_CODE_AI_SERVER = "AI_SERVER_ERROR";
    private static final String ERROR_MESSAGE_AI_SERVER = "AI 서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.";

    private final RecipeService recipeService;

    @GetMapping("/random")
    public ResponseEntity<List<RecipeDto.Response>> getRandomRecipes(
            @RequestParam(defaultValue = "10") int count
    ) {
        return ResponseEntity.ok(recipeService.getRandomRecipes(count));
    }

    @GetMapping("/{recipeId}")
    public ResponseEntity<RecipeDto.Response> getRecipe(@PathVariable String recipeId) {
        return ResponseEntity.ok(recipeService.getRecipe(recipeId));
    }

    @ExceptionHandler(RecipeNotFoundException.class)
    public ResponseEntity<ChatDto.ErrorResponse> handleNotFound(RecipeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ChatDto.ErrorResponse(ERROR_CODE_NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ChatDto.ErrorResponse> handleAiServerError(RestClientException e) {
        log.warn("AI 서버 호출 실패 (recipe 상세): {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ChatDto.ErrorResponse(ERROR_CODE_AI_SERVER, ERROR_MESSAGE_AI_SERVER));
    }
}
