package com.dorandoran.auth.controller;

import com.dorandoran.auth.dto.LoginRequest;
import com.dorandoran.auth.dto.LoginResponse;
import com.dorandoran.auth.dto.RefreshTokenRequest;
import com.dorandoran.auth.service.AuthService;
import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 인증 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 API 호출: email={}", request.getEmail());
        
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response, "로그인에 성공했습니다."));
        } catch (DoranDoranException e) {
            log.error("로그인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("로그인 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("로그인 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }
    
    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String token) {
        log.info("로그아웃 API 호출");
        
        try {
            // Bearer 토큰에서 실제 토큰 추출
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            authService.logout(actualToken);
            return ResponseEntity.ok(ApiResponse.success(null, "로그아웃에 성공했습니다."));
        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.success(null, "로그아웃에 성공했습니다."));
        }
    }
    
    /**
     * 토큰 검증
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Void>> validateToken(@RequestHeader("Authorization") String token) {
        log.info("토큰 검증 API 호출");
        
        try {
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            authService.validateToken(actualToken);
            return ResponseEntity.ok(ApiResponse.success(null, "토큰이 유효합니다."));
        } catch (DoranDoranException e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("토큰 검증 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("토큰 검증 중 오류가 발생했습니다.", ErrorCode.AUTH_TOKEN_INVALID.getCode()));
        }
    }
    
    /**
     * 헬스체크 (v1)
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
    
    
    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("토큰 갱신 API 호출");
        
        try {
            LoginResponse response = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success(response, "토큰 갱신에 성공했습니다."));
        } catch (DoranDoranException e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("토큰 갱신 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("토큰 갱신 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }
}