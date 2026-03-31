package com.dorandoran.auth.controller;

import com.dorandoran.auth.dto.LoginRequest;
import com.dorandoran.auth.dto.LoginResponse;
import com.dorandoran.auth.dto.RefreshTokenRequest;
import com.dorandoran.auth.dto.OAuthLoginRequest;
import com.dorandoran.shared.dto.PasswordResetCodeRequest;
import com.dorandoran.shared.dto.PasswordResetCodeVerifyRequest;
import com.dorandoran.shared.dto.ResetPasswordRequest;
import com.dorandoran.auth.service.AuthService;
import com.dorandoran.common.response.ApiResponse;
import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.shared.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 인증 컨트롤러 (User 서비스 중심 구조)
 * User 서비스와 완전 통합된 구조
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "인증 및 인가 API")
public class AuthController {
    
    private final AuthService authService;
    private final com.dorandoran.auth.service.UserIntegrationService userIntegrationService;
    private final com.dorandoran.auth.service.EmailVerificationRedisService emailVerificationRedisService;
    private final com.dorandoran.auth.service.EmailService emailService;
    private static final String EMAIL_VERIFICATION_APP_REDIRECT_BASE = "dorandoran://email-verified";
    @org.springframework.beans.factory.annotation.Value("${email.verification.backend-url:http://localhost:8081}")
    private String backendUrl;
    
