package com.mealbot.controller;

import com.mealbot.dto.ChatDto;
import com.mealbot.service.GuestChatException;
import com.mealbot.service.GuestChatService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [codex] 한 건의 일회성 게스트 대화를 위한 API 경계.
 * 게스트에게 과거 내역이나 목록 조회 엔드포인트는 의도적으로 제공하지 않는다.
 */
@RestController
@RequestMapping("/api/chat/guest")
@RequiredArgsConstructor
public class GuestChatController {

    private final GuestChatService guestChatService;

    @PostMapping
    public ResponseEntity<ChatDto.GuestChatResponse> createChat(
            @CookieValue(value = GuestChatService.COOKIE_NAME, required = false) String existingToken,
            HttpServletResponse response) {
        // [codex] 새 채팅을 생성할 때 화면에서 더 이상 보이지 않는 이전 게스트 대화도 폐기한다.
        GuestChatService.CreateResult createdChat = guestChatService.createChat(existingToken);
        response.addHeader(HttpHeaders.SET_COOKIE, guestChatService.createCookie(createdChat.token()));
        return ResponseEntity.ok(new ChatDto.GuestChatResponse(createdChat.expiresAt()));
    }

    @PostMapping("/sendMessage")
    public ResponseEntity<ChatDto.SendResponse> sendMessage(
            @CookieValue(value = GuestChatService.COOKIE_NAME, required = false) String token,
            @Valid @RequestBody ChatDto.SendRequest request,
            HttpServletResponse response) {
        // [codex] 쿠키가 서버 Chat을 식별하므로 클라이언트는 새 입력 내용만 전송한다.
        GuestChatService.MessageResult result = guestChatService.send(token, request);
        response.addHeader(HttpHeaders.SET_COOKIE, guestChatService.createCookie(result.token()));
        return ResponseEntity.ok(result.response());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteChat(
            @CookieValue(value = GuestChatService.COOKIE_NAME, required = false) String token,
            HttpServletResponse response) {
        guestChatService.deleteChat(token);
        response.addHeader(HttpHeaders.SET_COOKIE, guestChatService.deleteCookie());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(GuestChatException.class)
    public ResponseEntity<ChatDto.ErrorResponse> handleGuestChatException(
            GuestChatException exception,
            HttpServletResponse response) {
        // [codex] 누락되거나 만료된 서버 채팅에 브라우저 쿠키가 계속 연결되지 않도록 정리한다.
        if (exception.isClearCookie()) {
            response.addHeader(HttpHeaders.SET_COOKIE, guestChatService.deleteCookie());
        }
        return ResponseEntity.status(exception.getStatus())
                .body(new ChatDto.ErrorResponse(exception.getCode(), exception.getMessage()));
    }
}
