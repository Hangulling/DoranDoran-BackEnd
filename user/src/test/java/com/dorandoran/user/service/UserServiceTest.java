package com.dorandoran.user.service;

import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.infra.persistence.entity.User;
import com.dorandoran.infra.persistence.repository.UserRepository;
import com.dorandoran.shared.dto.CreateUserRequest;
import com.dorandoran.shared.dto.UpdateUserRequest;
import com.dorandoran.shared.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 사용자 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private CreateUserRequest createRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .firstName("홍")
            .lastName("길동")
            .name("홍길동")
            .passwordHash("encodedPassword")
            .picture("profile.jpg")
            .info("테스트 사용자")
            .lastConnTime(LocalDateTime.now())
            .status(User.UserStatus.ACTIVE)
            .coachCheck(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        createRequest = new CreateUserRequest(
            "test@example.com",
            "홍",
            "길동",
            "홍길동",
            "password123",
            "profile.jpg",
            "테스트 사용자"
        );
    }

    @Test
    void 사용자_생성_성공() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDto result = userService.createUser(createRequest);

        // Then
        assertNotNull(result);
        assertEquals("test@example.com", result.email());
        assertEquals("홍길동", result.name());
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 사용자_생성_실패_이메일_중복() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            userService.createUser(createRequest);
        });

        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 사용자_조회_성공() {
        // Given
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.findById(testUser.getId());

        // Then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.id());
        assertEquals(testUser.getEmail(), result.email());
        verify(userRepository).findById(testUser.getId());
    }

    @Test
    void 사용자_조회_실패_사용자_없음() {
        // Given
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            userService.findById(UUID.randomUUID());
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository).findById(any(UUID.class));
    }

    @Test
    void 사용자_정보_수정_성공() {
        // Given
        UpdateUserRequest updateRequest = new UpdateUserRequest(
            "updated@example.com",
            "김",
            "철수",
            "김철수",
            "new-profile.jpg",
            "수정된 정보",
            UserDto.UserStatus.ACTIVE,
            true
        );

        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDto result = userService.updateUser(testUser.getId(), updateRequest);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(testUser.getId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 사용자_상태_변경_성공() {
        // Given
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDto result = userService.updateUserStatus(testUser.getId(), UserDto.UserStatus.INACTIVE);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(testUser.getId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 사용자_삭제_성공() {
        // Given
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.deleteUser(testUser.getId());

        // Then
        verify(userRepository).findById(testUser.getId());
        verify(userRepository).save(any(User.class));
    }
}
