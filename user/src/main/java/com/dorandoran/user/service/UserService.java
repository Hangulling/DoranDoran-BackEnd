package com.dorandoran.user.service;

import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.user.entity.User;
import com.dorandoran.user.repository.UserRepository;
import com.dorandoran.shared.dto.CreateUserRequest;
import com.dorandoran.shared.dto.UpdateUserRequest;
import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.shared.event.UserCreatedEvent;
import com.dorandoran.shared.event.UserStatusChangedEvent;
import com.dorandoran.shared.event.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        
        // 1-1. 비밀번호 기초 정책 검사 (최소 8자, 영문/숫자 포함)
        validateBasicPasswordPolicy(request.password());
        
        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());
        
        // 3. 사용자 생성
        User user = User.builder()
            .id(UUID.randomUUID())
            .email(request.email())
            .firstName(request.firstName())
            .lastName(request.lastName())
            .name(request.getDisplayName())
            .passwordHash(encodedPassword)
            .picture(request.picture())
            .info(request.info() != null ? request.info() : "")
            .status(User.UserStatus.ACTIVE)
            .coachCheck(false)
            .build();
        
        // 4. 데이터베이스 저장
        User savedUser = userRepository.save(user);
        log.info("사용자 생성 완료: id={}, email={}", savedUser.getId(), savedUser.getEmail());
        
        // 5. 사용자 생성 이벤트 발행
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
            "",
            user.getLastConnTime(),
            convertToDtoStatus(user.getStatus()),
            convertToDtoRole(user.getRole()),
            user.isCoachCheck(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

//    private UserDto convertToDto(User user) {
//        UserDto.RoleName roleName = user.getRole() != null ? UserDto.RoleName.valueOf(user.getRole().name()) : null;
//        String preferences = ""; // User 엔티티에는 없으므로 빈 문자열 처리
//
//        return new UserDto(
//            user.getId(),
//            user.getEmail(),
//            user.getFirstName(),
//            user.getLastName(),
//            user.getName(),
//            user.getPasswordHash(),
//            user.getPicture(),
//            user.getInfo(),
//            preferences,
//            user.getLastConnTime(),
//            UserDto.UserStatus.valueOf(user.getStatus().name()),
//            roleName,
//            user.isCoachCheck(),
//            user.getCreatedAt(),
//            user.getUpdatedAt()
//        );
//    }


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
     * Entity Role을 DTO Role로 변환
     */
    private UserDto.RoleName convertToDtoRole(User.RoleName role) {
        return switch (role) {
            case ROLE_USER -> UserDto.RoleName.ROLE_USER;
            case ROLE_ADMIN -> UserDto.RoleName.ROLE_ADMIN;
        };
    }
}