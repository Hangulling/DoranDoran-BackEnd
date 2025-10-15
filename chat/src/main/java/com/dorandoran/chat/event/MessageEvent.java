package com.dorandoran.chat.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * 채팅 메시지 이벤트
 * Redis Pub/Sub으로 전송되는 메시지 데이터
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  // 메시지 ID
  private UUID messageId;

  // 채팅방 ID
  private UUID chatroomId;

  // 발신자 ID
  private UUID senderId;

  // 발신자 타입 (user | bot | system)
  private String senderType;

  // 메시지 내용
  private String content;

  // 콘텐츠 타입 (text | code | system | json)
  private String contentType;

  // 타임스탬프 (밀리초)
  private Long timestamp;
}