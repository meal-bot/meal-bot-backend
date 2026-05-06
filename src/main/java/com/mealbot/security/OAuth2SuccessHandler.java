package com.mealbot.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import com.mealbot.entity.User;
import com.mealbot.repository.UserRepository;

import java.io.IOException;

/**
 * [핸들러] Google OAuth2 로그인 성공 후 JWT를 발급하고 프론트엔드로 리다이렉트하는 핸들러.
 *
 * 처리 흐름 (이 핸들러가 호출되기 이전 단계 포함):
 *   1. 사용자가 /oauth2/authorization/google 로 접근 → Google 로그인 페이지로 이동
 *   2. Google 인증 완료 → /login/oauth2/code/google 로 콜백
 *   3. CustomOAuth2UserService.loadUser() → Google 사용자 정보를 DB에 저장/갱신
 *   4. [이 핸들러] onAuthenticationSuccess() 호출
 *      → DB에서 User 조회 → JWT 생성 → 프론트엔드 콜백 URL로 리다이렉트
 *   5. 프론트엔드(/oauth/callback)에서 URL 파라미터의 token을 꺼내 localStorage 등에 저장
 *   6. 이후 API 요청 시 Authorization: Bearer {token} 헤더로 전송
 *
 * SimpleUrlAuthenticationSuccessHandler를 상속받아
 * 기본 리다이렉트 동작을 오버라이드하고 JWT 전달 로직을 추가함.
 */
@Component
@RequiredArgsConstructor  // Lombok: final 필드를 파라미터로 받는 생성자 자동 생성 (의존성 주입)
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

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
        //    → CustomOAuth2UserService.loadUser()에서 반환한 Google 사용자 정보 객체
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 2. Google에서 받은 사용자 속성에서 이메일을 추출
        //    이메일은 User 엔티티의 고유 식별자로 사용됨
        String email = oAuth2User.getAttribute("email");

        // 3. 이메일로 DB에서 User 엔티티를 조회
        //    CustomOAuth2UserService에서 이미 저장했으므로 반드시 존재해야 함
        //    없으면 IllegalStateException 발생 (시스템 오류 상황)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 DB에서 찾을 수 없음: " + email));

        // 4. JwtUtil을 이용해 해당 사용자의 JWT 토큰을 생성
        //    토큰에는 이메일, 이름, 권한, 만료 시각이 포함됨
        String token = jwtUtil.generateToken(user);

        // 5. 생성한 JWT를 URL 쿼리 파라미터로 붙여 프론트엔드 콜백 페이지로 리다이렉트
        //    프론트엔드의 /oauth/callback 페이지에서 URL의 token 파라미터를 읽어 저장함
        //    예: http://localhost:5173/oauth/callback?token=eyJhbGciOiJIUzI1NiJ9...
        //
        //    주의: URL에 토큰을 노출하는 방식은 브라우저 히스토리에 남을 수 있음.
        //    프로덕션에서는 HttpOnly 쿠키나 Authorization Code 방식을 고려할 것.
        response.sendRedirect("http://localhost:5173/oauth/callback?token=" + token);
    }
}