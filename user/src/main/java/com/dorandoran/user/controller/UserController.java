package com.dorandoran.user.controller;

import com.dorandoran.shared.dto.CreateUserRequest;
import com.dorandoran.shared.dto.UpdateUserRequest;
import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.shared.dto.ResetPasswordRequest;
import com.dorandoran.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User 서비스 REST API
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    /**
     * 사용자 생성
     */
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request) {
        log.info("사용자 생성 요청: email={}", request.email());
        
        try {
            UserDto createdUser = userService.createUser(request);
            return ResponseEntity.ok(createdUser);
        } catch (Exception e) {
            log.error("사용자 생성 실패: email={}, error={}", request.email(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 사용자 ID로 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        log.info("사용자 조회 요청: userId={}", userId);
        
        try {
            UserDto user = userService.findById(UUID.fromString(userId));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("사용자 조회 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 이메일로 사용자 조회
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        log.info("이메일로 사용자 조회 요청: email={}", email);
        
        try {
            UserDto user = userService.findByEmail(email);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("이메일로 사용자 조회 실패: email={}, error={}", email, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 사용자 프로필 업데이트
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable String userId,
            @RequestBody UpdateUserRequest request) {
        log.info("사용자 업데이트 요청: userId={}", userId);
        
        try {
            UserDto updatedUser = userService.updateUser(UUID.fromString(userId), request);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 사용자 ID: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("사용자 업데이트 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 사용자 상태 업데이트
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserDto> updateUserStatus(
            @PathVariable String userId,
            @RequestParam String status) {
        log.info("사용자 상태 업데이트 요청: userId={}, status={}", userId, status);
        
        try {
            UserDto.UserStatus userStatus = UserDto.UserStatus.valueOf(status.toUpperCase());
            UserDto updatedUser = userService.updateUserStatus(UUID.fromString(userId), userStatus);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 사용자 ID 또는 상태: userId={}, status={}, error={}", userId, status, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("사용자 상태 업데이트 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User service is running");
    }

    /**
     * 비밀번호 재설정 (이메일 기준)
     */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        log.info("비밀번호 재설정 요청: email={}", request.getEmail());
        try {
            userService.resetPasswordByEmail(request.getEmail(), request.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("비밀번호 재설정 실패: email={}, error={}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 사용자 비밀번호 업데이트
     */
    @PutMapping("/{userId}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable String userId,
            @RequestBody String newPassword) {
        log.info("사용자 비밀번호 업데이트 요청: userId={}", userId);
        
        try {
            userService.updatePassword(UUID.fromString(userId), newPassword);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("잘못된 사용자 ID: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("비밀번호 업데이트 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 회원탈퇴 (소프트 삭제 - 상태를 INACTIVE로 변경)
     * 실제 삭제는 배치 작업에서 처리 예정
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.info("회원탈퇴 요청: userId={}", userId);
        
        try {
            userService.deleteUser(UUID.fromString(userId));
            log.info("회원탈퇴 완료: userId={}", userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("잘못된 사용자 ID: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("회원탈퇴 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
