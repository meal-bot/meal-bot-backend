package com.mealbot.service;

import com.mealbot.dto.ChatDto;
import com.mealbot.entity.Chat;
import com.mealbot.repository.ChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** [codex] 게스트 소유권, 활동 기준 만료 연장, AI 처리 재사용, 만료 정리를 검증한다. */
@ExtendWith(MockitoExtension.class)
class GuestChatServiceTests {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private GuestChatService guestChatService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(guestChatService, "ttlMinutes", 60L);
        ReflectionTestUtils.setField(guestChatService, "cookieSecure", false);
    }

    @Test
    void createChatPersistsAnonymousChatWithHashedTokenAndExpiry() {
        GuestChatService.CreateResult createdChat = guestChatService.createChat(null);

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());
        Chat saved = captor.getValue();

        assertThat(saved.getUser()).isNull();
        assertThat(saved.getGuestTokenHash()).hasSize(64).isNotEqualTo(createdChat.token());
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(59));
        assertThat(createdChat.expiresAt()).isNotNull();
    }

    @Test
    void createChatDeletesPreviousGuestChatWhenCookieExists() {
        Chat previous = Chat.builder().build();
        when(chatRepository.findByGuestTokenHashAndUserIsNull(any())).thenReturn(Optional.of(previous));

        guestChatService.createChat("previous-token");

        verify(chatRepository).delete(previous);
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void sendDelegatesToSharedChatProcessingAndExtendsExpiry() {
        Chat chat = Chat.builder()
                .id(42L)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        ChatDto.SendRequest request = new ChatDto.SendRequest("dinner");
        ChatDto.SendResponse response = new ChatDto.SendResponse(
                3L, "ask", "answer", List.of(), new ChatDto.Flags(false, false, false, false));
        when(chatRepository.findByGuestTokenHashAndUserIsNull(any())).thenReturn(Optional.of(chat));
        when(chatService.processMessage(eq(chat), eq(request), eq("guest-42"))).thenReturn(response);

        GuestChatService.MessageResult result = guestChatService.send("token", request);

        assertThat(result.response()).isEqualTo(response);
        assertThat(chat.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(59));
        verify(chatService).processMessage(chat, request, "guest-42");
    }

    @Test
    void expiredChatIsRejectedBeforeAiProcessing() {
        Chat expired = Chat.builder().expiresAt(LocalDateTime.now().minusMinutes(1)).build();
        when(chatRepository.findByGuestTokenHashAndUserIsNull(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> guestChatService.send("token", new ChatDto.SendRequest("hello")))
                .isInstanceOf(GuestChatException.class)
                .extracting("code")
                .isEqualTo("GUEST_CHAT_EXPIRED");

        verify(chatRepository).delete(expired);
        verify(chatService, never()).processMessage(any(), any(), any());
    }

    @Test
    void cleanupDeletesOnlyRepositorySelectedExpiredGuestChats() {
        Chat expired = Chat.builder().build();
        when(chatRepository.findByUserIsNullAndExpiresAtBefore(any())).thenReturn(List.of(expired));

        guestChatService.deleteExpiredChats();

        verify(chatRepository).deleteAll(List.of(expired));
    }
}
