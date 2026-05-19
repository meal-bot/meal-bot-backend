package com.mealbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mealbot.dto.CalendarDto;
import com.mealbot.entity.ChatMessage;
import com.mealbot.entity.User;
import com.mealbot.repository.ChatMessageRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public List<CalendarDto.Response> getCalendar(User user, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<ChatMessage> messages = chatMessageRepository
                .findByChatUserAndRoleAndRecommendationsJsonIsNotNullAndCreatedAtBetweenOrderByCreatedAtDesc(
                        user, ChatMessage.ROLE_ASSISTANT, start, end);

        // 채팅별 마지막 추천 메시지 1개씩 추출 (DESC 정렬이므로 첫 번째 = 최신)
        Map<Long, ChatMessage> lastPerChat = messages.stream()
                .collect(Collectors.toMap(
                        m -> m.getChat().getId(),
                        m -> m,
                        (existing, replacement) -> existing
                ));

        return lastPerChat.values().stream()
                .map(m -> new CalendarDto.Response(
                        m.getChat().getId(),
                        m.getChat().getTitle(),
                        m.getContent(),
                        m.getCreatedAt()
                ))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }
}
