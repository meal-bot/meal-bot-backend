package pack.service;

// ──────────────────────────────────────────────────────────────────────────────
// [현재 미사용] AuthService - 회원가입/로그인 비즈니스 로직 서비스
//
// 현재 상태:
//   - 이 클래스는 스프링 빈(@Service)으로 등록되지 않아 실제로 사용되지 않음.
//   - AuthController에서도 이 서비스를 주입받아 사용하지 않음.
//
// 활성화 필요 시:
//   1. @Service 어노테이션 추가
//   2. UserRepository, PasswordEncoder, JwtUtil 등을 주입받아 실제 로직 구현
//   3. AuthController에서 @RequiredArgsConstructor로 이 서비스를 주입해서 사용
//
// 현재 앱의 실제 인증 흐름은 아래 경로로 처리됨:
//   Google OAuth2 로그인 → CustomOAuth2UserService → OAuth2SuccessHandler → JWT 발급
// ──────────────────────────────────────────────────────────────────────────────
public class AuthService {

    /**
     * [미구현] 회원가입 처리.
     *
     * 향후 구현 시 필요한 작업:
     *   1. username 중복 여부 확인 (UserRepository.existsByUsername())
     *   2. password를 BCryptPasswordEncoder로 해시 처리
     *   3. User 엔티티 생성 후 UserRepository.save()로 DB에 저장
     *   4. 성공/실패 응답 반환
     */
    public String signup(String username, String password) {
        // TODO: DB 저장 로직 구현
        return "회원가입 성공";
    }

    /**
     * [미구현] 로그인 처리 및 JWT 발급.
     *
     * 향후 구현 시 필요한 작업:
     *   1. username으로 DB에서 사용자 조회 (없으면 예외)
     *   2. 입력한 password와 DB의 해시값을 BCryptPasswordEncoder.matches()로 비교
     *   3. 일치하면 JwtUtil.generateToken()으로 JWT 생성 후 반환
     *   4. 불일치 시 인증 실패 예외 반환
     */
    public String login(String username, String password) {
        // TODO: 실제 인증 및 JWT 발급 로직 구현
        return "fake-jwt-token";
    }
}