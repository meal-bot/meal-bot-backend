package com.mealbot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * [엔티티] 사용자(User) 테이블과 매핑되는 JPA 엔티티 클래스.
 *
 * 이 앱은 Google OAuth2 로그인만 지원하므로 비밀번호 필드가 없음.
 * 사용자는 Google 계정으로 처음 로그인할 때 자동으로 DB에 등록됨.
 */
@Entity                     // JPA가 이 클래스를 DB 테이블과 연결되는 엔티티로 인식
@Table(name = "users")      // 연결할 DB 테이블 이름을 "users"로 지정
@Getter @Setter             // Lombok: 모든 필드의 getter/setter 자동 생성
@Builder                    // Lombok: User.builder().email(...).build() 형태의 빌더 패턴 지원
@NoArgsConstructor          // Lombok: 기본 생성자(파라미터 없음) 자동 생성 - JPA 필수
@AllArgsConstructor         // Lombok: 모든 필드를 받는 생성자 자동 생성 - @Builder와 함께 사용
public class User {

    /**
     * 사용자 고유 식별자 (Primary Key).
     * DB에서 INSERT 시 자동으로 1씩 증가하는 값이 할당됨 (AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자 이메일 주소.
     * - unique = true : 같은 이메일로 중복 가입 불가
     * - nullable = false : 반드시 값이 있어야 함
     * - OAuth2 로그인 시 Google에서 제공하는 email 값이 저장됨
     * - JWT 토큰의 subject(주체)로 사용되어 사용자를 식별하는 핵심 키
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * 사용자 이름 (Google 계정 표시 이름).
     * 로그인 후 CustomOAuth2UserService에서 Google의 "name" 속성 값으로 저장/갱신됨.
     */
    private String name;

    /**
     * 사용자 프로필 이미지 URL.
     * Google 계정의 "picture" 속성 값으로 저장됨.
     * 로그인할 때마다 최신 이미지로 갱신됨.
     */
    private String profileImageUrl;

    /**
     * 사용자 권한(역할).
     * - @Enumerated(EnumType.STRING) : DB에 숫자(0,1) 대신 문자열("USER","ADMIN")로 저장
     * - nullable = false : 반드시 값이 있어야 함
     * - 신규 가입 시 기본값으로 Role.USER가 설정됨 (CustomOAuth2UserService 참고)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * 계정 생성 일시.
     * - @CreationTimestamp : 최초 INSERT 시점의 시각이 자동으로 기록됨
     * - updatable = false : 한번 기록된 후 UPDATE 시에도 변경되지 않음
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * 사용자 권한 등급을 나타내는 열거형(Enum).
     * - USER  : 일반 사용자 (기본값)
     * - ADMIN : 관리자
     */
    public enum Role {
        USER, ADMIN
    }
}