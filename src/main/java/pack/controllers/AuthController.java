package pack.controllers;

import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * [컨트롤러] 인증 관련 REST API 엔드포인트를 제공하는 컨트롤러.
 *
 * 현재 상태:
 *   - /api/signup, /api/login : 임시 구현 (실제 DB 저장/검증 없음)
 *     → Google OAuth2 로그인으로 전환할 예정이므로 현재는 테스트용 스텁(stub)으로 남겨둠
 *   - /api/chat : 프론트엔드 연결 테스트용 에코 엔드포인트
 *
 * 실제 Google OAuth2 로그인 흐름은 이 컨트롤러가 아닌
 * SecurityConfig → CustomOAuth2UserService → OAuth2SuccessHandler에서 처리됨.
 */
@RestController   // 이 클래스가 REST API 컨트롤러임을 선언 → 메서드의 반환값이 JSON으로 자동 변환되어 HTTP 응답 본문(body)으로 전달됨
@RequestMapping("/api")   // 이 컨트롤러의 모든 엔드포인트 URL 앞에 "/api"가 붙음
public class AuthController {

    /**
     * [임시] POST /api/signup - 회원가입 API. -- 미구현
     *
     * 현재는 DB 저장 없이 성공 메시지만 반환하는 스텁(stub) 구현.
     * 실제 회원가입은 Google OAuth2 로그인 시 CustomOAuth2UserService에서 자동으로 처리됨.
     *
     * @param data 프론트엔드에서 전송한 JSON (예: {"username": "...", "password": "..."})
     * @return 성공 메시지 JSON (예: {"message": "회원가입 성공!"})
     */
//    @PostMapping("/signup")
//    public Map<String, String> signup(@RequestBody Map<String, String> data) {
//        Map<String, String> response = new HashMap<>();
//        response.put("message", "회원가입 성공!");
//        return response;
//    }

    /**
     * [임시] POST /api/login - 로그인 API. -- 미구현
     *
     * 현재는 가짜 JWT 토큰을 반환하는 스텁(stub) 구현.
     * 실제 로그인은 Google OAuth2 로그인으로 처리하며,
     * 성공 시 OAuth2SuccessHandler에서 진짜 JWT가 발급됨.
     *
     * @param data 프론트엔드에서 전송한 JSON (예: {"username": "...", "password": "..."})
     * @return 가짜 토큰과 사용자 이름 JSON (예: {"token": "...", "userName": "..."})
     */
//    @PostMapping("/login")
//    public Map<String, String> login(@RequestBody Map<String, String> data) {
//        Map<String, String> response = new HashMap<>();
//        response.put("token", "fake-jwt-token-for-test");          // 테스트용 가짜 토큰
//        response.put("userName", data.get("username"));             // 입력한 아이디를 그대로 이름으로 반환
//        return response;
//    }
}