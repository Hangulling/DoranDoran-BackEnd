package com.dorandoran.auth.entity;

import com.dorandoran.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType; // LOGIN, LOGOUT, TOKEN_ROTATE, PASSWORD_CHANGE 등

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // 간단히 JSON 문자열 보관

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}


