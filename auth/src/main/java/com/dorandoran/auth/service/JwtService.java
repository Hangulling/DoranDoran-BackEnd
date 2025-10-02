package com.dorandoran.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 토큰 서비스
 */
@Service
@Slf4j
public class JwtService {
    
    @Value("${application.security.jwt.secret-key:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;
    
    @Value("${application.security.jwt.access-token-expiration:3600000}") // 1시간
    private long jwtExpiration;
    
    @Value("${application.security.jwt.refresh-token-expiration:604800000}") // 7일
    private long refreshExpiration;
    
    /**
     * 액세스 토큰 생성
     */
    public String generateAccessToken(String userId, String email, String name) {
        return generateToken(new HashMap<>(), userId, email, name, jwtExpiration);
    }
    
    /**
     * 리프레시 토큰 생성
     */
    public String generateRefreshToken(String userId, String email, String name) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return generateToken(claims, userId, email, name, refreshExpiration);
    }
    
    /**
     * 토큰 생성
     */
    private String generateToken(Map<String, Object> extraClaims, String userId, String email, String name, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userId)
                .claim("email", email)
                .claim("name", name)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * 토큰에서 사용자 ID 추출
     */
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * 토큰에서 이메일 추출
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }
    
    /**
     * 토큰에서 이름 추출
     */
    public String extractName(String token) {
        return extractClaim(token, claims -> claims.get("name", String.class));
    }
    
    /**
     * 토큰에서 클레임 추출
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * 토큰 유효성 검사
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 토큰 만료 여부 확인
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * 토큰 만료 시간 추출
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * 모든 클레임 추출
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 서명 키 생성
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
