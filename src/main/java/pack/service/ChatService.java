package pack.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pack.dto.AiDto;
import pack.dto.ChatDto;
import pack.entity.Chat;
import pack.entity.ChatMessage;
import pack.entity.User;
import pack.repository.ChatMessageRepository;
import pack.repository.ChatRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiClient aiClient;

    @Transactional
    public ChatDto.ChatResponse createChat(User user) {
        Chat chat = Chat.builder()
                .user(user)
                .title("새 채팅")
                .build();
        chatRepository.save(chat);
        return new ChatDto.ChatResponse(chat.getId(), chat.getTitle(), chat.getCreatedAt());
    }

    public List<ChatDto.ChatResponse> getChats(User user) {
        return chatRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(c -> new ChatDto.ChatResponse(c.getId(), c.getTitle(), c.getCreatedAt()))
                .toList();
    }

    public ChatDto.ChatDetailResponse getChat(User user, Long chatId) {
        Chat chat = chatRepository.findByIdAndUser(chatId, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없습니다: " + chatId));

        List<ChatDto.ChatMessageResponse> messages = chatMessageRepository
                .findByChatOrderByCreatedAt(chat).stream()
                .map(m -> new ChatDto.ChatMessageResponse(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();

        return new ChatDto.ChatDetailResponse(chat.getId(), chat.getTitle(), chat.getCreatedAt(), messages);
    }

    @Transactional
    public ChatDto.SendResponse send(User user, Long chatId, ChatDto.SendRequest request) {
        Chat chat = chatRepository.findByIdAndUser(chatId, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없습니다: " + chatId));

        if ("새 채팅".equals(chat.getTitle())) {
            String title = request.getContent().length() > 20
                    ? request.getContent().substring(0, 20) + "..."
                    : request.getContent();
            chat.setTitle(title);
        }

        chatMessageRepository.save(ChatMessage.builder()
                .chat(chat).role("user").content(request.getContent()).build());

        AiDto.Response aiResponse = aiClient.ask(request.getContent());

        ChatMessage reply = chatMessageRepository.save(
                ChatMessage.builder()
                        .chat(chat).role("assistant").content(aiResponse.getAnswer()).build());

        return new ChatDto.SendResponse(reply.getId(), aiResponse.getAnswer(), aiResponse.getResults());
    }

    public ChatDto.GuestSendResponse sendGuest(ChatDto.GuestSendRequest request) {
        String query = request.getMessages().getLast().getContent();
        AiDto.Response aiResponse = aiClient.ask(query);
        return new ChatDto.GuestSendResponse(aiResponse.getAnswer(), aiResponse.getResults());
    }

    @Transactional
    public ChatDto.ChatMessageResponse addChatMessage(User user, Long chatId, ChatDto.ChatMessageRequest request) {
        Chat chat = chatRepository.findByIdAndUser(chatId, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없습니다: " + chatId));

        if ("user".equals(request.getRole()) && "새 채팅".equals(chat.getTitle())) {
            String title = request.getContent().length() > 20
                    ? request.getContent().substring(0, 20) + "..."
                    : request.getContent();
            chat.setTitle(title);
        }

        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .chat(chat)
                .role(request.getRole())
                .content(request.getContent())
                .build());

        return new ChatDto.ChatMessageResponse(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
    }

    @Transactional
    public void deleteChat(User user, Long chatId) {
        Chat chat = chatRepository.findByIdAndUser(chatId, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없습니다: " + chatId));
        chatRepository.delete(chat);
    }
}