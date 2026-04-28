package pack.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import pack.dto.AiDto;

public class ChatDto {

    // 게스트 메시지 전송 요청 시 메시지 형식 (role + content)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageRequest {
        private String role;
        private String content;
    }

    // POST /api/chats/{chatId}/sendMessage 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRequest {
        private String content;
    }

    // POST /api/chats/guest/sendMessage 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuestSendRequest {
        private List<ChatMessageRequest> messages;
    }

    @Getter
    @AllArgsConstructor
    public static class GuestSendResponse {
        private String reply;
        private List<AiDto.RecipeResult> results;
    }

    // 메시지 전송 응답 (AI 응답 포함)
    @Getter
    @AllArgsConstructor
    public static class SendResponse {
        private Long messageId;
        private String reply;
        private List<AiDto.RecipeResult> results;
    }

    // 채팅 생성/목록 응답 (사이드바용)
    @Getter
    @AllArgsConstructor
    public static class ChatResponse {
        private Long chatId;
        private String title;
        private LocalDateTime createdAt;
    }

    // 메시지 단건 응답
    @Getter
    @AllArgsConstructor
    public static class ChatMessageResponse {
        private Long messageId;
        private String role;
        private String content;
        private LocalDateTime createdAt;
    }

    // 채팅 상세 조회 응답 (이전 대화 복원용)
    @Getter
    @AllArgsConstructor
    public static class ChatDetailResponse {
        private Long chatId;
        private String title;
        private LocalDateTime createdAt;
        private List<ChatMessageResponse> messages;
    }
}