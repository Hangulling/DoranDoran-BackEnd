package com.dorandoran.modules.chat.entity;

import com.dorandoran.modules.user.entity.DoranUser;
import com.dorandoran.shared.database.entity.BaseTimeEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "chatroom")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ChatroomId.class)
public class Chatroom extends BaseTimeEntity {

  @Id
  @GeneratedValue(generator = "uuid2")
  @Column(name = "room_id", columnDefinition = "uuid", nullable = false)
  private UUID roomId;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private DoranUser user;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bot_id", nullable = false)
  private Chatbot bot;

  @Column(name = "room_name", length = 50, nullable = false)
  private String roomName;

  @Column(name = "is_deleted", nullable = false)
  @ColumnDefault("false")
  private Boolean isDeleted = false;

  @Type(JsonBinaryType.class)
  @Column(name = "settings", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> settings;
}