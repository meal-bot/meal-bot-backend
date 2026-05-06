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

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiClient aiClient;

    @Transactional
    public ChatDto.ChatResponse createChat(User user) {
        Chat chat = Chat.builder()
                .user(user)
                .title(DEFAULT_CHAT_TITLE)
                .build();
        chatRepository.save(chat);
        return toChatResponse(chat);
    }

    public List<ChatDto.ChatResponse> getChats(User user) {
        return chatRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toChatResponse)
                .toList();
    }

    public ChatDto.ChatDetailResponse getChat(User user, Long chatId) {
        Chat chat = chatRepository.findByIdAndUser(chatId, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없습니다: " + chatId));

        List<ChatDto.ChatMessageResponse> messages = chatMessageRepository
                .findByChatOrderByCreatedAt(chat).stream()
                .map(this::toChatMessageResponse)
                .toList();

        return new ChatDto.ChatDetailResponse(chat.getId(), chat.getTitle(), chat.getCreatedAt(), messages);
    }

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

        AiDto.Response aiResponse = aiClient.ask(userMessage);

        ChatMessage reply = chatMessageRepository.save(
                ChatMessage.builder()
                        .chat(chat).role(ChatMessage.ROLE_ASSISTANT).content(aiResponse.getAnswer()).build());

        return new ChatDto.SendResponse(reply.getId(), aiResponse.getAnswer(), aiResponse.getResults());
    }

    public ChatDto.GuestSendResponse sendGuest(ChatDto.GuestSendRequest request) {
        String query = request.getMessages().getLast().getContent();
        AiDto.Response aiResponse = aiClient.ask(query);
        return new ChatDto.GuestSendResponse(aiResponse.getAnswer(), aiResponse.getResults());
    }

    // AI 도입 전 에코 패턴용 — send()가 유저 메시지 저장 + AI 호출 + 응답 저장을 한 번에 처리하므로 중복
    // @Transactional
    // public ChatDto.ChatMessageResponse addChatMessage(User user, Long chatId, ChatDto.ChatMessageRequest request) { ... }

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
