package com.dorandoran.modules.user.repository;

import com.dorandoran.modules.user.entity.DoranUser;
import com.dorandoran.shared.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<DoranUser, UUID> {

  // 이메일로 사용자 조회
  Optional<DoranUser> findByEmail(String email);

  // 이메일 중복 확인
  boolean existsByEmail(String email);

  // 활성 사용자 조회
  Optional<DoranUser> findByEmailAndActive(String email, UserStatus active);

  // 마지막 접속 시간 업데이트
  @Modifying
  @Query("UPDATE DoranUser u SET u.lastConTime = :time WHERE u.userId = :userId")
  void updateLastConnectionTime(@Param("userId") UUID userId, @Param("time") LocalDateTime time);

  // 코치마크 상태 업데이트
  @Modifying
  @Query("UPDATE DoranUser u SET u.coachCheck = :status WHERE u.userId = :userId")
  void updateCoachCheckStatus(@Param("userId") UUID userId, @Param("status") Boolean status);

  // 사용자 상태 업데이트
  @Modifying
  @Query("UPDATE DoranUser u SET u.active = :status WHERE u.userId = :userId")
  void updateUserStatus(@Param("userId") UUID userId, @Param("status") UserStatus status);
}