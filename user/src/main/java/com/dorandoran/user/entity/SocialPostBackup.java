package com.dorandoran.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 소셜 플랫폼(Instagram/Facebook) 자동동기화 백업 테이블
 */
@Entity
@Table(name = "social_posts_backup", schema = "user_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialPostBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_domain", nullable = false, length = 20)
    private String sourceDomain;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "permalink", columnDefinition = "TEXT")
    private String permalink;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "media_type", length = 20)
    private String mediaType;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assets", columnDefinition = "jsonb")
    private String assets;
}
