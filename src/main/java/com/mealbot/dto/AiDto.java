package com.mealbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class AiDto {

    @Getter
    @AllArgsConstructor
    public static class Request {
        private String query;
        @JsonProperty("top_k")
        private int topK;
        private String mode;
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String answer;
        private List<RecipeResult> results;
    }

    @Getter
    @NoArgsConstructor
    public static class RecipeResult {
        private int rank;
        @JsonProperty("recipe_id")
        private int recipeId;
        private String name;
        private String category;
        @JsonProperty("cooking_way")
        private String cookingWay;
        private double score;
        @JsonProperty("image_url")
        private String imageUrl;
        @JsonProperty("thumbnail_url")
        private String thumbnailUrl;
    }
}
