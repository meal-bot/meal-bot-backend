package com.mealbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mealbot.dto.ChatDto;
import com.mealbot.entity.User;
import com.mealbot.service.ChatService;

import java.util.List;
import java.util.Map;

@Tag(name = "Chat", description = "채팅 세션 및 메시지 관련 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** POST /api/chat — 1. 새 채팅 스레드 생성 */
    @Operation(summary = "새 채팅 세션 생성", description = "로그인 사용자의 새 채팅 스레드를 생성합니다.")
    @ApiResponse(responseCode = "200", description = "생성된 채팅 세션 정보 반환")
    @PostMapping
    public ResponseEntity<ChatDto.ChatResponse> createChat(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.createChat(user));
    }

    // /** POST /api/chat/guest/sendMessage — 게스트 메시지 전송 (v0.3 비활성화, 추후 보완 예정) */
    // @Operation(summary = "게스트 메시지 전송", description = "비로그인 사용자의 메시지를 처리합니다. DB 저장 없음.")
    // @ApiResponse(responseCode = "200", description = "AI 응답 텍스트 및 추천 레시피 반환")
    // @PostMapping("/guest/sendMessage")
    // public ResponseEntity<ChatDto.GuestSendResponse> sendMessageToGuest(
    //         @RequestBody ChatDto.GuestSendRequest request) {
    //     return ResponseEntity.ok(chatService.sendGuest(request));
    // }

    /** POST /api/chat/{chatId}/sendMessage — 2. 메시지 전송 + AI 응답 */
    @Operation(summary = "메시지 전송 및 AI 응답", description = "사용자 메시지를 저장하고 AI 응답을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "AI 응답 텍스트 및 추천 레시피 반환")
    @PostMapping("/{chatId}/sendMessage")
    public ResponseEntity<ChatDto.SendResponse> sendMessage(
            @AuthenticationPrincipal User user,
            @Parameter(description = "채팅 세션 ID") @PathVariable Long chatId,
            @RequestBody ChatDto.SendRequest request) {
        return ResponseEntity.ok(chatService.send(user, chatId, request));
    }

    /** GET /api/chat — 3. 내 채팅 목록 조회 (사이드바) */
    @Operation(summary = "내 채팅 목록 조회", description = "사이드바용 채팅 세션 목록을 최신순으로 반환합니다.")
    @ApiResponse(responseCode = "200", description = "채팅 세션 목록 반환")
    @GetMapping
    public ResponseEntity<List<ChatDto.ChatResponse>> getChats(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getChats(user));
    }

    /** GET /api/chat/{chatId} — 4. 특정 채팅의 전체 메시지 조회 */
    @Operation(summary = "채팅 상세 조회", description = "특정 채팅 세션의 전체 메시지 목록을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "채팅 세션 및 메시지 목록 반환")
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDto.ChatDetailResponse> getMessages(
            @AuthenticationPrincipal User user,
            @Parameter(description = "채팅 세션 ID") @PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.getChat(user, chatId));
    }

    /** DELETE /api/chat/{chatId} — 5. 채팅 삭제 */
    @Operation(summary = "채팅 삭제", description = "채팅 세션과 하위 메시지를 모두 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "삭제 성공 여부 반환")
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Map<String, Boolean>> deleteChat(
            @AuthenticationPrincipal User user,
            @Parameter(description = "채팅 세션 ID") @PathVariable Long chatId) {
        chatService.deleteChat(user, chatId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // AI 도입 전 에코 패턴용 — POST /api/chat/{chatId}/messages (7번 엔드포인트) 비활성화
    // @PostMapping("/{chatId}/messages")
    // public ResponseEntity<ChatDto.ChatMessageResponse> addChatMessage(...) { ... }
}