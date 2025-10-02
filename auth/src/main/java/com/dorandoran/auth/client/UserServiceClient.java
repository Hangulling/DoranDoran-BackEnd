package com.dorandoran.auth.client;

import com.dorandoran.shared.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * User Service Feign 클라이언트
 */
@FeignClient(
    name = "user-service",
    url = "${user.service.url}",
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {
    
    /**
     * 사용자 ID로 조회
     */
    @GetMapping("/api/users/{userId}")
    UserDto getUserById(@PathVariable("userId") String userId);
    
    /**
     * 이메일로 사용자 조회
     */
    @GetMapping("/api/users/email/{email}")
    UserDto getUserByEmail(@PathVariable("email") String email);
    
    /**
     * 사용자 서비스 헬스체크
     */
    @GetMapping("/api/users/health")
    String healthCheck();
}