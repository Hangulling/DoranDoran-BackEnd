package com.dorandoran.modules.user.entity;

import com.dorandoran.shared.database.entity.BaseTimeEntity;
import com.dorandoran.shared.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "doran_user")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoranUser extends BaseTimeEntity {
public class DoranUser {
  @Id
  @GeneratedValue(generator = "uuid2")
  @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
  private UUID userId;

  @Column(name = "email", length = 50, nullable = false, unique = true)
  private String email;

  @Column(name = "name", length = 50, nullable = false)
  private String name;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "picture")
  private String picture;

  @Column(name = "info", length = 100, nullable = false)
  private String info;

  @Column(name = "last_con_time", nullable = false)
  private LocalDateTime lastConTime;

  @Column(name = "first_name", length = 50, nullable = false)
  private String firstName;

  @Column(name = "last_name", length = 50, nullable = false)
  private String lastName;

  @Enumerated(EnumType.STRING)
  @Column(name = "active", nullable = false)
  @ColumnDefault("'active'")
  private UserStatus active = UserStatus.ACTIVE;

  @Column(name = "coach_check")
  @ColumnDefault("false")
  private Boolean coachCheck = false;

  @PrePersist
  public void prePersist() {
    if (this.lastConTime == null) {
      this.lastConTime = LocalDateTime.now();
    }
    if (this.info == null) {
      this.info = "";
    }
  }
}
