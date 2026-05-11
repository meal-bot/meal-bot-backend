package com.mealbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mealbot.entity.User;

import java.util.Optional;

/**
 * [레포지토리] User 엔티티에 대한 DB 접근 인터페이스.
 *
 * JpaRepository<User, Long>를 상속받으면 아래 기본 메서드들이 자동으로 제공됨:
 *   - save(user)        : INSERT 또는 UPDATE
 *   - findById(id)      : id로 단건 조회
 *   - findAll()         : 전체 조회
 *   - deleteById(id)    : 삭제
 *
 * Spring Data JPA가 인터페이스 이름만 보고 구현체를 자동으로 생성해주므로
 * 직접 SQL을 작성할 필요 없음.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자를 조회한다.
     *
     * Spring Data JPA의 메서드 이름 규칙에 따라
     * "findBy + 필드명"으로 메서드명을 작성하면 SQL이 자동 생성됨.
     * → SELECT * FROM users WHERE email = ?
     *
     * Optional<User> 반환 이유: 해당 이메일의 사용자가 없을 수 있으므로
     * null 대신 Optional을 사용해 NullPointerException을 방지.
     *
     * 사용 위치:
     *   - CustomOAuth2UserService : OAuth2 로그인 시 기존 회원 조회 또는 신규 가입 처리
     *   - OAuth2SuccessHandler    : JWT 생성을 위해 User 엔티티 조회
     *   - JwtAuthFilter           : 매 요청마다 JWT의 이메일로 사용자 인증 처리
     */
    Optional<User> findByEmail(String email);

    /**
     * 카카오 고유 ID로 사용자를 조회한다.
     * JWT subject가 "kakao_" 접두사인 경우 JwtAuthFilter에서 사용.
     * → SELECT * FROM users WHERE kakao_id = ?
     */
    Optional<User> findByKakaoId(String kakaoId);
}