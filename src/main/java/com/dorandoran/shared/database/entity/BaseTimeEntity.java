package com.dorandoran.shared.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
  @CreatedDate
  @Column(name = "create_at", nullable = false, updatable = false)
  private LocalDateTime createAt;

  @LastModifiedDate
  @Column(name = "update_at")
  private LocalDateTime updateAt;
}
