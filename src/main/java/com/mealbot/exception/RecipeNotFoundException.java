package com.mealbot.exception;

import lombok.Getter;

/**
 * AI 서버가 GET /recipes/{id}에 대해 404를 반환했을 때 던지는 예외.
 * RecipeController의 @ExceptionHandler가 HTTP 404 응답으로 변환한다.
 */
@Getter
public class RecipeNotFoundException extends RuntimeException {

    private final String recipeId;

    public RecipeNotFoundException(String recipeId) {
        super("recipe not found: id=" + recipeId);
        this.recipeId = recipeId;
    }
}