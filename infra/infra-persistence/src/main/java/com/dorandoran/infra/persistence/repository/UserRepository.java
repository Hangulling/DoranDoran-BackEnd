package com.dorandoran.infra.persistence.repository;

import com.dorandoran.infra.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);
    
    /**
     * 이름으로 사용자 검색 (LIKE 검색)
     */
    @Query("SELECT u FROM User u WHERE u.name LIKE %:name% OR u.firstName LIKE %:name% OR u.lastName LIKE %:name%")
    List<User> findByNameContaining(@Param("name") String name);
    
    /**
     * 상태별 사용자 목록 조회
     */
    @Query(value = "SELECT * FROM app_user WHERE status = :status", nativeQuery = true)
    List<User> findByStatus(@Param("status") String status);
    
    /**
     * 최근 연결 시간 기준으로 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.lastConnTime >= :fromDate ORDER BY u.lastConnTime DESC")
    List<User> findUsersByLastConnectionAfter(@Param("fromDate") LocalDateTime fromDate);
}