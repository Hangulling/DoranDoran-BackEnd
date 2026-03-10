package com.dorandoran.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth 콜백 CSRF 방지용 state 발급·검증 (Redis 기반)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OAuthStateRedisService {

    private static final String KEY_PREFIX = "oauth:state:";
    private static final int STATE_EXPIRATION_MINUTES = 10;
    private static final String VALUE_SEPARATOR = "|";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * state 생성 후 Redis에 저장 (TTL 10분)
     *
     * @param provider         provider 식별자 (예: "apple")
     * @param redirectUriAfter 로그인 성공 후 리다이렉트할 URI (선택, null 가능)
     * @return 발급된 state 값 (authorize URL의 state 파라미터로 사용)
     */
    public String createState(String provider, String redirectUriAfter) {
        String state = UUID.randomUUID().toString();
        String value = redirectUriAfter != null && !redirectUriAfter.isBlank()
                ? provider + VALUE_SEPARATOR + redirectUriAfter
                : provider;
        String key = KEY_PREFIX + state;
        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(STATE_EXPIRATION_MINUTES));
        log.debug("OAuth state 생성: provider={}, state={}", provider, state);
        return state;
    }

    /**
     * state 검증 후 1회 사용으로 삭제
     *
     * @param state 콜백 쿼리의 state 값
     * @return 저장된 provider(및 선택 redirect_uri_after). 없거나 만료 시 empty
     */
    public Optional<OAuthStateResult> consumeState(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        String key = KEY_PREFIX + state;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            log.warn("OAuth state 없음 또는 만료: state={}", state);
            return Optional.empty();
        }
        redisTemplate.delete(key);
        String provider = value;
        String redirectUriAfter = null;
        int sep = value.indexOf(VALUE_SEPARATOR);
        if (sep > 0) {
            provider = value.substring(0, sep);
            redirectUriAfter = value.substring(sep + 1);
        }
        log.debug("OAuth state 소비: provider={}", provider);
        return Optional.of(new OAuthStateResult(provider, redirectUriAfter));
    }

    /**
     * consumeState 결과 (provider, 선택 redirect_uri_after)
     */
    public record OAuthStateResult(String provider, String redirectUriAfter) {
    }
}
