# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 빌드 및 실행 명령어

```bash
# 애플리케이션 실행
./mvnw spring-boot:run

# 빌드 (테스트 생략)
./mvnw clean install -DskipTests

# 전체 테스트 실행
./mvnw test

# 단일 테스트 클래스 실행
./mvnw test -Dtest=GuestChatServiceTests

# MySQL 시작 (앱 실행 전 필수)
docker-compose up -d
```

로컬 실행 시 Swagger UI: `http://localhost:8080/swagger-ui.html`

## 아키텍처 개요

Spring Boot 4.0.5 (Java 21) REST API. 사용자 요청을 받아 Python FastAPI AI 서버와 통신하며 식단 추천을 제공한다.

### 요청 흐름

```
Client → JwtAuthFilter → Controller → Service → AiClient (Python /chat)
                                              ↘ Repository → MySQL
```

### Python AI 서버 연동

모든 AI 통신은 `AiClient` → `http://localhost:8000` (`ai.server.url` 설정값)을 통한다.

- **POST /chat** — 모든 채팅 인텐트 처리. Spring이 `AiDto.Request`(session_id, turn_id, message, history, slots, last_recommendations)를 보내고, `AiDto.Response`(intent, answer, slots_updated, recommendations, flags)를 받는다.
- **GET /recipes/{recipe_id}** — AI 서버에서 레시피 상세 정보 조회.

인텐트 분류(`recommend`, `slot_fill`, `refine`, `ask`, `out_of_scope`)는 AI가 담당한다. Spring은 히스토리와 슬롯을 저장하고 다음 턴에 다시 전달하는 역할이다.

### 채팅 슬롯 누적

`Chat` 엔티티가 누적 슬롯(`mealTimes`, `purpose`, `freeText`)을 보관한다. 매 턴마다 `ChatService`가 AI 응답의 `slots_updated`를 기존 `Chat` 레코드에 병합한다. 최근 50개 메시지 슬라이딩 윈도우 히스토리를 AI에 매번 전송한다.

### 추천 결과 저장

AI 추천 결과는 JSON으로 직렬화되어 `ChatMessage.recommendationsJson`에 저장된다. 다음 턴에 `lastRecommendations`로 역직렬화해 AI에 전달함으로써 `refine`/`ask` 인텐트 시 컨텍스트를 유지한다.

### 게스트 채팅

`GuestChatController` / `GuestChatService`가 비인증 사용자를 처리한다. 세션은 HttpOnly 쿠키의 불투명 토큰(DB에 SHA-256 해시로 저장)으로 식별된다. 60분 비활성 시 만료되며, 스케줄러가 주기적으로 만료 세션을 정리한다.

### 인증 흐름

- Google OAuth2 / Kakao OAuth2 (Spring Security)
- 로그인 완료 후 `OAuth2SuccessHandler`가 JWT를 발급하고 프론트엔드로 리다이렉트
- 이후 모든 API 요청은 `Authorization: Bearer <token>` 헤더 사용 (세션 없음)
- Kakao subject 형식: `"kakao_{id}"` / Google subject 형식: 이메일 문자열

## 설정

프로파일 계층 구조로 설정을 관리한다:

| 파일 | 용도 |
|------|------|
| `application.properties` | 기본 설정 (DB URL, OAuth2 엔드포인트, AI 서버 URL) |
| `application-secret.properties` | 시크릿 — **gitignore 처리됨**, `.example` 파일 참고하여 직접 생성 |
| `application-prod.properties` | 프로덕션 오버라이드 |
| `application-aws.properties` | AWS EC2 오버라이드 |

`application-secret.properties`에 반드시 설정해야 할 항목:
- `spring.datasource.username` / `password`
- `spring.security.oauth2.client.registration.google.client-id` / `client-secret`
- `spring.security.oauth2.client.registration.kakao.client-id` / `client-secret`
- `jwt.secret` (Base64 인코딩, 256비트 이상)

## 패키지 구조 (`com.mealbot`)

| 패키지 | 역할 |
|--------|------|
| `client` | `AiClient` — Python AI 서버 RestClient 래퍼 |
| `controller` | HTTP 레이어: Chat, GuestChat, Recipe, Calendar, Fridge, Inbody |
| `dto` | `AiDto`(AI 프로토콜 v0.3), `ChatDto`, `RecipeDto`, `FridgeDto` 등 |
| `entity` | JPA 엔티티: `User`, `Chat`, `ChatMessage`, `Inbody` |
| `service` | 비즈니스 로직. `ChatService`가 메인 오케스트레이터 |
| `security` | JWT 필터, OAuth2 핸들러, `JwtUtil` |
| `util` | `InbodyCalculator`(BMI/BMR), `RecommendationUtils`(JSON 직렬화) |

## 데이터베이스

MySQL 8.0 (`mealbot_db`). Hibernate(`ddl-auto: update`)가 스키마를 자동 관리하므로 별도 마이그레이션 불필요. 엔티티 관계: `User` → `Chat`(1:N) → `ChatMessage`(1:N), `User` → `Inbody`(1:N). 게스트 채팅은 `user_id = NULL`.

## CORS 및 배포

CORS 허용 출처는 `app.frontend-url`, `app.frontend-url-www`, `app.frontend-url-local` 프로퍼티로 관리한다. 프로덕션은 EC2 + Nginx(HTTPS)에서 실행되며, 프론트엔드는 Vercel에 배포되어 있다. 리버스 프록시 뒤에서 올바른 리다이렉트 URI 처리를 위해 `server.forward-headers-strategy=FRAMEWORK` 설정이 필요하다.
