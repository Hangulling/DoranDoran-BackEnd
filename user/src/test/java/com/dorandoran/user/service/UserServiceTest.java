package com.dorandoran.user.service;

import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.infra.persistence.entity.User;
import com.dorandoran.infra.persistence.repository.UserRepository;
import com.dorandoran.shared.dto.CreateUserRequest;
import com.dorandoran.shared.dto.UpdateUserRequest;
import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.shared.event.UserCreatedEvent;
import com.dorandoran.shared.event.UserUpdatedEvent;
import com.dorandoran.shared.event.UserStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

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

    @Test
    void 사용자_이메일_조회_성공() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.findByEmail("test@example.com");

        // Then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.id());
        assertEquals(testUser.getEmail(), result.email());
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void 사용자_이메일_조회_실패() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            userService.findByEmail("nonexistent@example.com");
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void 전체_사용자_목록_조회() {
        // Given
        User user1 = createTestUser("user1@example.com", "사용자1");
        User user2 = createTestUser("user2@example.com", "사용자2");
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        // When
        List<UserDto> result = userService.findAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void 활성_사용자_목록_조회() {
        // Given
        User activeUser = createTestUser("active@example.com", "활성사용자");
        activeUser.updateStatus(User.UserStatus.ACTIVE);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(List.of(activeUser));

        // When
        List<UserDto> result = userService.findActiveUsers();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("active@example.com", result.get(0).email());
        verify(userRepository).findByStatus("ACTIVE");
    }

    @Test
    void 마지막_연결시간_업데이트() {
        // Given
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateLastConnectionTime(testUser.getId());

        // Then
        verify(userRepository).findById(testUser.getId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 비밀번호_재설정_이메일_성공() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.resetPasswordByEmail("test@example.com", "newPassword123");

        // Then
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 비밀번호_재설정_이메일_실패_사용자없음() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            userService.resetPasswordByEmail("nonexistent@example.com", "newPassword123");
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void 비밀번호_업데이트_성공() {
        // Given
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updatePassword(testUser.getId(), "newPassword123");

        // Then
        verify(userRepository).findById(testUser.getId());
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 비밀번호_업데이트_실패_사용자없음() {
        // Given
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            userService.updatePassword(UUID.randomUUID(), "newPassword123");
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository).findById(any(UUID.class));
    }

    @Test
    void 비밀번호_정책_검증_실패_길이부족() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            CreateUserRequest request = new CreateUserRequest(
                "test@example.com",
                "홍",
                "길동", 
                "홍길동",
                "123", // 8자 미만
                "profile.jpg",
                "테스트 사용자"
            );
            userService.createUser(request);
        });

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 비밀번호_정책_검증_실패_영문없음() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            CreateUserRequest request = new CreateUserRequest(
                "test@example.com",
                "홍",
                "길동",
                "홍길동", 
                "12345678", // 영문 없음
                "profile.jpg",
                "테스트 사용자"
            );
            userService.createUser(request);
        });

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 비밀번호_정책_검증_실패_숫자없음() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // When & Then
        DoranDoranException exception = assertThrows(DoranDoranException.class, () -> {
            CreateUserRequest request = new CreateUserRequest(
                "test@example.com",
                "홍",
                "길동",
                "홍길동",
                "password", // 숫자 없음
                "profile.jpg", 
                "테스트 사용자"
            );
            userService.createUser(request);
        });

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 사용자_생성_시_이벤트_발행_검증() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.createUser(createRequest);

        // Then
        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        UserCreatedEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(testUser.getId(), capturedEvent.userId());
        assertEquals(testUser.getEmail(), capturedEvent.email());
        assertEquals(testUser.getFirstName(), capturedEvent.firstName());
        assertEquals(testUser.getLastName(), capturedEvent.lastName());
        assertEquals(testUser.getName(), capturedEvent.name());
    }

    @Test
    void 사용자_수정_시_이벤트_발행_검증() {
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
        userService.updateUser(testUser.getId(), updateRequest);

        // Then
        ArgumentCaptor<UserUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(UserUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        UserUpdatedEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(testUser.getId(), capturedEvent.userId());
        assertEquals(testUser.getEmail(), capturedEvent.email());
        assertEquals(testUser.getFirstName(), capturedEvent.firstName());
        assertEquals(testUser.getLastName(), capturedEvent.lastName());
        assertEquals(testUser.getName(), capturedEvent.name());
    }

    @Test
    void 사용자_상태변경_시_이벤트_발행_검증() {
        // Given
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUserStatus(testUser.getId(), UserDto.UserStatus.INACTIVE);

        // Then
        ArgumentCaptor<UserStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(UserStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        UserStatusChangedEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(testUser.getId(), capturedEvent.userId());
        assertEquals(testUser.getEmail(), capturedEvent.email());
        assertEquals(UserDto.UserStatus.ACTIVE, capturedEvent.oldStatus());
        assertEquals(UserDto.UserStatus.INACTIVE, capturedEvent.newStatus());
    }

    @Test
    void 사용자_삭제_시_이벤트_발행_없음() {
        // Given
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.deleteUser(testUser.getId());

        // Then - 삭제는 이벤트를 발행하지 않음
        verify(eventPublisher, never()).publishEvent(any(UserCreatedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(UserUpdatedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(UserStatusChangedEvent.class));
    }

    // 헬퍼 메서드
    private User createTestUser(String email, String name) {
        return User.builder()
            .id(UUID.randomUUID())
            .email(email)
            .firstName("테스트")
            .lastName("사용자")
            .name(name)
            .passwordHash("encodedPassword")
            .picture("profile.jpg")
            .info("테스트 사용자")
            .status(User.UserStatus.ACTIVE)
            .coachCheck(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
