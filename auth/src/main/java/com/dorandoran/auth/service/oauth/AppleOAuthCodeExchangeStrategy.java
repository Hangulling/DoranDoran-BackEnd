package com.dorandoran.auth.service.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Apple Sign in with Apple - Authorization Code를 id_token으로 교환
 * @see <a href="https://developer.apple.com/documentation/sign_in_with_apple/generating_the_client_secret">Generating the client_secret</a>
 */
@Component
@Slf4j
public class AppleOAuthCodeExchangeStrategy implements OAuthCodeExchangeStrategy {

    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String APPLE_AUD = "https://appleid.apple.com";
    private static final int CLIENT_SECRET_EXPIRATION_MINUTES = 5;

    @Value("${apple.oauth.service-id:}")
    private String serviceId;

    @Value("${apple.oauth.team-id:}")
    private String teamId;

    @Value("${apple.oauth.key-id:}")
    private String keyId;

    @Value("${apple.oauth.private-key:}")
    private String privateKeyPem;

    @Value("${apple.oauth.redirect-uri:}")
    private String redirectUri;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Override
    public String getProvider() {
        return "apple";
    }

    @Override
    public String exchangeCodeForIdToken(String code, String redirectUriUsed) {
        if (serviceId == null || serviceId.isBlank() || teamId == null || teamId.isBlank()
                || keyId == null || keyId.isBlank() || privateKeyPem == null || privateKeyPem.isBlank()) {
            throw new IllegalStateException("Apple OAuth 웹 플로우 설정이 없습니다. (service-id, team-id, key-id, private-key)");
        }
        String uri = redirectUriUsed != null && !redirectUriUsed.isBlank() ? redirectUriUsed : redirectUri;
        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("Apple OAuth redirect_uri가 설정되지 않았습니다.");
        }

        String clientSecret = buildClientSecret();
        String form = "client_id=" + URLEncoder.encode(serviceId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(APPLE_TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                log.error("Apple token 교환 실패: status={}, body={}", response.statusCode(), response.body());
                throw new IllegalArgumentException("Apple authorization code 교환에 실패했습니다. status=" + response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode idTokenNode = json.get("id_token");
            if (idTokenNode == null || !idTokenNode.isTextual()) {
                log.error("Apple token 응답에 id_token 없음: {}", response.body());
                throw new IllegalArgumentException("Apple 응답에 id_token이 없습니다.");
            }
            return idTokenNode.asText();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Apple token 교환 중 오류", e);
            throw new RuntimeException("Apple authorization code 교환 중 오류: " + e.getMessage(), e);
        }
    }

    /**
     * Apple 규격 client_secret JWT 생성 (ES256, .p8 private key)
     */
    private String buildClientSecret() {
        try {
            PrivateKey key = parseP8PrivateKey(privateKeyPem);
            long now = System.currentTimeMillis() / 1000;
            long exp = now + TimeUnit.MINUTES.toSeconds(CLIENT_SECRET_EXPIRATION_MINUTES);

            return Jwts.builder()
                    .setHeaderParam("kid", keyId)
                    .setIssuer(teamId)
                    .setAudience(APPLE_AUD)
                    .setSubject(serviceId)
                    .setIssuedAt(new java.util.Date(TimeUnit.SECONDS.toMillis(now)))
                    .setExpiration(new java.util.Date(TimeUnit.SECONDS.toMillis(exp)))
                    .signWith(key, SignatureAlgorithm.ES256)
                    .compact();
        } catch (Exception e) {
            log.error("Apple client_secret JWT 생성 실패", e);
            throw new RuntimeException("Apple client_secret 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * PEM 문자열에서 EC Private Key 로드 (PKCS#8)
     */
    private static PrivateKey parseP8PrivateKey(String pem) throws Exception {
        String content = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN EC PRIVATE KEY-----", "")
                .replace("-----END EC PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(content);
        // Apple .p8 is PKCS#8; if EC PRIVATE KEY (SEC1), would need BouncyCastle
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePrivate(spec);
    }
}
