package com.dorandoran.user.service;

import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.user.entity.User;
import com.dorandoran.user.repository.UserRepository;
import com.dorandoran.user.repository.UserStatsRepository;
import com.dorandoran.user.util.EmailMaskingUtil;
import com.dorandoran.shared.dto.CreateUserRequest;
import com.dorandoran.shared.dto.UpdateUserRequest;
import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.shared.dto.UserWithPasswordDto;
import com.dorandoran.shared.dto.FindEmailRequest;
import com.dorandoran.shared.dto.FindEmailResponse;
import com.dorandoran.shared.event.UserCreatedEvent;
import com.dorandoran.shared.event.UserStatusChangedEvent;
import com.dorandoran.shared.event.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 사용자 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthServiceIntegration authServiceIntegration;
    private final UserWithdrawalArchiveService userWithdrawalArchiveService;
    private final UserStatsRepository userStatsRepository;
    
    /**
     * 사용자 생성
     */
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        System.out.println("사용자 생성 요청: email=" + request.email());
        
        // 1. 이메일 중복 검사
        if (userRepository.existsByEmail(request.email())) {
            throw new DoranDoranException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        
        // 2. 이메일 인증 완료 여부 확인
        boolean isEmailVerified = authServiceIntegration.isEmailVerified(request.email());
        if (!isEmailVerified) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "이메일 인증을 먼저 완료해주세요.");
        }
        
        // 3. 비밀번호 기초 정책 검사 (최소 8자, 영문/숫자 포함)
        validateBasicPasswordPolicy(request.password());
        
        // 4. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());
        
        // 4-1. 생년월일 파싱 (yyyy-MM-dd)
        LocalDate birthDate = LocalDate.parse(request.birthDate());
        
        // 5. 사용자 생성 (ACTIVE 상태로 바로 생성)
        User user = User.builder()
            .id(UUID.randomUUID())
            .email(request.email())
            .firstName(request.firstName())
            .lastName(request.lastName())
            .name(request.getDisplayName())
            .passwordHash(encodedPassword)
            .picture(request.picture())
            .info(request.info() != null ? request.info() : "")
            .birthDate(birthDate)
            .signupQuestion(request.signupQuestion())
            .signupAnswer(request.signupAnswer())
            .status(User.UserStatus.ACTIVE)
            .coachCheck(false)
            .exitModalDoNotShowAgain(false)
            .isOnboard(false)
            .oauthProvider(null)
            .oauthId(null)
            .build();
        
        // 5-1. 인증 방법 검증
        user.validateAuthMethod();
        
        // 6. 데이터베이스 저장
        User savedUser = userRepository.save(user);
        log.info("사용자 생성 완료: id={}, email={}", savedUser.getId(), savedUser.getEmail());
        
        // 7. 이메일 인증 데이터 삭제 (Redis TTL로 자동 삭제되지만 명시적으로 처리)
        authServiceIntegration.deleteEmailVerification(request.email());
        
        // 8. 사용자 생성 이벤트 발행
        UserCreatedEvent event = UserCreatedEvent.of(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            savedUser.getName()
        );
        eventPublisher.publishEvent(event);
        log.info("사용자 생성 이벤트 발행: userId={}", savedUser.getId());
        
        return convertToDto(savedUser);
    }

    private void validateBasicPasswordPolicy(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "비밀번호는 최소 8자 이상이어야 합니다.");
        }
        boolean hasLetter = rawPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = rawPassword.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "비밀번호에는 영문과 숫자가 각각 최소 1자 포함되어야 합니다.");
        }
    }
    
    /**
     * 사용자 조회 (ID)
     */
    public UserDto findById(UUID id) {
        System.out.println("사용자 조회: id=" + id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
        
        return convertToDto(user);
    }
    
    /**
     * 사용자 조회 (이메일)
     */
    public UserDto findByEmail(String email) {
        System.out.println("사용자 조회: email=" + email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
        
        return convertToDto(user);
    }
    
    /**
     * 사용자 조회 (이메일) - Auth 서비스용 (passwordHash 포함)
     */
    public UserWithPasswordDto findByEmailForAuth(String email) {
        System.out.println("Auth 서비스용 사용자 조회: email=" + email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
        
        return convertToDtoWithPassword(user);
    }
    
    /**
     * OAuth 사용자 조회
     */
    public UserDto findByOAuth(String provider, String oauthId) {
        log.info("OAuth 사용자 조회: provider={}, oauthId={}", provider, oauthId);
        
        try {
            User.OAuthProvider oauthProvider = User.OAuthProvider.valueOf(provider.toUpperCase());
            User user = userRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId)
                    .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
            
            return convertToDto(user);
        } catch (IllegalArgumentException e) {
            log.error("지원하지 않는 OAuth 제공자: provider={}", provider);
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "지원하지 않는 OAuth 제공자입니다.");
        }
    }
    
    /**
     * OAuth 사용자 생성
     */
    @Transactional
    public UserDto createOAuthUser(String email, String firstName, String lastName, String name,
                                   String picture, String provider, String oauthId, String birthDate) {
        log.info("OAuth 사용자 생성 요청: email={}, provider={}", email, provider);
        
        // 1. 이메일 중복 검사
        if (userRepository.existsByEmail(email)) {
            throw new DoranDoranException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        
        // 2. OAuth ID 중복 검사
        try {
            User.OAuthProvider oauthProvider = User.OAuthProvider.valueOf(provider.toUpperCase());
            if (userRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId).isPresent()) {
                throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "이미 등록된 OAuth 계정입니다.");
            }
            
            // 3. 생년월일 파싱 (yyyy-MM-dd). 미전달 또는 형식 오류 시 기본값 사용
            LocalDate parsedBirthDate = LocalDate.of(1900, 1, 1);
            if (birthDate != null && !birthDate.isBlank()) {
                try {
                    parsedBirthDate = LocalDate.parse(birthDate);
                } catch (java.time.format.DateTimeParseException e) {
                    log.warn("OAuth 사용자 생성 - birthDate 파싱 실패, 기본값 사용: birthDate={}", birthDate);
                }
            }
            
            // 4. 사용자 생성 (ACTIVE 상태로 바로 생성)
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email(email)
                    .firstName(firstName != null ? firstName : "")
                    .lastName(lastName != null ? lastName : "")
                    .name(name != null ? name : ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim())
                    .passwordHash(null) // OAuth 사용자는 비밀번호 없음
                    .picture(picture)
                    .info("")
                    .birthDate(parsedBirthDate)
                    .status(User.UserStatus.ACTIVE)
                    .coachCheck(false)
                    .exitModalDoNotShowAgain(false)
                    .isOnboard(false)
                    .oauthProvider(oauthProvider)
                    .oauthId(oauthId)
                    .build();
            
            // 5. 인증 방법 검증
            user.validateAuthMethod();
            
            // 6. 데이터베이스 저장
            User savedUser = userRepository.save(user);
            log.info("OAuth 사용자 생성 완료: id={}, email={}, provider={}", savedUser.getId(), savedUser.getEmail(), provider);
            
            // 7. 사용자 생성 이벤트 발행
            UserCreatedEvent event = UserCreatedEvent.of(
                    savedUser.getId(),
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    savedUser.getLastName(),
                    savedUser.getName()
            );
            eventPublisher.publishEvent(event);
            log.info("OAuth 사용자 생성 이벤트 발행: userId={}", savedUser.getId());
            
            return convertToDto(savedUser);
        } catch (IllegalArgumentException e) {
            log.error("지원하지 않는 OAuth 제공자: provider={}", provider);
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "지원하지 않는 OAuth 제공자입니다.");
        }
    }
    
    /**
     * 이메일 중복확인
     */
    public boolean isEmailDuplicate(String email) {
        log.info("이메일 중복확인: email={}", email);
        
        // 이메일 형식 검증
        if (email == null || email.trim().isEmpty()) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "이메일을 입력해주세요.");
        }
        
        // 기본적인 이메일 형식 검증
        if (!email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "올바른 이메일 형식이 아닙니다.");
        }
        
        boolean exists = userRepository.existsByEmail(email);
        log.info("이메일 중복확인 결과: email={}, exists={}", email, exists);
        
        return exists;
    }
    
    /**
     * OAuth 사용자 여부 확인
     */
    public boolean isOAuthUser(String email) {
        log.info("OAuth 사용자 여부 확인: email={}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
        
        boolean isOAuth = user.getOauthProvider() != null;
        log.info("OAuth 사용자 여부 확인 결과: email={}, isOAuth={}", email, isOAuth);
        
        return isOAuth;
    }
    
    /**
     * 사용자 목록 조회
     */
    public List<UserDto> findAllUsers() {
        System.out.println("전체 사용자 목록 조회");
        
        List<User> users = userRepository.findAll();
        System.out.println("DEBUG - 조회된 사용자 수: " + users.size());
        
        for (User user : users) {
            System.out.println("DEBUG - User ID: " + user.getId() + ", Role: " + user.getRole());
        }
        
        return users.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * 활성 사용자 목록 조회
     */
    public List<UserDto> findActiveUsers() {
        System.out.println("활성 사용자 목록 조회");
        
        return userRepository.findByStatus("ACTIVE").stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * 사용자 정보 수정
     */
    @Transactional
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        System.out.println("사용자 정보 수정: id=" + id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
        
        // 이메일 변경 시 중복 검사
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new DoranDoranException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(request.email());
        }
        
        // 정보 업데이트
        user.updateInfo(
            request.firstName(),
            request.lastName(),
            request.name(),
            request.picture(),
            request.info()
        );
        
        // 상태 업데이트
        if (request.status() != null) {
            user.updateStatus(convertToEntityStatus(request.status()));
        }
        
        // 코치 체크 업데이트
        if (request.coachCheck() != null) {
            user.updateCoachCheck(request.coachCheck());
        }
        
        // 나가기 모달 다시 보지 않기 설정 업데이트
        if (request.exitModalDoNotShowAgain() != null) {
            user.updateExitModalDoNotShowAgain(request.exitModalDoNotShowAgain());
        }
        
        User savedUser = userRepository.save(user);
        
        // 사용자 업데이트 이벤트 발행
        UserUpdatedEvent event = UserUpdatedEvent.of(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            savedUser.getName(),
            savedUser.getPicture(),
            savedUser.getInfo()
        );
        eventPublisher.publishEvent(event);
        log.info("사용자 업데이트 이벤트 발행: userId={}", savedUser.getId());
        
        System.out.println("사용자 정보 수정 완료: id=" + savedUser.getId());
        
        return convertToDto(savedUser);
    }
    
    /**
     * 사용자 상태 변경
     */
    @Transactional
    public UserDto updateUserStatus(UUID id, UserDto.UserStatus status) {
        System.out.println("사용자 상태 변경: id=" + id + ", status=" + status);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
        
        // 현재 상태와 동일한지 확인
        User.UserStatus currentStatus = user.getStatus();
        User.UserStatus newStatus = convertToEntityStatus(status);
        
        if (currentStatus == newStatus) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, 
                "현재 상태와 동일한 상태로 변경할 수 없습니다");
        }
        
        user.updateStatus(newStatus);
        User savedUser = userRepository.save(user);
        
        // 사용자 상태 변경 이벤트 발행
        UserStatusChangedEvent event = UserStatusChangedEvent.of(
            savedUser.getId(),
            savedUser.getEmail(),
            convertToDtoStatus(currentStatus),
            convertToDtoStatus(newStatus)
        );
        eventPublisher.publishEvent(event);
        log.info("사용자 상태 변경 이벤트 발행: userId={}, {} -> {}", 
                savedUser.getId(), currentStatus, newStatus);
        
        System.out.println("사용자 상태 변경 완료: id=" + savedUser.getId() + ", " + currentStatus + " -> " + newStatus);
        
        return convertToDto(savedUser);
    }
    
    /**
     * 사용자 삭제 (소프트 삭제 - 상태를 INACTIVE로 변경)
     */
    @Transactional
    public void deleteUser(UUID id) {
        System.out.println("사용자 삭제: id=" + id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
        
        if (user.getStatus() == User.UserStatus.INACTIVE) {
            throw new DoranDoranException(ErrorCode.USER_ALREADY_INACTIVE);
        }
        
        user.updateStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);
        
        System.out.println("사용자 삭제 완료: id=" + id);
    }

    /**
     * 회원 탈퇴(하드 삭제): 토큰 무효화 → archive 이관 → 선행 삭제 → app_user 물리 삭제.
     * {@link #deleteUser(UUID)} 는 소프트 삭제(INACTIVE)만 수행.
     */
    @Transactional
    public void hardDeleteUser(UUID id) {
        log.info("회원 탈퇴(하드 삭제) 시작: userId={}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));

        authServiceIntegration.invalidateTokensForUser(id.toString());

        userWithdrawalArchiveService.archiveUserData(id);

        userWithdrawalArchiveService.deleteMonthlyUserCosts(id);
        userStatsRepository.findById(id).ifPresent(userStatsRepository::delete);
        userWithdrawalArchiveService.deleteUserChatbotLastInteraction(id);

        userRepository.delete(user);

        log.info("회원 탈퇴(하드 삭제) 완료: userId={}", id);
    }
    
    /**
     * 마지막 연결 시간 업데이트
     */
    public void updateLastConnectionTime(UUID id) {
        System.out.println("마지막 연결 시간 업데이트: id=" + id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
        
        user.updateLastConnectionTime();
        userRepository.save(user);
    }

    /**
     * 이메일로 비밀번호 재설정
     */
    @Transactional
    public void resetPasswordByEmail(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));

        validateBasicPasswordPolicy(newPassword);
        String encoded = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encoded);
        userRepository.save(user);
    }
    
    /**
     * Entity를 DTO로 변환
     */
    private UserDto convertToDto(User user) {
        System.out.println("DEBUG - User role: " + user.getRole());
        System.out.println("DEBUG - User role type: " + (user.getRole() != null ? user.getRole().getClass() : "NULL"));
        
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getName(),
            user.getPasswordHash(),
            user.getPicture(),
            user.getInfo(),
            user.getBirthDate(),
            user.getSignupQuestion(),
            user.getSignupAnswer(),
            null, // preferences - User 엔티티에 해당 필드가 없으므로 null로 설정
            user.getLastConnTime(),
            convertToDtoStatus(user.getStatus()),
            convertToDtoRole(user.getRole()),
            user.isCoachCheck(),
            user.isExitModalDoNotShowAgain(),
            user.isOnboard(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
    
    /**
     * Entity를 DTO로 변환 (Auth 서비스용 - passwordHash 포함)
     */
    private UserWithPasswordDto convertToDtoWithPassword(User user) {
        System.out.println("DEBUG - User role: " + user.getRole());
        System.out.println("DEBUG - User role type: " + (user.getRole() != null ? user.getRole().getClass() : "NULL"));
        
        return new UserWithPasswordDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getName(),
            user.getPasswordHash(),
            user.getPicture(),
            user.getInfo(),
            user.getBirthDate(),
            user.getSignupQuestion(),
            user.getSignupAnswer(),
            null, // preferences - User 엔티티에 해당 필드가 없으므로 null로 설정
            user.getLastConnTime(),
            convertToDtoStatus(user.getStatus()),
            convertToDtoRole(user.getRole()),
            user.isCoachCheck(),
            user.isExitModalDoNotShowAgain(),
            user.isOnboard(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
    
    /**
     * Entity Status를 DTO Status로 변환
     */
    private UserDto.UserStatus convertToDtoStatus(User.UserStatus status) {
        return switch (status) {
            case ACTIVE -> UserDto.UserStatus.ACTIVE;
            case INACTIVE -> UserDto.UserStatus.INACTIVE;
            case SUSPENDED -> UserDto.UserStatus.SUSPENDED;
        };
    }
    
    /**
     * DTO Status를 Entity Status로 변환
     */
    private User.UserStatus convertToEntityStatus(UserDto.UserStatus status) {
        return switch (status) {
            case ACTIVE -> User.UserStatus.ACTIVE;
            case INACTIVE -> User.UserStatus.INACTIVE;
            case SUSPENDED -> User.UserStatus.SUSPENDED;
        };
    }
    
    /**
     * 사용자 비밀번호 업데이트
     */
    @Transactional
    public void updatePassword(UUID userId, String newPassword) {
        log.info("사용자 비밀번호 업데이트: userId={}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
        
        // 비밀번호 정책 검증
        validateBasicPasswordPolicy(newPassword);
        
        // 새 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(newPassword);
        
        // 비밀번호 업데이트
        user.updatePassword(encodedPassword);
        userRepository.save(user);
        
        log.info("사용자 비밀번호 업데이트 완료: userId={}", userId);
    }
    
    /**
     * 사용자 온보딩 완료 여부 업데이트
     */
    @Transactional
    public UserDto updateOnboard(UUID userId) {
        log.info("사용자 온보딩 완료 업데이트: userId={}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
        
        // 온보딩 완료로 업데이트
        user.updateOnboard(true);
        User savedUser = userRepository.save(user);
        
        log.info("사용자 온보딩 완료 업데이트 완료: userId={}", userId);
        
        return convertToDto(savedUser);
    }
    
    /**
     * Entity Role을 DTO Role로 변환
     */
    private UserDto.RoleName convertToDtoRole(User.RoleName role) {
        return switch (role) {
            case ROLE_USER -> UserDto.RoleName.ROLE_USER;
            case ROLE_ADMIN -> UserDto.RoleName.ROLE_ADMIN;
        };
    }
    
    /**
     * 개인정보로 이메일 찾기
     */
    public FindEmailResponse findEmailByPersonalInfo(FindEmailRequest request) {
        log.info("이메일 찾기 요청: firstName={}, lastName={}, birthDate={}", 
                request.getFirstName(), request.getLastName(), request.getBirthDate());
        
        try {
            // 1. birthDate를 LocalDate로 파싱
            LocalDate birthDate = LocalDate.parse(request.getBirthDate());
            
            // 2. Repository에서 사용자 조회
            Optional<User> userOpt = userRepository.findByPersonalInfo(
                    request.getFirstName(),
                    request.getLastName(),
                    birthDate,
                    request.getSignupQuestion(),
                    request.getSignupAnswer()
            );
            
            // 3. 사용자 없음 → 예외 반환
            if (userOpt.isEmpty()) {
                log.warn("이메일 찾기 실패: 일치하는 사용자를 찾을 수 없음");
                throw new DoranDoranException(ErrorCode.USER_NOT_FOUND, "입력하신 정보와 일치하는 사용자를 찾을 수 없습니다.");
            }
            
            // 4. 사용자 있음 → 이메일 마스킹 후 반환
            User user = userOpt.get();
            String maskedEmail = EmailMaskingUtil.maskEmail(user.getEmail());
            
            log.info("이메일 찾기 성공: userId={}, maskedEmail={}", user.getId(), maskedEmail);
            
            return new FindEmailResponse(maskedEmail);
            
        } catch (java.time.format.DateTimeParseException e) {
            log.error("생년월일 파싱 실패: birthDate={}, error={}", request.getBirthDate(), e.getMessage());
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "생년월일 형식이 올바르지 않습니다. (yyyy-MM-dd 형식)");
        } catch (DoranDoranException e) {
            log.error("이메일 찾기 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("이메일 찾기 중 예상치 못한 오류: error={}", e.getMessage(), e);
            throw new DoranDoranException(ErrorCode.INTERNAL_SERVER_ERROR, "이메일 찾기 중 오류가 발생했습니다.");
        }
    }
}