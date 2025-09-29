package com.dorandoran.modules.chat.entity;

import com.dorandoran.modules.user.entity.DoranUser;
import com.dorandoran.shared.enums.MessageType;
import com.dorandoran.shared.enums.SenderType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "message")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(MessageId.class)
public class Message {

  @Id
  @GeneratedValue(generator = "uuid2")
  @Column(name = "messege_id", columnDefinition = "uuid", nullable = false)
  private UUID messageId;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private Chatroom chatroom;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bot_id", nullable = false)
  private Chatbot bot;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private DoranUser user;

  @Column(name = "content", length = 100, nullable = false)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "sender_type", nullable = false)
  private SenderType senderType;

  @Column(name = "chat_num", nullable = false)
  private Integer chatNum;

  @Column(name = "message_send_time", nullable = false)
  private LocalDateTime messageSendTime;

  @Enumerated(EnumType.STRING)
  @Column(name = "message_type", length = 10, nullable = false)
  private MessageType messageType;

  @Type(JsonBinaryType.class)
  @Column(name = "message_meta", columnDefinition = "jsonb")
  private Map<String, Object> messageMeta;

  @PrePersist
  public void prePersist() {
    if (this.messageSendTime == null) {
      this.messageSendTime = LocalDateTime.now();
    }
  }
}