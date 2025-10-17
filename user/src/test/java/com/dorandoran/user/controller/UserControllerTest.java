package com.dorandoran.user.controller;

import com.dorandoran.shared.dto.CreateUserRequest;
import com.dorandoran.shared.dto.UpdateUserRequest;
import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 테스트
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto userDto;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        
        userDto = new UserDto(
                UUID.fromString(userId),
                "test@example.com",
                "Test",
                "User",
                "Test User",
                "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi", // bcrypt hash
                "https://example.com/profile.jpg",
                "Hello World",
                null, // lastConnTime
                UserDto.UserStatus.ACTIVE,
                UserDto.RoleName.ROLE_USER,
                false, // coachCheck
                null, // createdAt
                null  // updatedAt
        );

        createUserRequest = new CreateUserRequest(
                "test@example.com",
                "Test",
                "User",
                "Test User",
                "password123",
                "https://example.com/profile.jpg",
                "Hello World"
        );

        updateUserRequest = new UpdateUserRequest(
                "test@example.com",
                "Updated",
                "Name", 
                "Updated Name",
                "https://example.com/new-profile.jpg",
                "Updated bio",
                UserDto.UserStatus.ACTIVE,
                false
        );
    }

    @Test
    @DisplayName("사용자 생성 성공 테스트")
    void createUser_Success() throws Exception {
        // Given
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(userDto);

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.phone").value("010-1234-5678"))
                .andExpect(jsonPath("$.location").value("Seoul"))
                .andExpect(jsonPath("$.gender").value("Male"))
                .andExpect(jsonPath("$.birthDate").value("1990-01-01"))
                .andExpect(jsonPath("$.job").value("Software Engineer"))
                .andExpect(jsonPath("$.info").value("Hello World"))
                .andExpect(jsonPath("$.picture").value("https://example.com/profile.jpg"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("사용자 생성 실패 테스트")
    void createUser_Failure() throws Exception {
        // Given
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("사용자 ID로 조회 성공 테스트")
    void getUserById_Success() throws Exception {
        // Given
        when(userService.findById(UUID.fromString(userId))).thenReturn(userDto);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));

        verify(userService, times(1)).findById(UUID.fromString(userId));
    }

    @Test
    @DisplayName("사용자 ID로 조회 실패 테스트")
    void getUserById_Failure() throws Exception {
        // Given
        when(userService.findById(UUID.fromString(userId)))
                .thenThrow(new RuntimeException("User not found"));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).findById(UUID.fromString(userId));
    }

    @Test
    @DisplayName("이메일로 사용자 조회 성공 테스트")
    void getUserByEmail_Success() throws Exception {
        // Given
        String email = "test@example.com";
        when(userService.findByEmail(email)).thenReturn(userDto);

        // When & Then
        mockMvc.perform(get("/api/users/email/{email}", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));

        verify(userService, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("이메일로 사용자 조회 실패 테스트")
    void getUserByEmail_Failure() throws Exception {
        // Given
        String email = "nonexistent@example.com";
        when(userService.findByEmail(email))
                .thenThrow(new RuntimeException("User not found"));

        // When & Then
        mockMvc.perform(get("/api/users/email/{email}", email))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("사용자 업데이트 성공 테스트")
    void updateUser_Success() throws Exception {
        // Given
        UserDto updatedUserDto = new UserDto(
                UUID.fromString(userId),
                "test@example.com",
                "Updated",
                "Name",
                "Updated Name",
                "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi",
                "https://example.com/new-profile.jpg",
                "Updated bio",
                null, // lastConnTime
                UserDto.UserStatus.ACTIVE,
                UserDto.RoleName.ROLE_USER,
                false, // coachCheck
                null, // createdAt
                null  // updatedAt
        );
        
        when(userService.updateUser(UUID.fromString(userId), updateUserRequest)).thenReturn(updatedUserDto);

        // When & Then
        mockMvc.perform(put("/api/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.phone").value("010-9876-5432"))
                .andExpect(jsonPath("$.location").value("Busan"))
                .andExpect(jsonPath("$.gender").value("Female"))
                .andExpect(jsonPath("$.birthDate").value("1995-05-15"))
                .andExpect(jsonPath("$.job").value("Product Manager"))
                .andExpect(jsonPath("$.info").value("Updated bio"))
                .andExpect(jsonPath("$.picture").value("https://example.com/new-profile.jpg"));

        verify(userService, times(1)).updateUser(UUID.fromString(userId), updateUserRequest);
    }

    @Test
    @DisplayName("사용자 업데이트 실패 - 잘못된 사용자 ID")
    void updateUser_Failure_InvalidUserId() throws Exception {
        // Given
        String invalidUserId = "invalid-uuid";
        when(userService.updateUser(any(), any(UpdateUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid UUID"));

        // When & Then
        mockMvc.perform(put("/api/users/{userId}", invalidUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUser(any(), any(UpdateUserRequest.class));
    }

    @Test
    @DisplayName("사용자 업데이트 실패 - 서버 오류")
    void updateUser_Failure_ServerError() throws Exception {
        // Given
        when(userService.updateUser(UUID.fromString(userId), updateUserRequest))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(put("/api/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).updateUser(UUID.fromString(userId), updateUserRequest);
    }

    @Test
    @DisplayName("사용자 상태 업데이트 성공 테스트")
    void updateUserStatus_Success() throws Exception {
        // Given
        UserDto updatedUserDto = new UserDto(
                UUID.fromString(userId),
                "test@example.com",
                "Test",
                "User",
                "Test User",
                "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi",
                "https://example.com/profile.jpg",
                "Hello World",
                null, // lastConnTime
                UserDto.UserStatus.INACTIVE,
                UserDto.RoleName.ROLE_USER,
                false, // coachCheck
                null, // createdAt
                null  // updatedAt
        );
        
        when(userService.updateUserStatus(UUID.fromString(userId), UserDto.UserStatus.INACTIVE))
                .thenReturn(updatedUserDto);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/status", userId)
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(userService, times(1)).updateUserStatus(UUID.fromString(userId), UserDto.UserStatus.INACTIVE);
    }

    @Test
    @DisplayName("사용자 상태 업데이트 실패 - 잘못된 상태")
    void updateUserStatus_Failure_InvalidStatus() throws Exception {
        // Given
        String invalidStatus = "INVALID_STATUS";

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/status", userId)
                        .param("status", invalidStatus))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserStatus(any(), any());
    }

    @Test
    @DisplayName("사용자 상태 업데이트 실패 - 잘못된 사용자 ID")
    void updateUserStatus_Failure_InvalidUserId() throws Exception {
        // Given
        String invalidUserId = "invalid-uuid";

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/status", invalidUserId)
                        .param("status", "ACTIVE"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserStatus(any(), any());
    }

    @Test
    @DisplayName("사용자 상태 업데이트 실패 - 서버 오류")
    void updateUserStatus_Failure_ServerError() throws Exception {
        // Given
        when(userService.updateUserStatus(UUID.fromString(userId), UserDto.UserStatus.ACTIVE))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/status", userId)
                        .param("status", "ACTIVE"))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).updateUserStatus(UUID.fromString(userId), UserDto.UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("헬스체크 테스트")
    void health_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("User service is running"));
    }

    @Test
    @DisplayName("사용자 생성 요청 유효성 검증 실패")
    void createUser_ValidationFailure() throws Exception {
        // Given - 잘못된 이메일 형식
        CreateUserRequest invalidRequest = new CreateUserRequest(
                "invalid-email",
                "Test",
                "User",
                "Test User",
                "password123",
                "https://example.com/profile.jpg",
                "Hello World"
        );

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }
}