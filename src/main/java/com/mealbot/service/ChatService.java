package com.mealbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mealbot.client.AiClient;
import com.mealbot.dto.AiDto;
import com.mealbot.dto.ChatDto;
import com.mealbot.entity.Chat;
import com.mealbot.entity.ChatMessage;
import com.mealbot.entity.User;
import com.mealbot.repository.ChatMessageRepository;
import com.mealbot.repository.ChatRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String DEFAULT_CHAT_TITLE = "새 채팅";
    private static final int CHAT_TITLE_MAX_LENGTH = 20;
    private static final int HISTORY_WINDOW_SIZE = 10;

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiClient aiClient;

    /** 새 채팅 세션을 생성하고 기본 제목으로 저장한다. */
    @Transactional
    public ChatDto.ChatResponse createChat(User user) {
        Chat chat = Chat.builder()
                .user(user)
                .title(DEFAULT_CHAT_TITLE)
                .build();
        chatRepository.save(chat);
        return toChatResponse(chat);
    }

    /** 사용자의 채팅 세션 목록을 최신순으로 반환한다. */
    public List<ChatDto.ChatResponse> getChats(User user) {
        return chatRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map((chat) -> this.toChatResponse(chat))
                .toList();
    }

    /** 특정 채팅 세션의 전체 메시지 히스토리를 반환한다. */
    public ChatDto.ChatDetailResponse getChat(User user, Long chatId) {
        Chat chat = chatRepository.findByIdAndUser(chatId, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없습니다: " + chatId));

        List<ChatDto.ChatMessageResponse> messages = chatMessageRepository
                .findByChatOrderByCreatedAt(chat).stream()
                .map((m) -> this.toChatMessageResponse(m))
                .toList();

        return new ChatDto.ChatDetailResponse(chat.getId(), chat.getTitle(), chat.getCreatedAt(), messages);
    }

    /**
     * 로그인 사용자의 메시지를 저장하고 AI 응답을 반환한다.
     * DB에서 최근 대화 히스토리(최대 10개)를 조회해 Python AI에 함께 전달한다.
     */
    @Transactional
    public ChatDto.SendResponse send(User user, Long chatId, ChatDto.SendRequest request) {
        Chat chat = chatRepository.findByIdAndUser(chatId, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없습니다: " + chatId));

        String userMessage = request.getContent();
        if (DEFAULT_CHAT_TITLE.equals(chat.getTitle())) {
            chat.setTitle(createChatTitle(userMessage));
        }

        chatMessageRepository.save(ChatMessage.builder()
                .chat(chat).role(ChatMessage.ROLE_USER).content(userMessage).build());

        List<ChatMessage> allMessages = chatMessageRepository.findByChatOrderByCreatedAt(chat);
        List<AiDto.MessageDto> history = allMessages.stream()
                .skip(Math.max(0, allMessages.size() - HISTORY_WINDOW_SIZE))
                .map(m -> new AiDto.MessageDto(m.getRole(), m.getContent()))
                .toList();

        // [임시 테스트용] AiDto.Response aiResponse = new AiDto.Response(userMessage, List.of());
        AiDto.Response aiResponse = aiClient.ask(userMessage, history);

        ChatMessage reply = chatMessageRepository.save(
                ChatMessage.builder()
                        .chat(chat).role(ChatMessage.ROLE_ASSISTANT).content(aiResponse.getAnswer()).build());

        return new ChatDto.SendResponse(reply.getId(), aiResponse.getAnswer(), aiResponse.getResults());
    }
    
    /**
     * 비로그인 게스트의 메시지를 처리한다.
     * 히스토리는 프론트엔드가 관리하며 messages 배열로 전달받는다. DB 저장 없음.
     */
    public ChatDto.GuestSendResponse sendGuest(ChatDto.GuestSendRequest request) {
        List<ChatDto.ChatMessageRequest> incoming = request.getMessages();
        String query = incoming.getLast().getContent();
        List<AiDto.MessageDto> history = incoming.stream()
                .skip(Math.max(0, incoming.size() - HISTORY_WINDOW_SIZE))
                .map(m -> new AiDto.MessageDto(m.getRole(), m.getContent()))
                .toList();
        AiDto.Response aiResponse = aiClient.ask(query, history);
        return new ChatDto.GuestSendResponse(aiResponse.getAnswer(), aiResponse.getResults());
    }

    // AI 도입 전 에코 패턴용 — send()가 유저 메시지 저장 + AI 호출 + 응답 저장을 한 번에 처리하므로 중복
    // @Transactional
    // public ChatDto.ChatMessageResponse addChatMessage(User user, Long chatId, ChatDto.ChatMessageRequest request) { ... }

    /** 채팅 세션과 하위 메시지를 모두 삭제한다. */
    @Transactional
    public void deleteChat(User user, Long chatId) {
        Chat chat = chatRepository.findByIdAndUser(chatId, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없습니다: " + chatId));
        chatRepository.delete(chat);
    }

    private String createChatTitle(String content) {
        return content.length() > CHAT_TITLE_MAX_LENGTH
                ? content.substring(0, CHAT_TITLE_MAX_LENGTH) + "..."
                : content;
    }

    private ChatDto.ChatResponse toChatResponse(Chat chat) {
        return new ChatDto.ChatResponse(chat.getId(), chat.getTitle(), chat.getCreatedAt());
    }

    private ChatDto.ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        return new ChatDto.ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
