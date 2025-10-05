package com.dorandoran.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_events", schema = "auth_schema")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType; // LOGIN, LOGOUT, TOKEN_ROTATE, PASSWORD_CHANGE 등

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // 간단히 JSON 문자열 보관

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}


