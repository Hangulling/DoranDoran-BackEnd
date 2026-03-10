package com.dorandoran.auth.service.oauth;

/**
 * OAuth Authorization Code를 id_token으로 교환하는 전략
 */
public interface OAuthCodeExchangeStrategy {

    /**
     * provider 식별자 (예: "apple")
     */
    String getProvider();

    /**
     * authorization code를 id_token으로 교환
     *
     * @param code        IdP가 콜백으로 전달한 authorization code
     * @param redirectUri authorize 요청 시 사용한 redirect_uri (동일해야 함)
     * @return id_token (JWT 문자열)
     */
    String exchangeCodeForIdToken(String code, String redirectUri);
}
