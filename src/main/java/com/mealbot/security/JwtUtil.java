package com.mealbot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.mealbot.entity.User;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * [유틸리티] JWT(JSON Web Token) 생성, 파싱, 검증을 담당하는 클래스.
 *
 * JWT 구조: header.payload.signature
 *   - header    : 토큰 타입(JWT), 서명 알고리즘(HS256) 정보
 *   - payload   : 사용자 정보(이메일, 이름, 권한)와 만료 시각 등 클레임(claim)
 *   - signature : secret 키로 서명한 값 → 위변조 감지에 사용
 *
 * 사용 흐름:
 *   1. OAuth2 로그인 성공 → OAuth2SuccessHandler에서 generateToken() 호출 → JWT 발급
 *   2. 이후 API 요청마다 JwtAuthFilter에서 isValid() + getClaims() 호출 → 사용자 인증
 */
@Component  // 스프링 빈으로 등록 → 다른 클래스에서 @RequiredArgsConstructor로 주입받을 수 있음
public class JwtUtil {

    /**
     * JWT 서명에 사용되는 비밀 키 (Base64 인코딩된 문자열).
     * application.properties의 jwt.secret 값을 읽어옴.
     * 이 값이 유출되면 누구나 유효한 토큰을 위조할 수 있으므로 절대 공개하면 안 됨.
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * 토큰 유효 기간 (밀리초 단위).
     * application.properties의 jwt.expiration-ms 값을 주입받음.
     * 86400000ms = 24시간
     */
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /**
     * 사용자 정보를 담은 JWT 토큰을 생성한다.
     *
     * 토큰 payload에 포함되는 클레임(claim):
     *   - subject : 사용자 이메일 (토큰 주체, 사용자 식별에 사용)
     *   - name    : 사용자 이름
     *   - role    : 사용자 권한 (예: "USER", "ADMIN")
     *   - iat     : 발급 시각 (issuedAt)
     *   - exp     : 만료 시각 (현재 시각 + expirationMs)
     *
     * @param user JWT에 담을 사용자 엔티티
     * @return 서명된 JWT 문자열 (예: "eyJhbGciOiJIUzI1NiJ9....")
     */
    public String generateToken(User user) {
        // Google: subject = 이메일 / 카카오: subject = "kakao_{id}" (이메일 없을 수 있음)
        String subject = (user.getEmail() != null)
                ? user.getEmail()
                : "kakao_" + user.getKakaoId();

        return Jwts.builder()
                .subject(subject)                                              // 토큰 주체
                .claim("name", user.getName())                                 // 커스텀 클레임: 이름
                .claim("role", user.getRole().name())                          // 커스텀 클레임: 권한
                .issuedAt(new Date())                                          // 발급 시각
                .expiration(new Date(System.currentTimeMillis() + expirationMs)) // 만료 시각
                .signWith(getSigningKey())                                     // 비밀 키로 서명
                .compact();                                                    // 최종 JWT 문자열로 직렬화
    }

    /**
     * JWT 토큰을 파싱해 내부의 클레임(payload)을 꺼내 반환한다.
     *
     * 내부적으로 서명 검증을 수행하므로, 위변조된 토큰이면 예외가 발생함.
     * 만료된 토큰도 이 메서드에서 예외가 발생함.
     *
     * @param token 검증할 JWT 문자열
     * @return Claims 객체 (이메일: claims.getSubject(), 이름: claims.get("name") 등으로 접근)
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())  // 서명 검증에 사용할 키 설정
                .build()
                .parseSignedClaims(token)     // 토큰 파싱 + 서명 검증 (실패 시 예외 발생)
                .getPayload();                // payload(클레임) 부분만 반환
    }

    /**
     * JWT 토큰이 유효한지 검사한다.
     *
     * getClaims()를 호출해 파싱을 시도하고:
     *   - 성공하면 true (유효한 토큰, 아직 만료 안 됨)
     *   - 예외 발생 시 false (서명 불일치, 만료, 형식 오류 등 모든 이상 케이스)
     *
     * @param token 검사할 JWT 문자열
     * @return 유효하면 true, 아니면 false
     */
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            // JwtException, ExpiredJwtException, MalformedJwtException 등을 모두 포함
            return false;
        }
    }

    /**
     * application.properties의 Base64 인코딩된 secret 문자열을 디코딩해
     * HMAC-SHA 알고리즘용 SecretKey 객체로 변환한다.
     *
     * HMAC-SHA256은 대칭키 알고리즘이므로 서명과 검증에 동일한 키가 사용됨.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}