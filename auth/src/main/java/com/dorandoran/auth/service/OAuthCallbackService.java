package com.dorandoran.auth.service;

import com.dorandoran.auth.dto.LoginResponse;
import com.dorandoran.auth.dto.OAuthLoginRequest;
import com.dorandoran.auth.service.oauth.OAuthCodeExchangeStrategy;
import com.dorandoran.common.exception.DoranDoranException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * OAuth 콜백 처리: state 검증 → code 교환 → 기존 oauthLogin → 앱 리다이렉트 URL 생성
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OAuthCallbackService {

    private final OAuthStateRedisService oauthStateRedisService;
    private final AuthService authService;

    @Value("${oauth.app-redirect-base:dorandoran://oauth-callback}")
    private String appRedirectBase;

    @Value("${apple.oauth.redirect-uri:}")
    private String appleRedirectUri;

    @Value("${apple.oauth.service-id:}")
    private String appleServiceId;

    private final List<OAuthCodeExchangeStrategy> strategies;
    private Map<String, OAuthCodeExchangeStrategy> strategyMap;

    private Map<String, OAuthCodeExchangeStrategy> getStrategyMap() {
        if (strategyMap == null) {
            strategyMap = strategies.stream().collect(Collectors.toMap(OAuthCodeExchangeStrategy::getProvider, s -> s));
        }
        return strategyMap;
    }

    /**
     * 콜백 처리: state 소비 → code → id_token 교환 → oauthLogin → 리다이렉트 URL 반환
     *
     * @param code  authorization code
     * @param state state (Redis에서 1회 소비)
     * @return 성공 시 앱 리다이렉트 URL(토큰 포함), 실패 시 에러 쿼리 포함 URL
     */
    public OAuthCallbackResult handleCallback(String code, String state) {
        String redirectBase = appRedirectBase != null ? appRedirectBase : "dorandoran://oauth-callback";
        String sep = redirectBase.contains("?") ? "&" : "?";

        if (code == null || code.isBlank()) {
            log.warn("OAuth callback: code 없음");
            return OAuthCallbackResult.error(redirectBase + sep + "error=" + encode("missing_code"));
        }

        Optional<OAuthStateRedisService.OAuthStateResult> stateOpt = oauthStateRedisService.consumeState(state);
        if (stateOpt.isEmpty()) {
            log.warn("OAuth callback: state 없음 또는 만료");
            return OAuthCallbackResult.error(redirectBase + sep + "error=" + encode("invalid_state"));
        }

        OAuthStateRedisService.OAuthStateResult stateResult = stateOpt.get();
        String provider = stateResult.provider();
        OAuthCodeExchangeStrategy strategy = getStrategyMap().get(provider != null ? provider.toLowerCase() : null);
        if (strategy == null) {
            log.warn("OAuth callback: 지원하지 않는 provider={}", provider);
            return OAuthCallbackResult.error(redirectBase + sep + "error=" + encode("unsupported_provider"));
        }

        String redirectUriForExchange = "apple".equalsIgnoreCase(provider) ? appleRedirectUri : null;

        String idToken;
        try {
            idToken = strategy.exchangeCodeForIdToken(code, redirectUriForExchange);
        } catch (Exception e) {
            log.error("OAuth code 교환 실패: provider={}", provider, e);
            return OAuthCallbackResult.error(redirectBase + sep + "error=" + encode("token_exchange_failed"));
        }

        LoginResponse loginResponse;
        try {
            loginResponse = authService.oauthLogin(new OAuthLoginRequest(provider, idToken, true, null));  // 콜백 플로우는 즉시 가입(confirmSignup), birthDate 미전달
        } catch (DoranDoranException e) {
            log.error("OAuth 로그인 실패: provider={}, code={}", provider, e.getErrorCode(), e);
            return OAuthCallbackResult.error(redirectBase + sep + "error=" + encode(e.getMessage()) + "&errorCode=" + encode(e.getErrorCode().getCode()));
        } catch (Exception e) {
            log.error("OAuth 로그인 중 오류: provider={}", provider, e);
            return OAuthCallbackResult.error(redirectBase + sep + "error=" + encode("login_failed"));
        }

        String successUrl = redirectBase + sep
                + "accessToken=" + encode(loginResponse.getAccessToken())
                + "&refreshToken=" + encode(loginResponse.getRefreshToken())
                + "&tokenType=" + encode(loginResponse.getTokenType() != null ? loginResponse.getTokenType() : "Bearer")
                + "&expiresIn=" + (loginResponse.getExpiresIn() != null ? loginResponse.getExpiresIn() : 3600);
        return OAuthCallbackResult.success(successUrl);
    }

    private static String encode(String value) {
        if (value == null) return "";
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Authorization URL 발급 (state 생성 후 Apple 로그인 페이지 URL 반환)
     *
     * @param provider          "apple" 지원
     * @param redirectUriAfter  로그인 성공 후 리다이렉트할 URI (선택, state에 저장)
     * @return authorizationUrl, state. 지원하지 않는 provider 또는 설정 없으면 empty
     */
    public Optional<AuthorizeUrlResult> buildAuthorizeUrl(String provider, String redirectUriAfter) {
        if (provider == null || !"apple".equalsIgnoreCase(provider)) {
            return Optional.empty();
        }
        if (appleServiceId == null || appleServiceId.isBlank() || appleRedirectUri == null || appleRedirectUri.isBlank()) {
            log.warn("Apple OAuth 웹 플로우 설정 없음 (service-id, redirect-uri)");
            return Optional.empty();
        }
        String state = oauthStateRedisService.createState("apple", redirectUriAfter);
        String url = "https://appleid.apple.com/auth/authorize"
                + "?client_id=" + encode(appleServiceId)
                + "&redirect_uri=" + encode(appleRedirectUri)
                + "&response_type=code"
                + "&scope=" + encode("name email")
                + "&state=" + encode(state)
                + "&response_mode=query";
        return Optional.of(new AuthorizeUrlResult(url, state));
    }

    public record AuthorizeUrlResult(String authorizationUrl, String state) {
    }

    public record OAuthCallbackResult(boolean success, String redirectUrl) {
        static OAuthCallbackResult success(String redirectUrl) {
            return new OAuthCallbackResult(true, redirectUrl);
        }

        static OAuthCallbackResult error(String redirectUrl) {
            return new OAuthCallbackResult(false, redirectUrl);
        }
    }
}
