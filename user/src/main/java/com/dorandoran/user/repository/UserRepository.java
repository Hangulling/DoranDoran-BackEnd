package com.dorandoran.user.repository;

import com.dorandoran.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 Repository (User 서비스)
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * 이메일로 사용자 조회 (대소문자 무시; 조회 전 소문자 정규화 권장)
     */
    Optional<User> findByEmailIgnoreCase(String email);
    
    /**
     * 이메일 존재 여부 확인 (대소문자 무시)
     */
    boolean existsByEmailIgnoreCase(String email);
    
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
    
    /**
     * OAuth 제공자와 OAuth ID로 사용자 조회
     */
    Optional<User> findByOauthProviderAndOauthId(User.OAuthProvider oauthProvider, String oauthId);
    
    /**
     * 개인정보로 사용자 조회 (이메일 찾기용)
     * firstName, lastName, birthDate, signupQuestion, signupAnswer 모두 일치하는 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.firstName = :firstName " +
           "AND u.lastName = :lastName " +
           "AND u.birthDate = :birthDate " +
           "AND u.signupQuestion = :signupQuestion " +
           "AND u.signupAnswer = :signupAnswer")
    Optional<User> findByPersonalInfo(
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("birthDate") LocalDate birthDate,
            @Param("signupQuestion") String signupQuestion,
            @Param("signupAnswer") String signupAnswer
    );
}
