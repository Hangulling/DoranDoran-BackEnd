package com.dorandoran.user.integration;

import com.dorandoran.shared.dto.CreateUserRequest;
import com.dorandoran.shared.dto.UpdateUserRequest;
import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User Service 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void 사용자_생성_및_이벤트_발행_테스트() {
        // Given
        CreateUserRequest request = new CreateUserRequest(
            "integration@example.com",
            "김",
            "철수",
            "김철수",
            "password123",
            "profile.jpg",
            "통합 테스트 사용자"
        );

        // When
        UserDto createdUser = userService.createUser(request);

        // Then
        assertNotNull(createdUser);
        assertEquals("integration@example.com", createdUser.email());
        assertEquals("김철수", createdUser.name());
        assertEquals(UserDto.UserStatus.ACTIVE, createdUser.status());
    }

    @Test
    void 사용자_업데이트_및_이벤트_발행_테스트() {
        // Given
        CreateUserRequest createRequest = new CreateUserRequest(
            "update@example.com",
            "이",
            "영희",
            "이영희",
            "password123",
            "profile.jpg",
            "업데이트 테스트 사용자"
        );
        UserDto createdUser = userService.createUser(createRequest);

        UpdateUserRequest updateRequest = new UpdateUserRequest(
            "updated@example.com",
            "박",
            "민수",
            "박민수",
            "new-profile.jpg",
            "수정된 정보",
            UserDto.UserStatus.ACTIVE,
            true
        );

        // When
        UserDto updatedUser = userService.updateUser(createdUser.id(), updateRequest);

        // Then
        assertNotNull(updatedUser);
        assertEquals("updated@example.com", updatedUser.email());
        assertEquals("박민수", updatedUser.name());
        assertEquals("수정된 정보", updatedUser.info());
        assertTrue(updatedUser.coachCheck());
    }

    @Test
    void 사용자_상태_변경_및_이벤트_발행_테스트() {
        // Given
        CreateUserRequest createRequest = new CreateUserRequest(
            "status@example.com",
            "최",
            "지영",
            "최지영",
            "password123",
            "profile.jpg",
            "상태 변경 테스트 사용자"
        );
        UserDto createdUser = userService.createUser(createRequest);

        // When
        UserDto updatedUser = userService.updateUserStatus(createdUser.id(), UserDto.UserStatus.INACTIVE);

        // Then
        assertNotNull(updatedUser);
        assertEquals(UserDto.UserStatus.INACTIVE, updatedUser.status());
    }

    @Test
    void 사용자_조회_테스트() {
        // Given
        CreateUserRequest createRequest = new CreateUserRequest(
            "find@example.com",
            "정",
            "수진",
            "정수진",
            "password123",
            "profile.jpg",
            "조회 테스트 사용자"
        );
        UserDto createdUser = userService.createUser(createRequest);

        // When
        UserDto foundUser = userService.findById(createdUser.id());
        UserDto foundByEmail = userService.findByEmail(createdUser.email());

        // Then
        assertNotNull(foundUser);
        assertEquals(createdUser.id(), foundUser.id());
        assertEquals(createdUser.email(), foundUser.email());

        assertNotNull(foundByEmail);
        assertEquals(createdUser.id(), foundByEmail.id());
        assertEquals(createdUser.email(), foundByEmail.email());
    }

    @Test
    void 사용자_삭제_테스트() {
        // Given
        CreateUserRequest createRequest = new CreateUserRequest(
            "delete@example.com",
            "한",
            "민호",
            "한민호",
            "password123",
            "profile.jpg",
            "삭제 테스트 사용자"
        );
        UserDto createdUser = userService.createUser(createRequest);

        // When
        userService.deleteUser(createdUser.id());

        // Then
        UserDto deletedUser = userService.findById(createdUser.id());
        assertEquals(UserDto.UserStatus.INACTIVE, deletedUser.status());
    }
}