    /**
     * 로그인
     */
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 인증 정보")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
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
     * OAuth 로그인
     */
    @Operation(summary = "OAuth 로그인", description = "Google OAuth 2.0 ID Token으로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OAuth 로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 인증 정보")
    })
    @PostMapping("/oauth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> oauthLogin(@RequestBody OAuthLoginRequest request) {
        log.info("OAuth 로그인 API 호출: provider={}", request.provider());
        
        try {
            LoginResponse response = authService.oauthLogin(request);
            return ResponseEntity.ok(ApiResponse.success(response, "OAuth 로그인에 성공했습니다."));
        } catch (DoranDoranException e) {
            log.error("OAuth 로그인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("OAuth 로그인 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("OAuth 로그인 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
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
     * 토큰 검증 (완전 구현)
     */
    @Operation(summary = "토큰 검증", description = "JWT 토큰의 유효성을 검증하고 사용자 정보를 반환합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 검증 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 유효하지 않음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<UserDto>> validateToken(
            @Parameter(description = "Bearer JWT 토큰", required = true)
            @RequestHeader("Authorization") String token) {
        log.info("토큰 검증 API 호출");
        
        try {
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            UserDto user = authService.validateToken(actualToken);
            return ResponseEntity.ok(ApiResponse.success(user, "토큰이 유효합니다."));
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
    
    /**
     * 비밀번호 재설정 코드 요청 (새로운 방식)
     */
    @PostMapping("/password/reset/request-code")
    public ResponseEntity<ApiResponse<Void>> requestPasswordResetCode(@RequestBody PasswordResetCodeRequest request) {
        log.info("비밀번호 재설정 코드 요청 API 호출: email={}", request.getEmail());
        
        try {
            authService.requestPasswordResetCode(request.getEmail());
            
            return ResponseEntity.ok(ApiResponse.success(null, "비밀번호 재설정 코드가 이메일로 발송되었습니다."));
        } catch (DoranDoranException e) {
            log.error("비밀번호 재설정 코드 요청 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("비밀번호 재설정 코드 요청 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("비밀번호 재설정 코드 요청 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }
    
    /**
     * 비밀번호 재설정 코드 검증
     */
    @PostMapping("/password/reset/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyPasswordResetCode(@RequestBody PasswordResetCodeVerifyRequest request) {
        log.info("비밀번호 재설정 코드 검증 API 호출: email={}", request.getEmail());
        
        try {
            authService.verifyPasswordResetCode(request.getEmail(), request.getCode());
            
            return ResponseEntity.ok(ApiResponse.success(null, "인증 코드가 확인되었습니다."));
        } catch (DoranDoranException e) {
            log.error("비밀번호 재설정 코드 검증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("비밀번호 재설정 코드 검증 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("비밀번호 재설정 코드 검증 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }
    
    /**
     * 비밀번호 재설정 실행 (코드 기반)
     */
    @PostMapping("/password/reset/execute")
    public ResponseEntity<ApiResponse<Void>> executePasswordReset(@RequestBody ResetPasswordRequest request) {
        log.info("비밀번호 재설정 실행 API 호출: email={}", request.getEmail());
        
        try {
            // 코드 기반 재설정
            if (request.getCode() != null && !request.getCode().isEmpty()) {
                authService.resetPasswordWithCode(request.getEmail(), request.getCode(), request.getNewPassword());
            } else {
                // 코드가 없으면 에러 반환
                throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "인증 코드가 필요합니다.");
            }
            
            return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 성공적으로 재설정되었습니다."));
        } catch (DoranDoranException e) {
            log.error("비밀번호 재설정 실행 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("비밀번호 재설정 실행 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("비밀번호 재설정 실행 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }
    
    /**
     * 비밀번호 재설정 요청 (기존 방식 - 하위 호환성 유지)
     * @deprecated 새로운 코드 기반 방식을 사용하세요: POST /api/auth/password/reset/request-code
     */
    @Deprecated
    @PostMapping("/password/reset/request")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(@RequestParam String email) {
        log.info("비밀번호 재설정 요청 API 호출 (기존 방식): email={}", email);
        
        try {
            // 비밀번호 재설정 토큰 생성 및 반환
            String resetToken = authService.requestPasswordReset(email);
            
            return ResponseEntity.ok(ApiResponse.success(resetToken, "비밀번호 재설정 토큰이 생성되었습니다. 토큰: " + resetToken));
        } catch (DoranDoranException e) {
            log.error("비밀번호 재설정 요청 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("비밀번호 재설정 요청 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("비밀번호 재설정 요청 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }
    
    /**
     * 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }

    /**
     * 이메일 인증 요청
     */
    @PostMapping("/email/request-verification")
    public ResponseEntity<ApiResponse<String>> requestEmailVerification(@RequestBody java.util.Map<String, String> request) {
        String email = request != null ? request.get("email") : null;
        String firstName = request != null ? request.get("firstName") : null;
        String lastName = request != null ? request.get("lastName") : null;
        log.info("이메일 인증 요청: email={}, firstName={}, lastName={}", email, firstName, lastName);        
        try {
            // 1. 이메일 중복 확인
            boolean isDuplicate = userIntegrationService.isEmailDuplicate(email);
            if (isDuplicate) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("이미 사용 중인 이메일입니다.", ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));
            }
            
            // 2. 토큰 생성
            String token = java.util.UUID.randomUUID().toString();
            
            // 3. Redis에 인증 요청 정보 저장 (이메일 전송 전에 저장)
            emailVerificationRedisService.saveVerificationRequest(email, token);
            log.info("Redis에 이메일 인증 요청 저장 완료: email={}", email);
            
            // 4. 이메일 발송 - 인증 링크에 firstName, lastName 포함
            String verifyLink = backendUrl + "/api/auth/email/verify?token=" + 
                    java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8) +
                    "&email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
            
            // firstName, lastName이 있으면 쿼리 파라미터에 추가
            if (firstName != null && !firstName.trim().isEmpty()) {
                verifyLink += "&firstName=" + java.net.URLEncoder.encode(firstName, java.nio.charset.StandardCharsets.UTF_8);
            }
            if (lastName != null && !lastName.trim().isEmpty()) {
                verifyLink += "&lastName=" + java.net.URLEncoder.encode(lastName, java.nio.charset.StandardCharsets.UTF_8);
            }
            
            try {
                emailService.sendVerificationEmail(email, verifyLink);
                log.info("이메일 인증 요청 완료: email={}", email);
                return ResponseEntity.ok(ApiResponse.success("sent", "인증 메일이 발송되었습니다."));
            } catch (Exception e) {
                // 이메일 전송 실패해도 Redis에는 저장되어 있으므로 사용자가 나중에 재시도 가능
                log.error("이메일 전송 실패 (Redis에는 저장됨): email={}, error={}", email, e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요. (인증 요청은 저장되었습니다.)", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
            }
        } catch (DoranDoranException e) {
            log.error("이메일 인증 요청 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("이메일 인증 요청 처리 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("이메일 인증 요청 처리 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }
    
    /**
     * 이메일 인증 링크 검증 및 완료 처리
     * HTML 페이지를 반환하여 같은 창에서 리디렉션되도록 처리
     */
    @GetMapping("/email/verify")
    public void verifyEmail(
            @RequestParam("token") String token,
            @RequestParam("email") String email,
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        log.info("이메일 인증 검증 API 호출: email={}, firstName={}, lastName={}", email, firstName, lastName);
        
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // 1. 토큰 검증
            if (!emailVerificationRedisService.verifyToken(email, token)) {
                log.warn("토큰 검증 실패: email={}", email);
                String errorMsg = java.net.URLEncoder.encode("인증 링크가 유효하지 않거나 만료되었습니다.", java.nio.charset.StandardCharsets.UTF_8);
                String redirectUrl = buildEmailVerificationRedirectUrl(
                        email, false, firstName, lastName, errorMsg);
                writeRedirectHtml(response, redirectUrl, "인증 실패");
                return;
            }
            
            // 2. Redis에서 인증 완료 처리
            emailVerificationRedisService.markEmailVerified(email);
            
            // 3. 프론트엔드 SignupPage로 리다이렉트 (같은 창에서) - firstName, lastName 포함
            log.info("이메일 인증 완료: email={}", email);
            String redirectUrl = buildEmailVerificationRedirectUrl(
                    email, true, firstName, lastName, null);
            writeRedirectHtml(response, redirectUrl, "인증 완료");
        } catch (Exception e) {
            log.error("이메일 인증 처리 중 오류", e);
            String errorMsg = java.net.URLEncoder.encode("이메일 인증 처리 중 오류가 발생했습니다.", java.nio.charset.StandardCharsets.UTF_8);
            String redirectUrl = buildEmailVerificationRedirectUrl(
                    email, false, firstName, lastName, errorMsg);
            writeRedirectHtml(response, redirectUrl, "인증 오류");
        }
    }

    /**
     * 이메일 인증 결과를 앱 딥링크로 조립
     */
    private String buildEmailVerificationRedirectUrl(
            String email,
            boolean verified,
            String firstName,
            String lastName,
            String errorMsgEncoded) {
        String redirectUrl = EMAIL_VERIFICATION_APP_REDIRECT_BASE + "?email="
                + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8)
                + "&verified=" + verified;
        if (errorMsgEncoded != null && !errorMsgEncoded.trim().isEmpty()) {
            redirectUrl += "&error=" + errorMsgEncoded;
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            redirectUrl += "&firstName=" + java.net.URLEncoder.encode(firstName, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            redirectUrl += "&lastName=" + java.net.URLEncoder.encode(lastName, java.nio.charset.StandardCharsets.UTF_8);
        }
        return redirectUrl;
    }
    
    /**
     * HTML 페이지를 작성하여 같은 창에서 리디렉션
     */
    private void writeRedirectHtml(jakarta.servlet.http.HttpServletResponse response, String redirectUrl, String title) throws java.io.IOException {
        java.io.PrintWriter writer = response.getWriter();
        writer.write("<!DOCTYPE html>\n");
        writer.write("<html lang=\"ko\">\n");
        writer.write("<head>\n");
        writer.write("    <meta charset=\"UTF-8\">\n");
        writer.write("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        writer.write("    <title>" + title + "</title>\n");
        writer.write("    <script>\n");
        writer.write("        // 같은 창에서 리디렉션\n");
        writer.write("        window.location.href = '" + redirectUrl.replace("'", "\\'") + "';\n");
        writer.write("    </script>\n");
        writer.write("</head>\n");
        writer.write("<body>\n");
        writer.write("    <p>리디렉션 중...</p>\n");
        writer.write("    <p>자동으로 이동하지 않으면 <a href=\"" + redirectUrl.replace("\"", "&quot;") + "\">여기를 클릭하세요</a>.</p>\n");
        writer.write("</body>\n");
        writer.write("</html>");
        writer.flush();
    }
    
    /**
     * 이메일 인증 완료 여부 확인
     */
    @GetMapping("/email/check")
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> checkEmailVerified(@RequestParam("email") String email) {
        // URL 디코딩 처리 (Spring이 자동 디코딩하지 않는 경우 대비)
        String decodedEmail = email;
        try {
            decodedEmail = java.net.URLDecoder.decode(email, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("이메일 URL 디코딩 실패, 원본 사용: email={}", email);
        }
        log.info("이메일 인증 완료 여부 확인: email={}, decodedEmail={}", email, decodedEmail);
        
        try {
            boolean verified = emailVerificationRedisService.isEmailVerified(decodedEmail);
            java.util.Map<String, Boolean> result = new java.util.HashMap<>();
            result.put("verified", verified);
            
            return ResponseEntity.ok(ApiResponse.success(result, verified ? "인증이 완료되었습니다." : "인증이 완료되지 않았습니다."));
        } catch (Exception e) {
            log.error("이메일 인증 확인 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("이메일 인증 확인 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }
    
    /**
     * 사용자 정보 조회 (인증된 사용자)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(HttpServletRequest request) {
        log.info("현재 사용자 정보 조회 API 호출");
        
        try {
            // HMAC 헤더에서 사용자 정보 추출 (Gateway에서 주입됨)
            String userEmail = request.getHeader("X-User-Email");
            String userId = request.getHeader("X-User-Id");
            
            if (userEmail == null || userEmail.isEmpty()) {
                log.warn("HMAC 헤더에서 사용자 이메일을 찾을 수 없음");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("인증이 필요합니다.", ErrorCode.AUTH_TOKEN_INVALID.getCode()));
            }
            
            UserDto user = authService.findUserByEmail(userEmail);
            return ResponseEntity.ok(ApiResponse.success(user, "사용자 정보를 성공적으로 조회했습니다."));
        } catch (DoranDoranException e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("사용자 정보 조회 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }

    /**
     * 내부 API: 회원 탈퇴(하드 삭제) 시 해당 사용자 토큰 즉시 무효화.
     * User 서비스에서 HMAC 헤더로 호출.
     */
    @PostMapping("/internal/users/{userId}/invalidate-tokens")
    public ResponseEntity<Void> invalidateTokensForUser(@PathVariable String userId) {
        try {
            authService.invalidateAllTokensForUser(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("토큰 무효화 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}