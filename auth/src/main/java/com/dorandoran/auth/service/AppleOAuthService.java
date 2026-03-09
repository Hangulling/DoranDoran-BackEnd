package com.dorandoran.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Apple Sign In 서비스
 * Apple identity token(JWT) 검증 및 사용자 정보 추출
 * @see <a href="https://developer.apple.com/documentation/sign_in_with_apple">Sign in with Apple</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppleOAuthService {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    @Value("${apple.oauth.client-id:}")
    private String clientId;

    @Value("${apple.oauth.client-ids:}")
    private String clientIdsCsv;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final ConcurrentHashMap<String, PublicKey> keyCache = new ConcurrentHashMap<>();

    /**
     * Apple identity token 검증 및 사용자 정보 추출
     *
     * @param idToken Apple에서 발급한 identity token (JWT)
     * @return Apple 사용자 정보 (sub, email 등)
     */
    public AppleUserInfo verifyIdToken(String idToken) throws Exception {
        log.debug("Apple identity token 검증 시작");

        List<String> allowedAudiences = getAllowedClientIds();
        if (allowedAudiences.isEmpty()) {
            throw new IllegalStateException("Apple OAuth client-id가 설정되지 않았습니다.");
        }

        // JWT 헤더에서 kid 추출
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Apple identity token 형식이 올바르지 않습니다.");
        }
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        JsonNode header = objectMapper.readTree(headerJson);
        String kid = header.has("kid") ? header.get("kid").asText() : null;
        if (kid == null || kid.isEmpty()) {
            throw new IllegalArgumentException("Apple identity token에 kid가 없습니다.");
        }

        PublicKey publicKey = keyCache.computeIfAbsent(kid, this::fetchApplePublicKey);
        if (publicKey == null) {
            throw new IllegalArgumentException("Apple 공개키를 가져올 수 없습니다. kid=" + kid);
        }

        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(idToken)
                .getPayload();

        // iss 검증 (슬래시 유무 모두 허용: https://appleid.apple.com, https://appleid.apple.com/)
        String iss = claims.getIssuer();
        String normalizedIss = (iss != null && !iss.isEmpty()) ? iss.replaceAll("/+$", "") : iss;
        if (normalizedIss == null || !APPLE_ISSUER.equals(normalizedIss)) {
            log.error("Apple identity token iss 검증 실패: iss={}", iss);
            throw new IllegalArgumentException("Apple identity token의 발급자가 올바르지 않습니다.");
        }

        // aud 검증 (우리 앱의 bundle id 또는 service id). aud는 문자열 또는 배열일 수 있음
        String aud = extractAudience(claims);
        if (aud == null || !allowedAudiences.contains(aud)) {
            Object rawAud = claims.get("aud");
            log.error("Apple identity token aud 검증 실패: aud={}, allowed={}, rawAudType={}, rawAudValue={}",
                    aud, allowedAudiences,
                    rawAud != null ? rawAud.getClass().getName() : "null",
                    rawAud);
            throw new IllegalArgumentException("Apple identity token의 대상이 올바르지 않습니다.");
        }

        String sub = claims.getSubject();
        if (sub == null || sub.isEmpty()) {
            throw new IllegalArgumentException("Apple identity token에 sub가 없습니다.");
        }

        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            // 이메일 미제공 시(숨기기 선택) 고유 식별용 플레이스홀더 사용
            email = sub + "@apple.privaterelay";
            log.debug("Apple 이메일 미제공, 플레이스홀더 사용: sub={}", sub);
        }

        // Apple은 identity token에 이름을 넣지 않음. 최초 로그인 시 클라이언트가 별도 전달 가능(추후 확장)
        String firstName = "";
        String lastName = "";
        String name = "";

        log.info("Apple identity token 검증 성공: sub={}, email={}", sub, email);
        return new AppleUserInfo(email, firstName, lastName, name, sub);
    }

    private PublicKey fetchApplePublicKey(String kid) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(APPLE_KEYS_URL))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.error("Apple JWKS 조회 실패: status={}", response.statusCode());
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode keys = root.get("keys");
            if (keys == null || !keys.isArray()) {
                return null;
            }
            for (JsonNode key : keys) {
                if (kid.equals(key.has("kid") ? key.get("kid").asText() : null)) {
                    return jwkToPublicKey(key);
                }
            }
            log.error("Apple JWKS에서 kid에 해당하는 키 없음: kid={}", kid);
            return null;
        } catch (Exception e) {
            log.error("Apple 공개키 조회 실패: kid={}", kid, e);
            return null;
        }
    }

    private static PublicKey jwkToPublicKey(JsonNode jwk) throws Exception {
        String n = jwk.get("n").asText();
        String e = jwk.get("e").asText();
        byte[] nBytes = Base64.getUrlDecoder().decode(n);
        byte[] eBytes = Base64.getUrlDecoder().decode(e);
        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    /**
     * JWT aud 클레임 추출. JJWT/Jackson이 다양한 타입으로 역직렬화할 수 있음.
     */
    private String extractAudience(Claims claims) {
        Object audObj = claims.get("aud");
        if (audObj == null) {
            return null;
        }
        if (audObj instanceof String s) {
            return s;
        }
        if (audObj instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            return first instanceof String fs ? fs : String.valueOf(first);
        }
        if (audObj instanceof Collection<?> col && !col.isEmpty()) {
            Object first = col.iterator().next();
            return first instanceof String fs ? fs : String.valueOf(first);
        }
        return String.valueOf(audObj);
    }

    private List<String> getAllowedClientIds() {
        List<String> ids = new java.util.ArrayList<>();
        if (clientId != null && !clientId.trim().isEmpty()) {
            ids.add(clientId.trim());
        }
        if (clientIdsCsv != null && !clientIdsCsv.trim().isEmpty()) {
            for (String id : clientIdsCsv.split(",")) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty() && !ids.contains(trimmed)) {
                    ids.add(trimmed);
                }
            }
        }
        return ids;
    }

    /**
     * Apple 사용자 정보 DTO
     */
    public record AppleUserInfo(
            String email,
            String firstName,
            String lastName,
            String name,
            String sub   // Apple 사용자 고유 ID (oauthId로 사용)
    ) {
    }
}
