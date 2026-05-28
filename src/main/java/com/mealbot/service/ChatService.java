package com.mealbot.service;

import com.mealbot.util.RecommendationUtils;
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

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String DEFAULT_CHAT_TITLE = "새 채팅";
    private static final int CHAT_TITLE_MAX_LENGTH = 20;

    /** 슬라이딩 윈도우: 최근 메시지 50개 (왕복 25회 = user 25 + assistant 25) */
    private static final int HISTORY_WINDOW_SIZE = 50;

    /** freeText 누적 시 사용할 구분자 (공백). retrieval query에 자연어로 들어감. */
    private static final String FREE_TEXT_DELIMITER = " ";

    /** meal_times List ↔ String 변환 구분자 (콤마). DB 저장용. */
    private static final String MEAL_TIMES_DELIMITER = ",";

    /** AI 호출 실패 시 사용자에게 보여줄 fallback 응답. */
    private static final String AI_FAILURE_ANSWER = "일시적으로 응답이 어렵습니다. 잠시 후 다시 시도해 주세요.";

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiClient aiClient;

    /** 새 채팅을 생성하고 기본 제목으로 저장한다. */
    @Transactional
    public ChatDto.ChatResponse createChat(User user) {
        Chat chat = Chat.builder()
                .user(user)
                .title(DEFAULT_CHAT_TITLE)
                .build();
        chatRepository.save(chat);
        return toChatResponse(chat);
    }

    /** 사용자의 채팅 목록을 최신순으로 반환한다. */
    public List<ChatDto.ChatResponse> getChats(User user) {
        return chatRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map((chat) -> this.toChatResponse(chat))
                .toList();
    }

    /** 특정 채팅의 전체 메시지 히스토리를 반환한다. */
    public ChatDto.ChatDetailResponse getChat(User user, Long chatId) {
        Chat chat = chatRepository.findByIdAndUser(chatId, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없습니다: " + chatId));

        List<ChatDto.ChatMessageResponse> messages = chatMessageRepository
                .findByChatOrderByCreatedAt(chat).stream()
                .map(this::toChatMessageResponse)
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
     *  - free_text 누적: recommend/refine intent일 때만 AI 응답 delta를 기존 freeText에 append.
     *    ask/slot_fill intent는 freeText 변경 없음 (QA 질문이 추천 조건을 오염하지 않도록).
     *  - session_id = chat PK, turn_id = user 메시지 PK (디버깅 + 로깅 추적용)
     *  - AI 호출 실패 시: user 메시지는 보존, assistant fallback 메시지 저장, isFallback=true 응답.
     */
    @Transactional
    public ChatDto.SendResponse send(User user, Long chatId, ChatDto.SendRequest request) {
        // 1. Chat 로드 + 권한 확인
        Chat chat = chatRepository.findByIdAndUser(chatId, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없습니다: " + chatId));

        return processMessage(chat, request, chat.getId().toString());
    }

    /**
     * [codex] 접근 권한이 이미 확인된 채팅의 단일 메시지를 저장하고 처리한다.
     * 사용자 인증 및 게스트 채팅 소유권 확인은 호출 서비스가 담당하며,
     * 로그인/게스트 모두 동일한 AI 히스토리·슬롯·추천 처리 흐름을 사용한다.
     */
    ChatDto.SendResponse processMessage(Chat chat, ChatDto.SendRequest request, String sessionId) {
        String userMessage = request.getContent();

        // 2. 슬라이딩 윈도우 history 로드 (최근 50개, user 메시지 저장 전 이전 턴까지만 포함)
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
        String turnId = savedUser.getId().toString();

        AiDto.Request aiRequest = new AiDto.Request(
                sessionId,
                turnId,
                userMessage,
                history,
                slots,
                lastRecommendations
        );

        log.info("AI 호출: session_id={} turn_id={} historySize={} message='{}'",
                sessionId, turnId, history.size(), userMessage);

        // 7. AI 호출 (실패 시 graceful fallback)
        AiDto.Response aiResponse;
        try {
            aiResponse = aiClient.chat(aiRequest);
            log.info("AI 응답: intent={} recommendations={} flags={} freeTextDelta='{}'",
                    aiResponse.intent(), aiResponse.recommendations().size(), aiResponse.flags(),
                    aiResponse.freeTextDelta());
        } catch (Exception e) {
            log.error("AI 호출 실패. session_id={} turn_id={} error={}",
                    sessionId, turnId, e.getMessage(), e);
            return buildAiFailureResponse(chat);
        }

        // 8. 응답 반영
        // 8-1. Chat 슬롯 업데이트
        AiDto.Slots updatedSlots = aiResponse.slotsUpdated();
        chat.setMealTimes(formatMealTimes(updatedSlots.mealTimes()));
        chat.setPurpose(updatedSlots.purpose());
        // slots_updated.free_text는 AI 서버가 echo하는 값이므로 사용하지 않음.
        // 이번 턴 새 조각은 free_text_delta 별도 필드로 수신.
        // slot_fill 포함: 시간대/스타일 답하기 전 자유조건을 먼저 말하는 경우 슬롯 단계에서 버려지지 않도록.
        // refine은 덮어쓰기 정책 확정 후 별도 추가.
        String intent = aiResponse.intent();
        if ("recommend".equals(intent) || "slot_fill".equals(intent)) {
            chat.setFreeText(appendFreeText(chat.getFreeText(), aiResponse.freeTextDelta()));
        }

        // 8-2. assistant 메시지 저장 (content=answer, recommendations_json=전체 저장)
        String recommendationsJson = RecommendationUtils.serialize(aiResponse.recommendations());

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
                aiResponse.flags().isFallback(),
                aiResponse.flags().refused()  // [파트너 요청] refused 필드 추가
        );

        return new ChatDto.SendResponse(
                savedAssistant.getId(),
                aiResponse.intent(),
                aiResponse.answer(),
                clientRecommendations,
                clientFlags
        );
    }

    /** 채팅과 하위 메시지를 모두 삭제한다. */
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

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    private ChatDto.ChatResponse toChatResponse(Chat chat) {
        return new ChatDto.ChatResponse(chat.getId(), chat.getTitle(), chat.getCreatedAt().atOffset(KST));
    }

    private ChatDto.ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        List<ChatDto.Recommendation> recommendations = RecommendationUtils.deserialize(message.getRecommendationsJson())
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

        ChatDto.Flags fallbackFlags = new ChatDto.Flags(false, false, true, false);

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

    private List<AiDto.Message> loadRecentHistory(Chat chat) {
        return chatMessageRepository
                .findTop50ByChatOrderByCreatedAtDesc(chat)
                .stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .map(m -> new AiDto.Message(m.getRole(), m.getContent()))
                .toList();
    }

    private List<AiDto.LastRecommendation> loadLastRecommendations(Chat chat) {
        return chatMessageRepository
                .findTop50ByChatOrderByCreatedAtDesc(chat)
                .stream()
                .filter(m -> ChatMessage.ROLE_ASSISTANT.equals(m.getRole()))
                .findFirst()
                .map(m -> RecommendationUtils.deserialize(m.getRecommendationsJson()).stream()
                        .map(r -> new AiDto.LastRecommendation(r.recipeId(), r.name()))
                        .toList())
                .orElse(List.of());
    }

}
