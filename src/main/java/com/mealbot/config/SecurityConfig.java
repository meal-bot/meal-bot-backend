package com.mealbot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.mealbot.security.JwtAuthFilter;
import com.mealbot.security.OAuth2SuccessHandler;
import com.mealbot.service.CustomOAuth2UserService;

import java.util.List;

/**
 * [설정] 스프링 시큐리티의 핵심 보안 설정 클래스.
 *
 * 이 클래스에서 설정하는 내용:
 *   1. CORS 정책 : 어떤 출처(Origin)의 요청을 허용할지
 *   2. CSRF 비활성화 : REST API는 세션 쿠키를 사용하지 않으므로 CSRF 불필요
 *   3. 세션 정책 : OAuth2 로그인 흐름을 위해 조건부 세션 허용
 *   4. 인가 규칙 : 어떤 URL은 인증 없이 허용, 나머지는 인증 필수
 *   5. OAuth2 로그인 설정 : Google 로그인 처리 서비스 및 성공 핸들러 연결
 *   6. JWT 필터 등록 : 매 요청마다 JWT를 검사하는 필터를 필터 체인에 추가
 */
@Configuration      // 스프링 설정 클래스임을 선언
@EnableWebSecurity  // 스프링 시큐리티 웹 보안 기능 활성화
@RequiredArgsConstructor  // Lombok: final 필드를 파라미터로 받는 생성자 자동 생성 (의존성 주입)
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.frontend-url-local:http://localhost:5173}")
    private String frontendUrlLocal;

    /**
     * 스프링 시큐리티 필터 체인을 구성하는 핵심 메서드.
     * 모든 HTTP 요청은 이 설정에 정의된 규칙을 따름.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── CORS 설정 ──────────────────────────────────────────────────────────
            // 아래 corsConfigurationSource() 빈에서 정의한 CORS 규칙을 적용
            // (CorsConfig.java는 현재 사용하지 않으며 이 설정이 CORS를 담당)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── CSRF 비활성화 ───────────────────────────────────────────────────────
            // CSRF는 세션+쿠키 기반 인증의 취약점 방어 기법.
            // 이 앱은 JWT를 Authorization 헤더로 전달하므로 CSRF 공격 대상이 아님 → 비활성화
            .csrf(csrf -> csrf.disable())

            // ── 세션 정책 ───────────────────────────────────────────────────────────
            // STATELESS로 설정하면 세션을 전혀 사용하지 않지만,
            // OAuth2 로그인 흐름은 내부적으로 세션이 필요함 (state 파라미터 저장 등).
            // IF_REQUIRED: 필요할 때만 세션 생성 (OAuth2 흐름에서만 사용, JWT API는 세션 없음)
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            // ── 인가(Authorization) 규칙 ────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                    // 아래 경로는 로그인 없이도 누구나 접근 가능
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
//                            "/api/signup",
//                            "/api/login",
                            "/api/chat/guest/sendMessage",
                            "/oauth2/**",
                            "/login/oauth2/**"
                    ).permitAll()
                    // 위에서 허용한 경로 외 나머지 모든 요청은 인증 필수
                    .anyRequest().authenticated()
            )

            // ── OAuth2 로그인 설정 ──────────────────────────────────────────────────
            .oauth2Login(oauth2 -> oauth2
                    // Google 로그인 완료 후 사용자 정보를 가져올 때 사용할 서비스 지정
                    // CustomOAuth2UserService가 Google API에서 받은 정보를 DB에 저장함
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService))
                    // 로그인 성공 시 호출할 핸들러: JWT 생성 후 프론트엔드로 리다이렉트
                    .successHandler(oAuth2SuccessHandler)
            )

            // ── JWT 필터 등록 ────────────────────────────────────────────────────────
            // JwtAuthFilter를 OAuth2LoginAuthenticationFilter 바로 뒤에 삽입.
            // 매 요청마다 Authorization 헤더의 JWT 토큰을 검사해 사용자를 인증함.
            // OAuth2 필터 뒤에 놓는 이유: OAuth2 로그인 흐름이 완전히 처리된 후 JWT 필터가 동작해야 함
            .addFilterAfter(jwtAuthFilter, OAuth2LoginAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 설정 빈.
     *
     * CORS: 브라우저가 다른 출처(Origin)의 API를 호출할 때 적용되는 보안 정책.
     * 프론트엔드(React, localhost:5173)와 백엔드(Spring, localhost:8080)가 다른 포트이므로
     * CORS 설정이 필요함.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(frontendUrl, frontendUrlLocal));

        // 허용할 HTTP 메서드
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 허용할 요청 헤더: "*"는 모든 헤더 허용 (Authorization 헤더 포함)
        config.setAllowedHeaders(List.of("*"));

        // 쿠키/인증정보(credentials) 포함 요청 허용
        // → OAuth2 로그인 흐름에서 세션 쿠키를 주고받기 위해 필요
        config.setAllowCredentials(true);

        // 위 설정을 모든 경로("/**")에 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
