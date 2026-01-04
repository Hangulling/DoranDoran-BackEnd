# 모바일 앱 배포 시 백엔드 작업 요구사항

> DoranDoran 프로젝트를 모바일 앱으로 배포할 때 백엔드에서 필요한 작업 목록

## 📋 목차

1. [현재 백엔드 상태](#현재-백엔드-상태)
2. [필수 작업](#필수-작업)
3. [선택적 작업](#선택적-작업)
4. [작업 우선순위](#작업-우선순위)
5. [예상 작업 시간](#예상-작업-시간)

---

## 현재 백엔드 상태

### ✅ 이미 구현된 기능

1. **REST API**: 모든 주요 기능이 REST API로 구현됨
2. **JWT 인증**: Access Token + Refresh Token 패턴 구현
3. **SSE 스트리밍**: 실시간 채팅을 위한 SSE 구현 완료
4. **CORS 설정**: 웹 도메인에 대한 CORS 설정 완료
5. **파일 업로드**: 10MB 제한으로 파일 업로드 지원
6. **마이크로서비스 아키텍처**: Gateway를 통한 통합 API 제공

### 현재 CORS 설정

```java:gateway/src/main/java/com/dorandoran/gateway/config/SecurityConfig.java
CorsConfiguration corsConfig = new CorsConfiguration();
corsConfig.setAllowCredentials(true);

// 로컬 개발 환경
corsConfig.addAllowedOrigin("http://localhost:3000");
corsConfig.addAllowedOrigin("http://localhost:3001");

// 프로덕션 도메인
corsConfig.addAllowedOrigin("https://doran-chat.com");
corsConfig.addAllowedOrigin("https://www.doran-chat.com");
corsConfig.addAllowedOrigin("https://doran-chat.vercel.app");
```

---

## 필수 작업

### 1. CORS 설정 조정 ⚠️ 중요

**문제점**: 모바일 앱은 웹과 달리 Origin 헤더가 없거나 `file://` 프로토콜을 사용할 수 있습니다.

**해결 방법**:

#### 1.1 모바일 앱용 Origin 허용 추가

```java
// gateway/src/main/java/com/dorandoran/gateway/config/SecurityConfig.java
@Bean
public CorsWebFilter corsWebFilter() {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.setAllowCredentials(true);

    // 기존 웹 도메인
    corsConfig.addAllowedOrigin("http://localhost:3000");
    corsConfig.addAllowedOrigin("https://doran-chat.com");
    // ... 기존 설정 ...

    // 모바일 앱용 설정
    // Capacitor: capacitor://localhost 또는 http://localhost
    corsConfig.addAllowedOriginPattern("capacitor://*");
    corsConfig.addAllowedOriginPattern("http://localhost:*");
    
    // React Native: 개발 환경
    corsConfig.addAllowedOriginPattern("http://*");
    
    // Flutter: 개발 환경
    corsConfig.addAllowedOriginPattern("http://*");
    
    // 모든 Origin 허용 (개발 환경용, 프로덕션에서는 제거)
    // corsConfig.addAllowedOriginPattern("*");

    corsConfig.addAllowedHeader("*");
    corsConfig.addAllowedMethod("*");
    corsConfig.addExposedHeader("*");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);

    return new CorsWebFilter(source);
}
```

#### 1.2 User-Agent 기반 분기 처리 (선택적)

모바일 앱 요청을 식별하기 위해 User-Agent 헤더를 확인할 수 있습니다.

```java
// gateway/src/main/java/com/dorandoran/gateway/filter/MobileAppFilter.java
@Component
public class MobileAppFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        
        if (userAgent != null && (userAgent.contains("Capacitor") || 
                                  userAgent.contains("ReactNative") || 
                                  userAgent.contains("Flutter"))) {
            // 모바일 앱 요청으로 식별
            exchange.getAttributes().put("isMobileApp", true);
        }
        
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return -100;
    }
}
```

**예상 작업 시간**: 2-3시간

---

### 2. SSE 호환성 확인 및 최적화

**현재 상태**: SSE는 이미 구현되어 있지만, 모바일 환경에서의 동작을 확인해야 합니다.

#### 2.1 모바일 환경 테스트

- [ ] iOS Safari에서 SSE 연결 테스트
- [ ] Android Chrome에서 SSE 연결 테스트
- [ ] 백그라운드/포그라운드 전환 시 연결 유지 확인
- [ ] 네트워크 전환 시 재연결 확인

#### 2.2 타임아웃 설정 조정

```yaml
# gateway/src/main/resources/application.yml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 45000
        response-timeout: 600s  # SSE용 10분 (모바일에서는 더 길게 설정 가능)
```

#### 2.3 Keep-Alive 설정

```java
// chat/src/main/java/com/dorandoran/chat/controller/SSEController.java
return ResponseEntity.ok()
    .header("Cache-Control", "no-cache")
    .header("Connection", "keep-alive")
    .header("X-Accel-Buffering", "no")
    .header("Keep-Alive", "timeout=600")  // 모바일용 추가
    .body(emitter);
```

**예상 작업 시간**: 1-2일 (테스트 포함)

---

### 3. 인증/인가 검증

**현재 상태**: JWT 기반 인증이 구현되어 있음. 모바일 앱에서도 동일하게 작동해야 함.

#### 3.1 토큰 저장 방식 확인

모바일 앱은 웹과 달리 Secure Storage를 사용합니다:
- iOS: Keychain
- Android: EncryptedSharedPreferences

백엔드는 변경 불필요하지만, 클라이언트 측 토큰 관리 방식을 확인해야 합니다.

#### 3.2 Refresh Token 자동 갱신

모바일 앱은 백그라운드에서도 토큰을 갱신할 수 있어야 합니다.

**현재 API**: `POST /api/auth/refresh` - 이미 구현됨 ✅

**예상 작업 시간**: 검증만 필요 (0일)

---

## 선택적 작업

### 4. 푸시 알림 API 추가 🔔

**필요성**: 모바일 앱의 핵심 기능 중 하나입니다.

#### 4.1 FCM (Firebase Cloud Messaging) 통합

**새로운 서비스 또는 기존 서비스에 추가**:

```java
// 새로운 서비스: notification-service 또는 기존 auth-service에 추가

// 1. FCM 토큰 등록 API
@PostMapping("/api/notifications/register")
public ResponseEntity<Void> registerFcmToken(
    @RequestHeader("X-User-Id") UUID userId,
    @RequestBody FcmTokenRequest request
) {
    fcmTokenService.saveToken(userId, request.getToken(), request.getPlatform());
    return ResponseEntity.ok().build();
}

// 2. 푸시 알림 발송 API (내부용)
@PostMapping("/api/notifications/send")
public ResponseEntity<Void> sendPushNotification(
    @RequestBody PushNotificationRequest request
) {
    fcmService.sendNotification(request.getUserId(), request.getTitle(), request.getBody());
    return ResponseEntity.ok().build();
}
```

**필요한 의존성**:
```gradle
implementation 'com.google.firebase:firebase-admin:9.2.0'
```

**데이터베이스 스키마**:
```sql
CREATE TABLE fcm_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token TEXT NOT NULL,
    platform VARCHAR(20) NOT NULL, -- 'ios' or 'android'
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, token)
);
```

**예상 작업 시간**: 3-5일

---

### 5. 앱 버전 관리 API 📱

**필요성**: 강제 업데이트, 선택적 업데이트 안내 등에 사용.

#### 5.1 버전 체크 API

```java
// gateway 또는 새로운 service에 추가

@GetMapping("/api/app/version/check")
public ResponseEntity<AppVersionResponse> checkAppVersion(
    @RequestParam String platform,  // 'ios' or 'android'
    @RequestParam String currentVersion
) {
    AppVersionInfo latestVersion = appVersionService.getLatestVersion(platform);
    
    boolean needsUpdate = versionComparator.isUpdateRequired(
        currentVersion, 
        latestVersion.getVersion()
    );
    
    return ResponseEntity.ok(AppVersionResponse.builder()
        .latestVersion(latestVersion.getVersion())
        .minimumVersion(latestVersion.getMinimumVersion())
        .needsUpdate(needsUpdate)
        .forceUpdate(needsUpdate && versionComparator.isForceUpdate(
            currentVersion, 
            latestVersion.getMinimumVersion()
        ))
        .updateUrl(latestVersion.getUpdateUrl())
        .releaseNotes(latestVersion.getReleaseNotes())
        .build());
}
```

**데이터베이스 스키마**:
```sql
CREATE TABLE app_versions (
    id UUID PRIMARY KEY,
    platform VARCHAR(20) NOT NULL, -- 'ios' or 'android'
    version VARCHAR(20) NOT NULL,
    minimum_version VARCHAR(20) NOT NULL, -- 강제 업데이트 기준
    update_url TEXT,
    release_notes TEXT,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(platform, version)
);
```

**예상 작업 시간**: 1-2일

---

### 6. 딥링크 처리 🔗

**필요성**: 앱 내 특정 화면으로 직접 이동 (예: 채팅방 열기, 표현 보관함 열기)

#### 6.1 딥링크 라우팅 API

```java
@GetMapping("/api/deeplink/route")
public ResponseEntity<DeeplinkResponse> routeDeeplink(
    @RequestParam String path,
    @RequestHeader("X-User-Id") UUID userId
) {
    // path 예시: /chatroom/{chatroomId}, /archive/{storeId}
    DeeplinkInfo info = deeplinkService.parsePath(path, userId);
    
    return ResponseEntity.ok(DeeplinkResponse.builder()
        .screen(info.getScreen())
        .params(info.getParams())
        .build());
}
```

**예상 작업 시간**: 1일

---

### 7. 파일 업로드 최적화 📸

**현재 상태**: 10MB 제한으로 파일 업로드 지원

#### 7.1 이미지 리사이징 및 압축

```java
// chat 또는 새로운 service에 추가

@PostMapping("/api/files/upload/image")
public ResponseEntity<ImageUploadResponse> uploadImage(
    @RequestParam("file") MultipartFile file,
    @RequestHeader("X-User-Id") UUID userId
) {
    // 1. 이미지 리사이징 (최대 1920x1920)
    BufferedImage resizedImage = imageService.resizeImage(file, 1920, 1920);
    
    // 2. 이미지 압축 (JPEG 품질 85%)
    byte[] compressedImage = imageService.compressImage(resizedImage, 0.85f);
    
    // 3. S3 또는 로컬 스토리지에 저장
    String imageUrl = storageService.uploadImage(compressedImage, userId);
    
    return ResponseEntity.ok(ImageUploadResponse.builder()
        .url(imageUrl)
        .size(compressedImage.length)
        .originalSize(file.getSize())
        .build());
}
```

**필요한 의존성**:
```gradle
implementation 'org.imgscalr:imgscalr-lib:4.2'
```

**예상 작업 시간**: 2-3일

---

### 8. 모바일 특화 최적화 ⚡

#### 8.1 응답 압축

```yaml
# gateway/src/main/resources/application.yml
spring:
  cloud:
    gateway:
      httpclient:
        compression:
          enabled: true
          mime-types: application/json,application/xml,text/html,text/xml,text/plain
          min-response-size: 1024  # 1KB 이상만 압축
```

#### 8.2 캐싱 헤더 최적화

```java
// Gateway 필터에서 모바일 앱 요청에 대한 캐싱 헤더 추가
@Component
public class MobileCacheFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();
            
            // 정적 리소스에 대한 캐싱
            String path = exchange.getRequest().getURI().getPath();
            if (path.startsWith("/api/chat/rooms") || path.startsWith("/api/users")) {
                headers.add("Cache-Control", "private, max-age=300"); // 5분
            }
        }));
    }
}
```

#### 8.3 배치 요청 API (선택적)

모바일 앱은 여러 API를 한 번에 호출하는 것이 효율적일 수 있습니다.

```java
@PostMapping("/api/batch")
public ResponseEntity<BatchResponse> batchRequest(
    @RequestBody BatchRequest request,
    @RequestHeader("X-User-Id") UUID userId
) {
    // 여러 API 요청을 배치로 처리
    List<BatchResponseItem> results = batchService.processBatch(request.getRequests(), userId);
    
    return ResponseEntity.ok(BatchResponse.builder()
        .results(results)
        .build());
}
```

**예상 작업 시간**: 1-2일

---

### 9. 모니터링 및 분석 📊

#### 9.1 모바일 앱 메트릭 추가

```java
// Gateway 또는 각 서비스에 추가

@Component
public class MobileMetricsFilter implements GlobalFilter {
    
    private final MeterRegistry meterRegistry;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        String platform = extractPlatform(userAgent); // 'ios', 'android', 'web'
        
        return chain.filter(exchange).doOnSuccess(v -> {
            Counter.builder("http.requests")
                .tag("platform", platform)
                .tag("status", String.valueOf(exchange.getResponse().getStatusCode().value()))
                .register(meterRegistry)
                .increment();
        });
    }
}
```

**예상 작업 시간**: 1일

---

## 작업 우선순위

### Phase 1: 필수 작업 (즉시 필요)
1. ✅ **CORS 설정 조정** - 모바일 앱이 API를 호출할 수 있도록
2. ✅ **SSE 호환성 확인** - 실시간 채팅이 모바일에서 작동하는지 확인

**예상 시간**: 1-2일

### Phase 2: 중요 작업 (출시 전)
3. ⚠️ **푸시 알림 API** - 모바일 앱의 핵심 기능
4. ⚠️ **파일 업로드 최적화** - 이미지 리사이징 및 압축

**예상 시간**: 5-8일

### Phase 3: 선택적 작업 (출시 후)
5. 📱 **앱 버전 관리 API** - 강제 업데이트 등
6. 🔗 **딥링크 처리** - 사용자 경험 개선
7. ⚡ **모바일 특화 최적화** - 성능 개선
8. 📊 **모니터링 및 분석** - 운영 데이터 수집

**예상 시간**: 3-5일

---

## 예상 작업 시간 요약

| 작업 | 우선순위 | 예상 시간 | 비고 |
|------|---------|----------|------|
| CORS 설정 조정 | 필수 | 2-3시간 | 즉시 필요 |
| SSE 호환성 확인 | 필수 | 1-2일 | 테스트 포함 |
| 푸시 알림 API | 중요 | 3-5일 | FCM 통합 |
| 파일 업로드 최적화 | 중요 | 2-3일 | 이미지 처리 |
| 앱 버전 관리 API | 선택 | 1-2일 | - |
| 딥링크 처리 | 선택 | 1일 | - |
| 모바일 최적화 | 선택 | 1-2일 | 캐싱, 압축 |
| 모니터링 추가 | 선택 | 1일 | - |

**총 예상 시간**:
- **최소 (필수만)**: 1-2일
- **권장 (필수 + 중요)**: 6-10일
- **완전 (모든 작업)**: 10-15일

---

## 결론

### 백엔드 작업량 평가

**결론: 백엔드 작업은 최소한입니다!** ✅

1. **기존 API 재사용 가능**: 대부분의 기능이 이미 REST API로 구현되어 있어 모바일 앱에서 그대로 사용 가능
2. **필수 작업은 적음**: CORS 설정 조정과 SSE 호환성 확인만 하면 기본 동작 가능
3. **선택적 작업은 점진적 추가**: 푸시 알림, 파일 최적화 등은 출시 후에도 추가 가능

### 권장 접근 방법

1. **Phase 1 완료 후 프로토타입 개발**: CORS와 SSE만 확인하고 모바일 앱 프로토타입 개발 시작
2. **Phase 2는 병렬 진행**: 모바일 앱 개발과 동시에 푸시 알림, 파일 최적화 작업
3. **Phase 3는 출시 후**: 사용자 피드백을 받은 후 선택적 기능 추가

### 팀 역할 분담

- **백엔드 개발자**: CORS 설정, SSE 확인, 푸시 알림 API (총 1-2주)
- **프론트엔드 개발자**: 모바일 앱 개발 (선택한 기술 스택에 따라 1-4주)
- **공동 작업**: API 테스트, 통합 테스트

---

**문서 작성일**: 2025-01-XX  
**작성자**: AI Assistant  
**검토 필요**: 백엔드 개발자, 팀 리더


