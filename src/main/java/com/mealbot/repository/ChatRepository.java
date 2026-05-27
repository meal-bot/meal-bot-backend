package com.mealbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mealbot.entity.Chat;
import com.mealbot.entity.User;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByUserOrderByCreatedAtDesc(User user);

    Optional<Chat> findByIdAndUser(Long id, User user);

    // [codex] 게스트는 쿠키 토큰으로만 접근하며 사용자 채팅 조회에는 포함되지 않는다.
    Optional<Chat> findByGuestTokenHashAndUserIsNull(String guestTokenHash);

    // [codex] 만료 정리 작업은 로그인 채팅이 아닌 만료된 게스트 대화만 조회한다.
    List<Chat> findByUserIsNullAndExpiresAtBefore(LocalDateTime now);
}
