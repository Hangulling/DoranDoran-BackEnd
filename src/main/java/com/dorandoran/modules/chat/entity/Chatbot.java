package com.dorandoran.modules.chat.entity;

import com.dorandoran.shared.database.entity.BaseTimeEntity;
import com.dorandoran.shared.enums.BotType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "chatbot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chatbot extends BaseTimeEntity {
  @Id
  @GeneratedValue(generator = "uuid2")
  @Column(name = "bot_id", columnDefinition = "uuid", nullable = false)
  private UUID botId;

  @Enumerated(EnumType.STRING)
  @Column(name = "bot_type", length = 50, nullable = false)
  private BotType botType;

  @Column(name = "intimacy", nullable = false)
  private Integer intimacy;

  @Column(name = "bot_img_url", nullable = false)
  private String botImgUrl;
}
