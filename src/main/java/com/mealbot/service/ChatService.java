package com.mealbot.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String DEFAULT_CHAT_TITLE = "새 채팅";
    private static final int CHAT_TITLE_MAX_LENGTH = 20;

    /** v0.3 명세: 최근 메시지 6개 (왕복 3회 = user 3 + assistant 3) */
    private static final int HISTORY_WINDOW_SIZE = 6;

    /** freeText 누적 시 사용할 구분자 (공백). retrieval query에 자연어로 들어감. */
    private static final String FREE_TEXT_DELIMITER = " ";

    /** meal_times List ↔ String 변환 구분자 (콤마). DB 저장용. */
    private static final String MEAL_TIMES_DELIMITER = ",";

    /** AI 호출 실패 시 사용자에게 보여줄 fallback 응답. */
    private static final String AI_FAILURE_ANSWER = "일시적으로 응답이 어렵습니다. 잠시 후 다시 시도해 주세요.";

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

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

        return new ChatDto.ChatDetailResponse(chat.getId(), chat.getTitle(), chat.getCreatedAt().atOffset(KST), messages);
    }

    /**
     * 로그인 사용자 메시지 전송 + v0.3 chat orchestration.
     *
     * 흐름:
     *  1. Chat 로드 + 권한 확인
     *  2. history 로드 (이전 턴까지만 포함)
     *  3. 직전 assistant 메시지에서 last_recommendations 추출
     *  4. 현재 슬롯 상태 → AiDto.Slots
     *  5. 제목 갱신 + user 메시지 저장 (turn_id 발급)
     *  6. AiDto.Request 조립
     *  7. aiClient.chat() 호출 (실패 시 buildAiFailureResponse로 graceful fallback)
     *  8. 응답 반영: Chat 슬롯 업데이트 + assistant 메시지 저장 + SendResponse 반환
     *
     * v0.3 정책 메모:
     *  - free_text 누적은 v0.3 단계에서 적용 안 함. AI 서버가 받은 그대로 돌려주는 값을 그대로 덮어씀.
     *  - session_id = chat PK, turn_id = user 메시지 PK (디버깅 + 로깅 추적용)
     *  - AI 호출 실패 시: user 메시지는 보존, assistant fallback 메시지 저장, isFallback=true 응답.
     */
    @Transactional
    public ChatDto.SendResponse send(User user, Long chatId, ChatDto.SendRequest request) {
        // 1. Chat 로드 + 권한 확인
        Chat chat = chatRepository.findByIdAndUser(chatId, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없습니다: " + chatId));

        String userMessage = request.getContent();

        // 2. 슬라이딩 윈도우 history 로드 (user 메시지 저장 전, 이전 턴까지만 포함)
        List<AiDto.Message> history = loadRecentHistory(chat);

        // 3. 직전 assistant 메시지에서 last_recommendations 추출
        List<AiDto.LastRecommendation> lastRecommendations = loadLastRecommendations(chat);

        // 4. 현재 슬롯 상태 → AiDto.Slots
        AiDto.Slots slots = toSlots(chat);

        // 5. 제목 갱신 + user 메시지 저장 (turn_id 발급용 PK 필요)
        if (DEFAULT_CHAT_TITLE.equals(chat.getTitle())) {
            chat.setTitle(createChatTitle(userMessage));
        }
        ChatMessage savedUser = chatMessageRepository.save(ChatMessage.builder()
                .chat(chat)
                .role(ChatMessage.ROLE_USER)
                .content(userMessage)
                .build());

        // 6. AiDto.Request 조립
        String sessionId = chat.getId().toString();
        String turnId = savedUser.getId().toString();

        AiDto.Request aiRequest = new AiDto.Request(
                sessionId,
                turnId,
                userMessage,
                history,
                slots,
                lastRecommendations
        );

        log.info("AI 호출: session_id={} turn_id={} message='{}'", sessionId, turnId, userMessage);

        // 7. AI 호출 (실패 시 graceful fallback)
        AiDto.Response aiResponse;
        try {
            aiResponse = aiClient.chat(aiRequest);
            log.info("AI 응답: intent={} recommendations={} flags={}",
                    aiResponse.intent(), aiResponse.recommendations().size(), aiResponse.flags());
        } catch (Exception e) {
            log.error("AI 호출 실패. session_id={} turn_id={} error={}",
                    sessionId, turnId, e.getMessage(), e);
            return buildAiFailureResponse(chat);
        }

        // 8. 응답 반영
        // 8-1. Chat 슬롯 업데이트 (v0.3: AI 응답을 그대로 덮어쓰기)
        AiDto.Slots updatedSlots = aiResponse.slotsUpdated();
        chat.setMealTimes(formatMealTimes(updatedSlots.mealTimes()));
        chat.setPurpose(updatedSlots.purpose());
        chat.setFreeText(updatedSlots.freeText());

        // 8-2. assistant 메시지 저장 (content=answer, recommendations_json=전체 저장)
        String recommendationsJson = serializeRecommendations(aiResponse.recommendations());

        ChatMessage savedAssistant = chatMessageRepository.save(ChatMessage.builder()
                .chat(chat)
                .role(ChatMessage.ROLE_ASSISTANT)
                .content(aiResponse.answer())
                .recommendationsJson(recommendationsJson)
                .build());

        // 8-3. SendResponse 조립
        List<ChatDto.Recommendation> clientRecommendations = aiResponse.recommendations().stream()
                .map(r -> new ChatDto.Recommendation(
                        r.recipeId(),
                        r.name(),
                        r.cookingTime(),
                        r.summary(),
                        r.mainIngredients(),
                        r.reason()
                ))
                .toList();

        ChatDto.Flags clientFlags = new ChatDto.Flags(
                aiResponse.flags().needsMoreSlots(),
                aiResponse.flags().outOfScope(),
                aiResponse.flags().isFallback()
        );

        return new ChatDto.SendResponse(
                savedAssistant.getId(),
                aiResponse.intent(),
                aiResponse.answer(),
                clientRecommendations,
                clientFlags
        );
    }

    /** 채팅 세션과 하위 메시지를 모두 삭제한다. */
    @Transactional
    public void deleteChat(User user, Long chatId) {
        Chat chat = chatRepository.findByIdAndUser(chatId, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없습니다: " + chatId));
        chatRepository.delete(chat);
    }

    // ── 게스트 모드 (v0.3 비활성화, 추후 보완 예정) ────────────────

    // public ChatDto.GuestSendResponse sendGuest(ChatDto.GuestSendRequest request) {
    //     List<ChatDto.ChatMessageRequest> incoming = request.getMessages();
    //     String query = incoming.getLast().getContent();
    //     List<AiDto.MessageDto> history = incoming.stream()
    //             .skip(Math.max(0, incoming.size() - HISTORY_WINDOW_SIZE))
    //             .map(m -> new AiDto.MessageDto(m.getRole(), m.getContent()))
    //             .toList();
    //     AiDto.Response aiResponse = aiClient.ask(query, history);
    //     return new ChatDto.GuestSendResponse(aiResponse.getAnswer(), aiResponse.getResults());
    // }

    private String createChatTitle(String content) {
        return content.length() > CHAT_TITLE_MAX_LENGTH
                ? content.substring(0, CHAT_TITLE_MAX_LENGTH) + "..."
                : content;
    }

    private static final ZoneOffset KST = ZoneOffset.UTC;

    private ChatDto.ChatResponse toChatResponse(Chat chat) {
        return new ChatDto.ChatResponse(chat.getId(), chat.getTitle(), chat.getCreatedAt().atOffset(KST));
    }

    private ChatDto.ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        List<ChatDto.Recommendation> recommendations = deserializeRecommendations(message.getRecommendationsJson())
                .stream()
                .map(r -> new ChatDto.Recommendation(r.recipeId(), r.name(), r.cookingTime(), r.summary(), r.mainIngredients(), r.reason()))
                .toList();
        return new ChatDto.ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt().atOffset(KST),
                recommendations
        );
    }

    // ── v0.3 fallback 헬퍼 ──────────────────────────────────────

    private ChatDto.SendResponse buildAiFailureResponse(Chat chat) {
        ChatMessage savedAssistant = chatMessageRepository.save(ChatMessage.builder()
                .chat(chat)
                .role(ChatMessage.ROLE_ASSISTANT)
                .content(AI_FAILURE_ANSWER)
                .recommendationsJson(null)
                .build());

        ChatDto.Flags fallbackFlags = new ChatDto.Flags(false, false, true);

        return new ChatDto.SendResponse(
                savedAssistant.getId(),
                "ask",
                AI_FAILURE_ANSWER,
                List.of(),
                fallbackFlags
        );
    }

    // ── v0.3 변환 헬퍼 ──────────────────────────────────────────

    private AiDto.Slots toSlots(Chat chat) {
        return new AiDto.Slots(
                parseMealTimes(chat.getMealTimes()),
                chat.getPurpose(),
                chat.getFreeText()
        );
    }

    private List<String> parseMealTimes(String mealTimesCsv) {
        if (mealTimesCsv == null || mealTimesCsv.isBlank()) {
            return null;
        }
        return List.of(mealTimesCsv.split(MEAL_TIMES_DELIMITER));
    }

    private String formatMealTimes(List<String> mealTimes) {
        if (mealTimes == null || mealTimes.isEmpty()) {
            return null;
        }
        return String.join(MEAL_TIMES_DELIMITER, mealTimes);
    }

    private String appendFreeText(String existing, String delta) {
        if (delta == null || delta.isBlank()) {
            return existing;
        }
        if (existing == null || existing.isBlank()) {
            return delta;
        }
        return existing + FREE_TEXT_DELIMITER + delta;
    }

    private String serializeLastRecommendations(List<AiDto.LastRecommendation> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (tools.jackson.core.JacksonException e) {
            throw new IllegalStateException("lastRecommendations 직렬화 실패", e);
        }
    }

    private List<AiDto.LastRecommendation> deserializeLastRecommendations(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<AiDto.LastRecommendation>>() {}
            );
        } catch (tools.jackson.core.JacksonException e) {
            log.warn("lastRecommendations 역직렬화 실패, 빈 리스트로 처리. json={}", json, e);
            return List.of();
        }
    }

    private List<AiDto.Message> loadRecentHistory(Chat chat) {
        return chatMessageRepository
                .findTop6ByChatOrderByCreatedAtDesc(chat)
                .stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .map(m -> new AiDto.Message(m.getRole(), m.getContent()))
                .toList();
    }

    private List<AiDto.LastRecommendation> loadLastRecommendations(Chat chat) {
        return chatMessageRepository
                .findTop6ByChatOrderByCreatedAtDesc(chat)
                .stream()
                .filter(m -> ChatMessage.ROLE_ASSISTANT.equals(m.getRole()))
                .findFirst()
                .map(m -> deserializeRecommendations(m.getRecommendationsJson()).stream()
                        .map(r -> new AiDto.LastRecommendation(r.recipeId(), r.name()))
                        .toList())
                .orElse(List.of());
    }

    private String serializeRecommendations(List<AiDto.Recommendation> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (tools.jackson.core.JacksonException e) {
            throw new IllegalStateException("recommendations 직렬화 실패", e);
        }
    }

    private List<AiDto.Recommendation> deserializeRecommendations(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<AiDto.Recommendation>>() {});
        } catch (tools.jackson.core.JacksonException e) {
            log.warn("recommendations 역직렬화 실패, 빈 리스트로 처리. json={}", json, e);
            return List.of();
        }
    }
}