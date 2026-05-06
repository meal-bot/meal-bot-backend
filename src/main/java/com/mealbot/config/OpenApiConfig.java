package com.mealbot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String JWT_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme jwtScheme = new SecurityScheme()
                .name(JWT_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("MealBot API")
                        .description("MealBot backend API documentation")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(JWT_SCHEME_NAME, jwtScheme))
                .addSecurityItem(new SecurityRequirement()
                        .addList(JWT_SCHEME_NAME));
    }
}
