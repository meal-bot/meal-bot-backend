package com.mealbot.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * [codex] 게스트 임시 채팅 처리 실패를 표현하는 예외.
 * 오류 코드는 프론트에 전달하며, clearCookie는 더 이상 사용할 수 없는 게스트 채팅 연결을 제거한다.
 */
@Getter
public class GuestChatException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final boolean clearCookie;

    public GuestChatException(HttpStatus status, String code, String message, boolean clearCookie) {
        super(message);
        this.status = status;
        this.code = code;
        this.clearCookie = clearCookie;
    }
}