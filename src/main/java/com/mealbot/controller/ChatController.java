package com.mealbot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mealbot.dto.ChatDto;
import com.mealbot.entity.User;
import com.mealbot.service.ChatService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** POST /api/chat — 1. 새 채팅 스레드 생성 */
    @PostMapping
    public ResponseEntity<ChatDto.ChatResponse> createChat(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.createChat(user));
    }

    /** POST /api/chat/{chatId}/sendMessage — 2. 메시지 전송 + AI 응답 */
    @PostMapping("/{chatId}/sendMessage")
    public ResponseEntity<ChatDto.SendResponse> sendMessage(
            @AuthenticationPrincipal User user,
            @PathVariable Long chatId,
            @RequestBody ChatDto.SendRequest request) {
        return ResponseEntity.ok(chatService.send(user, chatId, request));
    }

    /** GET /api/chat — 3. 내 채팅 목록 조회 (사이드바) */
    @GetMapping
    public ResponseEntity<List<ChatDto.ChatResponse>> getChats(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getChats(user));
    }

    /** GET /api/chat/{chatId} — 4. 특정 채팅의 전체 메시지 조회 */
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDto.ChatDetailResponse> getMessages(
            @AuthenticationPrincipal User user,
            @PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.getChat(user, chatId));
    }

    /** DELETE /api/chat/{chatId} — 5. 채팅 삭제 */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Map<String, Boolean>> deleteChat(
            @AuthenticationPrincipal User user,
            @PathVariable Long chatId) {
        chatService.deleteChat(user, chatId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // AI 도입 전 에코 패턴용 — POST /api/chat/{chatId}/messages (7번 엔드포인트) 비활성화
    // @PostMapping("/{chatId}/messages")
    // public ResponseEntity<ChatDto.ChatMessageResponse> addChatMessage(...) { ... }
}
