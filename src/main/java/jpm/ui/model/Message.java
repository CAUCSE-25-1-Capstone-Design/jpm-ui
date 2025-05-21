package jpm.ui.model;

import java.time.LocalDateTime;

/**
 * 채팅 메시지를 나타내는 모델 클래스
 */
public class Message {
    private String content;
    private LocalDateTime timestamp;
    private MessageType type;

    /**
     * 메시지 유형 열거형
     */
    public enum MessageType {
        SENT,       // 사용자가 보낸 메시지
        RECEIVED,   // 다른 사용자로부터 받은 메시지
        TYPING      // 타이핑 인디케이터
    }

    /**
     * 기본 생성자
     */
    public Message() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 메시지 내용과 유형을 지정하는 생성자
     *
     * @param content 메시지 내용
     * @param type 메시지 유형
     */
    public Message(String content, MessageType type) {
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    // Getter와 Setter 메서드
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Message{" +
                "content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                '}';
    }
}