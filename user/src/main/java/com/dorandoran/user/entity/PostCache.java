package com.dorandoran.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 메인홈 게시글 캐시 (인스타그램 피드)
 */
@Entity
@Table(name = "posts_cache", schema = "user_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCache {

    @Id
    @Column(name = "external_id", length = 100)
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
}
