package com.dorandoran.chat.entity.billing;

import com.dorandoran.chat.entity.User;
import com.dorandoran.chat.entity.ChatRoom;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_usage_events", schema = "billing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageEvent {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "event_time", nullable = false)
    private OffsetDateTime eventTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "input_tokens", nullable = false)
    private Integer inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private Integer outputTokens;

    @Column(name = "cost_in", nullable = false)
    private Double costIn;

    @Column(name = "cost_out", nullable = false)
    private Double costOut;
}