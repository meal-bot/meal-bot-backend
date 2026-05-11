package com.mealbot.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import com.mealbot.entity.User;
import com.mealbot.repository.UserRepository;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final String frontendUrl;

    public OAuth2SuccessHandler(JwtUtil jwtUtil,
                                UserRepository userRepository,
                                @Value("${app.frontend-url}") String frontendUrl) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
    }

    /**
     * OAuth2 인증 성공 시 스프링 시큐리티가 자동으로 호출하는 메서드.
     *
     * @param request        HTTP 요청
     * @param response       HTTP 응답 (리다이렉트에 사용)
     * @param authentication 인증 완료된 사용자 정보 (CustomOAuth2UserService에서 반환한 OAuth2User 포함)
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // 1. 인증 주체(principal)를 OAuth2User 타입으로 꺼냄
        //    → CustomOAuth2UserService.loadUser()에서 반환한 소셜 사용자 정보 객체
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 2. 제공자(Google/카카오)를 구분해 이메일 추출
        //    - Google : attribute에 "email" 키가 바로 존재
        //    - 카카오  : "kakao_account" 맵 안에 "email"이 중첩되어 있음
        org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauthToken =
                (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId();

        // 3. 제공자에 따라 DB 조회 방식 분기
        //    - Google : 이메일로 조회 (항상 존재)
        //    - 카카오 : kakaoId로 조회 (이메일 없을 수 있음)
        User user;
        if ("kakao".equals(provider)) {
            String kakaoId = String.valueOf((Object) oAuth2User.getAttribute("id"));
            user = userRepository.findByKakaoId(kakaoId)
                    .orElseThrow(() -> new IllegalStateException("카카오 로그인 사용자를 DB에서 찾을 수 없음: " + kakaoId));
        } else {
            // Google
            String email = oAuth2User.getAttribute("email");
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("구글 로그인 사용자를 DB에서 찾을 수 없음: " + email));
        }

        // 4. JwtUtil을 이용해 해당 사용자의 JWT 토큰을 생성
        //    토큰에는 이메일, 이름, 권한, 만료 시각이 포함됨
        String token = jwtUtil.generateToken(user);

        // 5. 생성한 JWT를 URL 쿼리 파라미터로 붙여 프론트엔드 콜백 페이지로 리다이렉트
        //    프론트엔드의 /oauth/callback 페이지에서 URL의 token 파라미터를 읽어 저장함
        //    예: http://localhost:5173/oauth/callback?token=eyJhbGciOiJIUzI1NiJ9...
        //
        //    주의: URL에 토큰을 노출하는 방식은 브라우저 히스토리에 남을 수 있음.
        //    프로덕션에서는 HttpOnly 쿠키나 Authorization Code 방식을 고려할 것.
        response.sendRedirect(frontendUrl + "/oauth/callback?token=" + token);
    }
}