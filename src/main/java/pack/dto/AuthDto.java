// ──────────────────────────────────────────────────────────────────────────────
// [현재 미사용] AuthDto - 회원가입/로그인 요청 데이터 전송 객체(DTO)
//
// DTO(Data Transfer Object): 클라이언트가 보낸 JSON 데이터를 자바 객체로 받기 위한 클래스.
// 예) POST /api/signup 요청 본문 {"username":"홍길동","password":"1234"}
//     → SignupRequest 객체로 자동 변환 (Jackson 라이브러리가 처리)
//
// 현재 주석 처리된 이유:
//   AuthController에서 Map<String, String>으로 직접 받고 있어 아직 사용하지 않음.
//   Google OAuth2 로그인 방식에서는 username/password가 필요 없으므로
//   이 DTO는 추후 일반 로그인 기능 추가 시 활성화 예정.
// ──────────────────────────────────────────────────────────────────────────────

//package pack.dto;
//
//public class AuthDto {
//
//    /**
//     * 회원가입 요청 DTO.
//     * POST /api/signup 요청 본문을 이 객체로 매핑.
//     */
//    public static class SignupRequest {
//        private String username; // 사용자 아이디
//        private String password; // 비밀번호 (저장 전에 반드시 BCrypt 등으로 해시해야 함)
//
//        public String getUsername() { return username; }
//        public void setUsername(String username) { this.username = username; }
//        public String getPassword() { return password; }
//        public void setPassword(String password) { this.password = password; }
//    }
//
//    /**
//     * 로그인 요청 DTO.
//     * POST /api/login 요청 본문을 이 객체로 매핑.
//     */
//    public static class LoginRequest {
//        private String username; // 사용자 아이디
//        private String password; // 비밀번호 (DB의 해시값과 비교)
//
//        public String getUsername() { return username; }
//        public void setUsername(String username) { this.username = username; }
//        public String getPassword() { return password; }
//        public void setPassword(String password) { this.password = password; }
//    }
//}