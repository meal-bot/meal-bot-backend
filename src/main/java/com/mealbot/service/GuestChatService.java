package com.mealbot.service;

import com.mealbot.dto.ChatDto;
import com.mealbot.entity.Chat;
import com.mealbot.exception.GuestChatException;
import com.mealbot.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

/**
 * [codex] 게스트 브라우저의 임시 채팅을 서버에서 관리한다.
 * 메시지와 AI 문맥은 기존 Chat 모델을 재사용하며, HttpOnly 쿠키를 통해
 * 마지막으로 수락된 메시지 이후 1시간 동안만 접근을 허용한다.
 */
@Service
@RequiredArgsConstructor
public class GuestChatService {

    // [codex] 불투명 토큰 원문은 브라우저만 보유하고 MySQL에는 SHA-256 해시만 저장한다.
    public static final String COOKIE_NAME = "mealbot_guest_chat";
    private static final String DEFAULT_CHAT_TITLE = "새 채팅";
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    private final ChatRepository chatRepository;
    private final ChatService chatService;

    @Value("${app.guest-chat.ttl-minutes:60}")
    private long ttlMinutes;

    @Value("${app.guest-chat.cookie-secure:false}")
    private boolean cookieSecure;

    public record CreateResult(String token, OffsetDateTime expiresAt) {}

    public record MessageResult(ChatDto.SendResponse response, String token, OffsetDateTime expiresAt) {}

    @Transactional
    public CreateResult createChat(String existingToken) {
        // [codex] 새 채팅 또는 새로고침 후 첫 메시지는 이전 문맥을 버리고 일회성 채팅을 새로 만든다.
        deleteByToken(existingToken);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = nextExpiry();
        chatRepository.save(Chat.builder()
                .title(DEFAULT_CHAT_TITLE)
                .guestTokenHash(hash(token))
                .expiresAt(expiresAt)
                .build());
        return new CreateResult(token, expiresAt.atOffset(KST));
    }

    // [codex] 만료 채팅을 거부하는 예외가 발생해도 바로 삭제한 데이터는 롤백하지 않는다.
    @Transactional(noRollbackFor = GuestChatException.class)
    public MessageResult send(String token, ChatDto.SendRequest request) {
        Chat chat = requireActiveChat(token);
        // [codex] 활동 중인 대화는 유지하되 과거 목록 복원 기능은 제공하지 않도록 만료를 연장한다.
        LocalDateTime expiresAt = nextExpiry();
        chat.setExpiresAt(expiresAt);
        ChatDto.SendResponse response = chatService.processMessage(
                chat, request, "guest-" + chat.getId());
        return new MessageResult(response, token, expiresAt.atOffset(KST));
    }

    @Transactional
    public void deleteChat(String token) {
        deleteByToken(token);
    }

    @Scheduled(fixedDelayString = "${app.guest-chat.cleanup-interval-ms:3600000}")
    @Transactional
    public void deleteExpiredChats() {
        // [codex] 방치된 게스트 데이터만 정리하며 로그인 사용자 채팅은 삭제 대상에 포함하지 않는다.
        chatRepository.deleteAll(chatRepository.findByUserIsNullAndExpiresAtBefore(LocalDateTime.now()));
    }

    public String createCookie(String token) {
        return cookie(token, Duration.ofMinutes(ttlMinutes)).toString();
    }

    public String deleteCookie() {
        return cookie("", Duration.ZERO).toString();
    }

    private ResponseCookie cookie(String token, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/chat/guest")
                .maxAge(maxAge)
                .build();
    }

    private Chat requireActiveChat(String token) {
        if (token == null || token.isBlank()) {
            throw new GuestChatException(
                    HttpStatus.UNAUTHORIZED,
                    "GUEST_CHAT_REQUIRED",
                    "A guest chat is required.",
                    false);
        }

        Chat chat = chatRepository.findByGuestTokenHashAndUserIsNull(hash(token))
                .orElseThrow(() -> new GuestChatException(
                        HttpStatus.UNAUTHORIZED,
                        "GUEST_CHAT_REQUIRED",
                        "A guest chat is required.",
                        true));

        if (chat.getExpiresAt() == null || !chat.getExpiresAt().isAfter(LocalDateTime.now())) {
            // [codex] 정리 작업 실행 전이라도 만료된 문맥은 즉시 거부하고 삭제한다.
            chatRepository.delete(chat);
            throw new GuestChatException(
                    HttpStatus.GONE,
                    "GUEST_CHAT_EXPIRED",
                    "The guest chat has expired.",
                    true);
        }
        return chat;
    }

    private void deleteByToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        chatRepository.findByGuestTokenHashAndUserIsNull(hash(token))
                .ifPresent(chatRepository::delete);
    }

    private LocalDateTime nextExpiry() {
        return LocalDateTime.now().plusMinutes(ttlMinutes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
