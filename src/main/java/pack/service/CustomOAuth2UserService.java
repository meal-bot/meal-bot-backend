package pack.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import pack.entity.User;
import pack.repository.UserRepository;

/**
 * [서비스] Google OAuth2 로그인 시 사용자 정보를 처리하는 커스텀 서비스.
 *
 * 흐름:
 *   1. 사용자가 "Google로 로그인" 버튼 클릭
 *   2. Google 인증 서버에서 인증 완료 후 이 서비스의 loadUser()가 호출됨
 *   3. Google에서 받은 사용자 정보(이메일, 이름, 사진)를 DB에 저장하거나 업데이트
 *   4. 이후 OAuth2SuccessHandler로 흐름이 넘어가 JWT가 발급됨
 *
 * DefaultOAuth2UserService를 상속받아 기본 Google API 호출 기능을 재사용하고,
 * loadUser() 메서드만 오버라이드해서 DB 저장 로직을 추가함.
 */
@Service
@RequiredArgsConstructor  // Lombok: final 필드를 파라미터로 받는 생성자 자동 생성 (의존성 주입)
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * Google 인증 완료 후 호출되는 메서드.
     * Google API에서 사용자 프로필을 가져와 DB에 저장하거나 정보를 갱신한다.
     *
     * @param userRequest Google OAuth2 인증 요청 정보 (액세스 토큰 포함)
     * @return Google에서 받은 OAuth2User 객체 (이후 SecurityContext에 저장됨)
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 부모 클래스의 loadUser()를 호출해 Google API에서 사용자 정보를 가져옴
        // → Google People API를 통해 이메일, 이름, 프로필 사진 등을 조회함
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Google에서 받은 속성(attribute)에서 필요한 정보를 추출
        String email   = oAuth2User.getAttribute("email");    // 이메일 (고유 식별자로 사용)
        String name    = oAuth2User.getAttribute("name");     // 구글 계정 표시 이름
        String picture = oAuth2User.getAttribute("picture"); // 프로필 이미지 URL

        // DB에서 이메일로 사용자를 조회:
        //   - 기존 회원이면 그 User 엔티티를 가져오고
        //   - 신규 회원이면 email과 기본 권한(USER)만 설정한 새 User 엔티티를 생성
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> User.builder()
                        .email(email)
                        .role(User.Role.USER)  // 신규 가입자는 기본 권한 USER로 설정
                        .build());

        // 이름과 프로필 이미지는 로그인할 때마다 최신 정보로 갱신
        // (Google 계정에서 이름이나 사진을 변경했을 경우에도 반영됨)
        user.setName(name);
        user.setProfileImageUrl(picture);

        // 변경 사항을 DB에 저장 (신규면 INSERT, 기존이면 UPDATE)
        userRepository.save(user);

        // 스프링 시큐리티 인증 흐름에 맞게 Google의 OAuth2User 객체를 그대로 반환
        // → 이후 OAuth2SuccessHandler.onAuthenticationSuccess()가 호출됨
        return oAuth2User;
    }
}