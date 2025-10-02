package com.dorandoran.user.controller;

import com.dorandoran.shared.dto.UpdateUserRequest;
import com.dorandoran.shared.dto.UserDto;
import com.dorandoran.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * User Controller 테스트
 */
@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new UserDto(
            testUserId,
            "test@example.com",
            "홍",
            "길동",
            "홍길동",
            "encodedPassword",
            "profile.jpg",
            "테스트 사용자",
            LocalDateTime.now(),
            UserDto.UserStatus.ACTIVE,
            UserDto.RoleName.ROLE_USER,
            false,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    @Test
    void 사용자_조회_성공() throws Exception {
        // Given
        when(userService.findById(testUserId)).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("홍길동"));
    }

    @Test
    void 사용자_조회_실패_사용자_없음() throws Exception {
        // Given
        when(userService.findById(any(UUID.class))).thenThrow(new RuntimeException("User not found"));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", testUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void 이메일로_사용자_조회_성공() throws Exception {
        // Given
        when(userService.findByEmail("test@example.com")).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/api/users/email/{email}", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("홍길동"));
    }

    @Test
    void 사용자_업데이트_성공() throws Exception {
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

        UserDto updatedUser = new UserDto(
            testUserId,
            "updated@example.com",
            "김",
            "철수",
            "김철수",
            "encodedPassword",
            "new-profile.jpg",
            "수정된 정보",
            LocalDateTime.now(),
            UserDto.UserStatus.ACTIVE,
            UserDto.RoleName.ROLE_USER,
            true,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(userService.updateUser(eq(testUserId), any(UpdateUserRequest.class)))
                .thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/users/{userId}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.name").value("김철수"));
    }

    @Test
    void 사용자_상태_변경_성공() throws Exception {
        // Given
        UserDto updatedUser = new UserDto(
            testUserId,
            "test@example.com",
            "홍",
            "길동",
            "홍길동",
            "encodedPassword",
            "profile.jpg",
            "테스트 사용자",
            LocalDateTime.now(),
            UserDto.UserStatus.INACTIVE,
            UserDto.RoleName.ROLE_USER,
            false,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(userService.updateUserStatus(eq(testUserId), eq(UserDto.UserStatus.INACTIVE)))
                .thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/status", testUserId)
                .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void 사용자_상태_변경_실패_잘못된_상태() throws Exception {
        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/status", testUserId)
                .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 헬스체크_성공() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("User service is running"));
    }
}
