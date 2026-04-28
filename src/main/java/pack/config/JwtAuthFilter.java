package pack.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pack.repository.UserRepository;

import java.io.IOException;
import java.util.List;

/**
 * [필터] 매 HTTP 요청마다 JWT 토큰을 검사해 사용자를 인증하는 필터.
 *
 * OncePerRequestFilter를 상속받아 하나의 요청에 대해 정확히 한 번만 실행됨을 보장함.
 *
 * 처리 흐름:
 *   1. 요청 헤더에서 "Authorization: Bearer {토큰}" 형태의 토큰을 꺼냄
 *   2. 토큰이 유효하면 JWT에서 이메일을 추출해 DB에서 사용자를 조회
 *   3. 조회된 사용자를 스프링 시큐리티의 SecurityContext에 등록
 *      → 이후 컨트롤러에서 @AuthenticationPrincipal 등으로 현재 사용자를 가져올 수 있음
 *   4. 다음 필터 또는 컨트롤러로 요청을 넘김
 *
 * 이 필터는 SecurityConfig에서 OAuth2LoginAuthenticationFilter 뒤에 등록됨.
 */
@Component
@RequiredArgsConstructor  // Lombok: final 필드를 파라미터로 받는 생성자 자동 생성 (의존성 주입)
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    /**
     * 실제 JWT 검증 및 인증 처리 로직.
     *
     * @param request     HTTP 요청 (헤더에서 토큰을 꺼냄)
     * @param response    HTTP 응답
     * @param filterChain 다음 필터로 요청을 넘기기 위한 체인
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 요청 헤더에서 "Authorization" 값을 읽어옴
        //    정상적인 형태: "Bearer eyJhbGciOiJIUzI1NiJ9...."
        String header = request.getHeader("Authorization");

        // 2. Authorization 헤더가 없거나 "Bearer "로 시작하지 않으면 JWT 인증을 건너뜀
        //    → 다음 필터로 요청을 그냥 넘김 (permitAll() 경로는 이 상태로도 접근 가능)
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. "Bearer " 접두사(7글자)를 제거해 순수 JWT 문자열만 추출
        String token = header.substring(7);

        // 4. JWT 유효성 검사 (서명 확인 + 만료 여부 확인)
        if (jwtUtil.isValid(token)) {

            // 5. 토큰에서 클레임(payload)을 꺼냄
            Claims claims = jwtUtil.getClaims(token);

            // 6. 클레임의 subject(주체)에서 이메일을 추출
            String email = claims.getSubject();

            // 7. 이메일로 DB에서 실제 사용자를 조회하고, 존재하면 인증 처리
            userRepository.findByEmail(email).ifPresent(user -> {

                // 스프링 시큐리티의 인증 객체 생성:
                //   - principal   : 인증된 사용자 (User 엔티티)
                //   - credentials : 비밀번호 (JWT 방식에선 필요 없으므로 null)
                //   - authorities : 사용자 권한 목록 (예: "ROLE_USER", "ROLE_ADMIN")
                var auth = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );

                // 8. 생성한 인증 객체를 SecurityContext에 저장
                //    → 이후 컨트롤러에서 이 요청이 인증된 요청으로 인식됨
                //    → SecurityConfig의 .anyRequest().authenticated() 조건을 통과함
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        // 9. 인증 처리 여부와 관계없이 항상 다음 필터로 요청을 전달
        filterChain.doFilter(request, response);
    }
}