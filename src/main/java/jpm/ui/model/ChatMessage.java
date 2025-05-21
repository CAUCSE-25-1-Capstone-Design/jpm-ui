package jpm.ui.model;

import java.time.LocalDateTime;

/**
 * 채팅 메시지를 나타내는 모델 클래스
 * 메시지 내용, 타입, 시간 정보를 포함합니다.
 */
public class ChatMessage {

    /**
     * 메시지 타입 열거형
     * USER: 사용자 메시지
     * JPM: JPM 응답 메시지
     * SYSTEM: 시스템 메시지 (환영 메시지 등)
     */
    public enum MessageType {
        USER,
        JPM,
        SYSTEM,
        TYPING   // 타이핑 인디케이터
    }

    private final String content;        // 메시지 내용
    private final MessageType type;      // 메시지 타입
    private final LocalDateTime timestamp; // 메시지 생성 시간

    /**
     * 메시지 생성자
     *
     * @param content 메시지 내용
     * @param type 메시지 타입 (USER, JPM, SYSTEM)
     */
    public ChatMessage(String content, MessageType type) {
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 메시지 내용 getter
     *
     * @return 메시지 내용
     */
    public String getContent() {
        return content;
    }

    /**
     * 메시지 타입 getter
     *
     * @return 메시지 타입
     */
    public MessageType getType() {
        return type;
    }

    /**
     * 메시지 생성 시간 getter
     *
     * @return 메시지 생성 시간
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * 사용자 메시지 여부 확인
     *
     * @return 사용자 메시지인 경우 true
     */
    public boolean isUserMessage() {
        return type == MessageType.USER;
    }

    /**
     * JPM 메시지 여부 확인
     *
     * @return JPM 메시지인 경우 true
     */
    public boolean isJpmMessage() {
        return type == MessageType.JPM;
    }

    /**
     * 시스템 메시지 여부 확인
     *
     * @return 시스템 메시지인 경우 true
     */
    public boolean isSystemMessage() {
        return type == MessageType.SYSTEM;
    }

    @Override
    public String toString() {
        return "[" + type + "] " + content;
    }
}