package com.dorandoran.user.controller;

import com.dorandoran.shared.dto.CreateUserRequest;
import com.dorandoran.shared.dto.UpdateUserRequest;
import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.shared.dto.ResetPasswordRequest;
import com.dorandoran.user.service.UserService;
import com.dorandoran.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "User Management", description = "사용자 관리 API")
public class UserController {
    
    private final UserService userService;
    
    /**
     * 사용자 생성
     */
    @Operation(summary = "사용자 생성", description = "새로운 사용자를 등록합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody CreateUserRequest request) {
        log.info("사용자 생성 요청: email={}", request.email());
        
        try {
            UserDto createdUser = userService.createUser(request);
            return ResponseEntity.ok(ApiResponse.success(createdUser, "사용자가 성공적으로 생성되었습니다."));
        } catch (Exception e) {
            log.error("사용자 생성 실패: email={}, error={}", request.email(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자 생성에 실패했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 사용자 ID로 조회
     */
    @Operation(summary = "사용자 조회 (ID)", description = "사용자 ID로 사용자 정보를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(
            @Parameter(description = "사용자 UUID", required = true)
            @PathVariable String userId) {
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
     * 현재 사용자 정보 조회 (인증된 사용자)
     */
    @Operation(summary = "현재 사용자 정보 조회", description = "인증된 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @RequestHeader("Authorization") String token) {
        log.info("현재 사용자 정보 조회 API 호출");
        
        try {
            // JWT 토큰에서 사용자 ID 추출 (간단한 구현)
            // 실제로는 JWT 서비스에서 토큰을 검증하고 사용자 정보를 반환해야 함
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            // 임시로 첫 번째 사용자를 반환 (실제로는 JWT에서 사용자 ID를 추출해야 함)
            // TODO: JWT 토큰 검증 및 사용자 ID 추출 로직 구현
            UserDto user = userService.findById(UUID.randomUUID()); // 임시 구현
            return ResponseEntity.ok(ApiResponse.success(user, "사용자 정보를 성공적으로 조회했습니다."));
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("사용자 정보 조회에 실패했습니다: " + e.getMessage()));
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
