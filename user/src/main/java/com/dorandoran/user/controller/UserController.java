package com.dorandoran.user.controller;

import com.dorandoran.shared.dto.CreateUserRequest;
import com.dorandoran.shared.dto.UpdateUserRequest;
import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.shared.dto.UserWithPasswordDto;
import com.dorandoran.shared.dto.ResetPasswordRequest;
import com.dorandoran.shared.dto.FindEmailRequest;
import com.dorandoran.shared.dto.FindEmailResponse;
import com.dorandoran.user.dto.OnboardingSubmitRequest;
import com.dorandoran.user.service.UserService;
import com.dorandoran.common.response.ApiResponse;
import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("사용자 생성 요청: email={}", request.email());
        
        try {
            UserDto createdUser = userService.createUser(request);
            return ResponseEntity.ok(ApiResponse.success(createdUser, "사용자가 성공적으로 생성되었습니다."));
        } catch (DoranDoranException e) {
            log.warn("사용자 생성 비즈니스 검증 실패: email={}, error={}", request.email(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("사용자 생성 실패: email={}, error={}", request.email(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자 생성 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }
    
    /**
     * 회원가입 (별도 엔드포인트)
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> registerUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("회원가입 요청: email={}", request.email());
        
        try {
            UserDto createdUser = userService.createUser(request);
            return ResponseEntity.ok(ApiResponse.success(createdUser, "회원가입이 성공적으로 완료되었습니다."));
        } catch (DoranDoranException e) {
            log.warn("회원가입 비즈니스 검증 실패: email={}, error={}", request.email(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("회원가입 실패: email={}, error={}", request.email(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("회원가입 처리 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
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
     * OAuth 사용자 조회
     */
    @GetMapping("/oauth/{provider}/{oauthId}")
    public ResponseEntity<UserDto> getUserByOAuth(
            @PathVariable String provider,
            @PathVariable String oauthId) {
        log.info("OAuth 사용자 조회 요청: provider={}, oauthId={}", provider, oauthId);
        
        try {
            UserDto user = userService.findByOAuth(provider, oauthId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("OAuth 사용자 조회 실패: provider={}, oauthId={}, error={}", provider, oauthId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * OAuth 사용자 생성
     */
    @PostMapping("/oauth")
    public ResponseEntity<UserDto> createOAuthUser(
            @RequestParam("email") String email,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("name") String name,
            @RequestParam(value = "picture", required = false) String picture,
            @RequestParam("provider") String provider,
            @RequestParam("oauthId") String oauthId,
            @RequestParam(value = "birthDate", required = false) String birthDate) {
        log.info("OAuth 사용자 생성 요청: email={}, provider={}", email, provider);
        
        try {
            UserDto createdUser = userService.createOAuthUser(email, firstName, lastName, name, picture, provider, oauthId, birthDate);
            return ResponseEntity.ok(createdUser);
        } catch (Exception e) {
            log.error("OAuth 사용자 생성 실패: email={}, provider={}, error={}", email, provider, e.getMessage());
            return ResponseEntity.badRequest().build();
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
     * 이메일로 사용자 조회 (Auth 서비스용 - passwordHash 포함)
     */
    @GetMapping("/auth/email/{email}")
    public ResponseEntity<UserWithPasswordDto> getUserByEmailForAuth(@PathVariable String email) {
        log.info("Auth 서비스용 이메일로 사용자 조회 요청: email={}", email);
        
        try {
            UserWithPasswordDto user = userService.findByEmailForAuth(email);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Auth 서비스용 이메일로 사용자 조회 실패: email={}, error={}", email, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 이메일 중복확인
     */
    @Operation(summary = "이메일 중복확인", description = "이메일이 이미 사용 중인지 확인합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이메일 중복확인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 이메일 형식")
    })
    @GetMapping("/check-email/{email}")
    public ResponseEntity<Boolean> checkEmailDuplicate(
            @Parameter(description = "확인할 이메일 주소", required = true)
            @PathVariable String email) {
        log.info("이메일 중복확인 요청: email={}", email);
        
        try {
            boolean isDuplicate = userService.isEmailDuplicate(email);
            return ResponseEntity.ok(isDuplicate);
        } catch (Exception e) {
            log.error("이메일 중복확인 실패: email={}, error={}", email, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * OAuth 사용자 여부 확인
     */
    @GetMapping("/check-oauth/{email}")
    public ResponseEntity<Boolean> checkOAuthUser(@PathVariable String email) {
        log.info("OAuth 사용자 여부 확인 요청: email={}", email);
        
        try {
            boolean isOAuth = userService.isOAuthUser(email);
            return ResponseEntity.ok(isOAuth);
        } catch (Exception e) {
            log.error("OAuth 사용자 여부 확인 실패: email={}, error={}", email, e.getMessage());
            return ResponseEntity.badRequest().build();
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
     * 사용자 온보딩 완료 업데이트
     */
    @Operation(summary = "온보딩 완료", description = "온보딩 완료(is_onboard) 처리. 선택 본문으로 관심 주제(topicKeys), 알림(pushEnabled), 설문(유입·레벨·목적 등)을 함께 저장합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "온보딩 완료 업데이트 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PatchMapping("/{userId}/onboard")
    public ResponseEntity<ApiResponse<UserDto>> updateOnboard(
            @Parameter(description = "사용자 UUID", required = true)
            @PathVariable String userId,
            @RequestBody(required = false) OnboardingSubmitRequest body) {
        log.info("사용자 온보딩 완료 업데이트 요청: userId={}", userId);
        
        try {
            UserDto updatedUser = userService.updateOnboard(UUID.fromString(userId), body);
            return ResponseEntity.ok(ApiResponse.success(updatedUser, "온보딩이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            log.error("잘못된 사용자 ID: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("잘못된 사용자 ID입니다."));
        } catch (Exception e) {
            log.error("온보딩 완료 업데이트 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("온보딩 완료 업데이트에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 사용자 이메일 조회 (회신용)
     */
    @GetMapping("/{userId}/email")
    public ResponseEntity<ApiResponse<String>> getUserEmail(@PathVariable String userId) {
        try {
            UserDto user = userService.findById(UUID.fromString(userId));
            return ResponseEntity.ok(ApiResponse.success(user.email(), "사용자 이메일 조회 성공"));
        } catch (Exception e) {
            log.error("사용자 이메일 조회 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("사용자 이메일 조회에 실패했습니다."));
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
     * 회원탈퇴(하드 삭제): archive 이관 후 회원 레코드 물리 삭제.
     * 본인만 가능: path userId와 X-User-Id(게이트웨이가 넣어주는 토큰 사용자) 일치 검증.
     */
    @DeleteMapping("/{userId}/hard")
    public ResponseEntity<Void> hardDeleteUser(
            @PathVariable String userId,
            @RequestHeader(value = "X-User-Id", required = false) String authUserId) {
        log.info("회원탈퇴(하드 삭제) 요청: userId={}", userId);
        if (authUserId == null || !authUserId.equals(userId)) {
            log.warn("회원탈퇴(하드 삭제) 권한 없음: path userId={}, X-User-Id={}", userId, authUserId);
            return ResponseEntity.status(403).build();
        }
        try {
            userService.hardDeleteUser(UUID.fromString(userId));
            log.info("회원탈퇴(하드 삭제) 완료: userId={}", userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("잘못된 사용자 ID: userId={}, error={}", userId, e.getMessage());
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
    
    /**
     * 이메일 찾기
     */
    @Operation(summary = "이메일 찾기", description = "개인정보를 입력하여 이메일을 찾습니다. 반환되는 이메일은 마스킹 처리됩니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이메일 찾기 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "일치하는 사용자를 찾을 수 없음")
    })
    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<FindEmailResponse>> findEmail(@RequestBody FindEmailRequest request) {
        log.info("이메일 찾기 요청: firstName={}, lastName={}", request.getFirstName(), request.getLastName());
        
        try {
            FindEmailResponse response = userService.findEmailByPersonalInfo(request);
            return ResponseEntity.ok(ApiResponse.success(response, "이메일을 찾았습니다."));
        } catch (com.dorandoran.common.exception.DoranDoranException e) {
            log.error("이메일 찾기 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("이메일 찾기 중 예상치 못한 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("이메일 찾기 중 오류가 발생했습니다.", 
                            com.dorandoran.common.exception.ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }
}
