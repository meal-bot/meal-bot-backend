package com.mealbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import com.mealbot.entity.User;
import com.mealbot.repository.UserRepository;

/**
 * [서비스] Google · 카카오 OAuth2 로그인 시 사용자 정보를 처리하는 커스텀 서비스.
 *
 * 공통 흐름:
 *   1. 사용자가 소셜 로그인 버튼 클릭
 *   2. 각 소셜 인증 서버에서 인증 완료 후 이 서비스의 loadUser()가 호출됨
 *   3. 제공자(Google/카카오)를 구분해 사용자 정보(이메일, 이름, 사진)를 추출
 *   4. 추출한 정보를 DB에 저장하거나 업데이트
 *   5. 이후 OAuth2SuccessHandler로 흐름이 넘어가 JWT가 발급됨
 *
 * ※ Google vs 카카오 응답 구조 차이
 *   - Google : { "email": "...", "name": "...", "picture": "..." }  → 평탄한 구조
 *   - 카카오  : { "kakao_account": { "email": "...", "profile": { "nickname": "...", "profile_image_url": "..." } } } → 중첩 구조
 */
@Service
@RequiredArgsConstructor  // Lombok: final 필드를 파라미터로 받는 생성자 자동 생성 (의존성 주입)
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * 소셜 로그인 인증 완료 후 호출되는 메서드.
     * 제공자(Google/카카오)를 구분해 사용자 정보를 추출하고 DB에 저장하거나 갱신한다.
     *
     * @param userRequest OAuth2 인증 요청 정보 (제공자 식별자 및 액세스 토큰 포함)
     * @return 소셜 제공자에서 받은 OAuth2User 객체 (이후 SecurityContext에 저장됨)
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 부모 클래스의 loadUser()를 호출해 소셜 API에서 사용자 정보를 가져옴
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 현재 로그인 요청의 제공자를 확인 (google / kakao)
        String provider = userRequest.getClientRegistration().getRegistrationId();

        String email;
        String name;
        String picture;

        if ("kakao".equals(provider)) {
            // ── 카카오: 응답이 중첩 구조이므로 kakao_account 맵을 먼저 꺼낸 뒤 추출
            java.util.Map<String, Object> kakaoAccount =
                    oAuth2User.getAttribute("kakao_account");
            java.util.Map<String, Object> profile =
                    (java.util.Map<String, Object>) kakaoAccount.get("profile");

            // 카카오 고유 ID (최상위 "id" 값, 카카오 서버 발급 식별자)
            // Object로 명시적 캐스팅 후 변환 (Long → char[] 오버로드 오해 방지)
            String kakaoId = String.valueOf((Object) oAuth2User.getAttribute("id"));
            email   = (String) kakaoAccount.get("email"); // 선택 동의 → null 가능
            name    = (String) profile.get("nickname");
            picture = (String) profile.get("profile_image_url");

            // 카카오 사용자는 kakaoId로 조회 (이메일 없을 수 있음)
            User user = userRepository.findByKakaoId(kakaoId)
                    .orElseGet(() -> User.builder()
                            .kakaoId(kakaoId)
                            .role(User.Role.USER)
                            .build());

            user.setEmail(email);
            user.setName(name);
            user.setProfileImageUrl(picture);
            userRepository.save(user);
            return oAuth2User;
        } else {
            // ── Google: 응답이 평탄한 구조이므로 attribute에서 바로 추출
            email   = oAuth2User.getAttribute("email");
            name    = oAuth2User.getAttribute("name");
            picture = oAuth2User.getAttribute("picture");
        }

        // Google 사용자: 이메일로 조회
        //   - 기존 회원이면 그 User 엔티티를 가져오고
        //   - 신규 회원이면 email과 기본 권한(USER)만 설정한 새 User 엔티티를 생성
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> User.builder()
                        .email(email)
                        .role(User.Role.USER)
                        .build());

        // 이름과 프로필 이미지는 로그인할 때마다 최신 정보로 갱신
        user.setName(name);
        user.setProfileImageUrl(picture);

        // 변경 사항을 DB에 저장 (신규면 INSERT, 기존이면 UPDATE)
        userRepository.save(user);

        // 스프링 시큐리티 인증 흐름에 맞게 OAuth2User 객체를 그대로 반환
        // → 이후 OAuth2SuccessHandler.onAuthenticationSuccess()가 호출됨
        return oAuth2User;
    }
}