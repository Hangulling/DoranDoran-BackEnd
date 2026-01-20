# DoranDoran 프로젝트 에러 처리 아키텍처

**작성일**: 2025-01-15  
**적용 범위**: 전체 마이크로서비스

---

## 1. 개요

DoranDoran 프로젝트는 **서비스별 독립적인 전역 예외 처리** 방식을 채택하고 있습니다. 각 마이크로서비스는 자체 `GlobalExceptionHandler`를 통해 예외를 처리하며, 통일된 에러 응답 형식을 제공합니다.

### 1.1 에러 처리 구조

```
┌─────────────────────────────────────────┐
│         Controller Layer                │
│  (@RestController, @Controller)         │
└──────────────┬──────────────────────────┘
               │
               │ Exception 발생
               ▼
┌─────────────────────────────────────────┐
│    GlobalExceptionHandler               │
│    (@RestControllerAdvice)              │
│  - 예외 타입별 핸들러                    │
│  - 통일된 ErrorResponse 생성            │
└──────────────┬──────────────────────────┘
               │
               │ ResponseEntity<ErrorResponse>
               ▼
┌─────────────────────────────────────────┐
│         Client (JSON Response)         │
└─────────────────────────────────────────┘
```

---

## 2. 서비스별 에러 처리 구현

### 2.1 Chat Service

**위치**: `chat/src/main/java/com/dorandoran/chat/exception/GlobalExceptionHandler.java`

**처리하는 예외 타입**:

1. **IllegalArgumentException** (400 Bad Request)
   - 잘못된 인수 전달
   - 예: 잘못된 파라미터, 범위를 벗어난 값

2. **IllegalStateException** (409 Conflict)
   - 잘못된 상태에서의 요청
   - 예: 이미 종료된 채팅방에 메시지 전송

3. **AccessDeniedException** (403 Forbidden)
   - 접근 권한 없음
   - 예: 다른 사용자의 채팅방 접근 시도

4. **MethodArgumentNotValidException** (400 Bad Request)
   - Bean Validation 실패
   - 필드별 검증 오류 상세 정보 제공

5. **BindException** (400 Bad Request)
   - 요청 데이터 바인딩 실패
   - 필드별 바인딩 오류 상세 정보 제공

6. **ResourceNotFoundException** (404 Not Found)
   - 리소스를 찾을 수 없음
   - 예: 존재하지 않는 채팅방, 메시지

7. **Exception** (500 Internal Server Error)
   - 예상치 못한 모든 예외
   - 최상위 예외 핸들러

**에러 응답 형식**:
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "잘못된 요청입니다",
  "path": "/api/chat/rooms/123",
  "validationErrors": {
    "fieldName": "오류 메시지"
  }
}
```

**특징**:
- `WebRequest`를 사용하여 요청 정보 추출
- 유효성 검증 오류는 `Map<String, String>` 형식으로 제공
- 타임스탬프 자동 추가

### 2.2 Store Service

**위치**: `store/src/main/java/com/dorandoran/store/exception/GlobalExceptionHandler.java`

**처리하는 예외 타입**:

1. **BookmarkNotFoundException** (404 Not Found)
   - 보관함 항목을 찾을 수 없음
   - 커스텀 에러 코드: `BOOKMARK_NOT_FOUND`

2. **DuplicateBookmarkException** (409 Conflict)
   - 중복 저장 시도
   - 커스텀 에러 코드: `DUPLICATE_BOOKMARK`

3. **UnauthorizedAccessException** (403 Forbidden)
   - 권한 없는 접근
   - 커스텀 에러 코드: `UNAUTHORIZED_ACCESS`

4. **IllegalArgumentException** (400 Bad Request)
   - 잘못된 인수
   - 커스텀 에러 코드: `INVALID_REQUEST`

5. **IllegalStateException** (400 Bad Request)
   - 잘못된 상태
   - 커스텀 에러 코드: `INVALID_STATE`

6. **MethodArgumentNotValidException** (400 Bad Request)
   - 유효성 검증 실패
   - 필드별 상세 오류 정보 제공
   - 커스텀 에러 코드: `VALIDATION_ERROR`

7. **Exception** (500 Internal Server Error)
   - 예상치 못한 모든 예외
   - ⚠️ **주의**: `detail` 필드에 예외 메시지 노출 (보안 이슈)

**에러 응답 형식**:
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 404,
  "code": "BOOKMARK_NOT_FOUND",
  "message": "보관함 항목을 찾을 수 없습니다",
  "path": "/api/store/bookmarks/123",
  "errors": [
    {
      "field": "fieldName",
      "message": "오류 메시지",
      "rejectedValue": "잘못된 값"
    }
  ]
}
```

**특징**:
- `HttpServletRequest`를 사용하여 요청 정보 추출
- 커스텀 에러 코드 제공 (`code` 필드)
- 유효성 검증 오류는 `List<FieldError>` 형식으로 제공
- `@JsonInclude(JsonInclude.Include.NON_NULL)`로 null 필드 제외

### 2.3 Auth Service & User Service

**현재 상태**: 전역 예외 핸들러 없음

**처리 방식**:
- Spring의 기본 예외 처리에 의존
- 또는 각 Controller에서 개별적으로 예외 처리

**개선 필요**:
- 통일된 에러 응답 형식을 위한 `GlobalExceptionHandler` 추가 권장

---

## 3. 커스텀 예외 클래스

### 3.1 공통 예외 (Common Module)

**DoranDoranException**:
```java
public class DoranDoranException extends RuntimeException {
    private final ErrorCode errorCode;
    // ...
}
```

**ErrorCode Enum**:
```java
public enum ErrorCode {
    // 사용자 관련
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다"),
    EMAIL_ALREADY_EXISTS("U002", "이미 존재하는 이메일입니다"),
    
    // 인증 관련
    AUTH_TOKEN_EXPIRED("A001", "인증 토큰이 만료되었습니다"),
    
    // 채팅 관련
    CHAT_ROOM_NOT_FOUND("C001", "채팅방을 찾을 수 없습니다"),
    
    // 공통
    INTERNAL_SERVER_ERROR("E001", "내부 서버 오류가 발생했습니다");
}
```

**특징**:
- 에러 코드와 메시지를 함께 관리
- 도메인별로 코드 체계 구분 (U: User, A: Auth, C: Chat, S: Store, E: Error)

### 3.2 서비스별 커스텀 예외

**Chat Service**:
- `ResourceNotFoundException`: 리소스를 찾을 수 없을 때

**Store Service**:
- `BookmarkNotFoundException`: 보관함 항목을 찾을 수 없을 때
- `DuplicateBookmarkException`: 중복 저장 시도
- `UnauthorizedAccessException`: 권한 없는 접근

---

## 4. 에러 응답 DTO 구조

### 4.1 Chat Service ErrorResponse

```java
public class ErrorResponse {
    private LocalDateTime timestamp;      // 발생 시각
    private int status;                  // HTTP 상태 코드
    private String error;                // 에러 타입 (예: "Bad Request")
    private String message;               // 에러 메시지
    private String path;                  // 요청 경로
    private Map<String, String> validationErrors;  // 유효성 검증 오류
}
```

### 4.2 Store Service ErrorResponse

```java
public class ErrorResponse {
    private LocalDateTime timestamp;      // 발생 시각
    private int status;                  // HTTP 상태 코드
    private String code;                 // 커스텀 에러 코드
    private String message;               // 에러 메시지
    private String detail;                // 상세 설명 (⚠️ 보안 이슈)
    private String path;                  // 요청 경로
    private List<FieldError> errors;      // 유효성 검증 오류
    
    public static class FieldError {
        private String field;             // 필드명
        private String message;           // 오류 메시지
        private Object rejectedValue;      // 거부된 값
    }
}
```

**차이점**:
- Chat Service: `error` 필드 사용, `Map` 형식의 validationErrors
- Store Service: `code` 필드 사용, `List<FieldError>` 형식의 errors, `detail` 필드 추가

---

## 5. HTTP 상태 코드 매핑

| 예외 타입 | HTTP 상태 코드 | 설명 |
|----------|--------------|------|
| `IllegalArgumentException` | 400 Bad Request | 잘못된 요청 |
| `IllegalStateException` | 409 Conflict (Chat) / 400 Bad Request (Store) | 상태 충돌 |
| `AccessDeniedException` / `UnauthorizedAccessException` | 403 Forbidden | 권한 없음 |
| `MethodArgumentNotValidException` | 400 Bad Request | 유효성 검증 실패 |
| `BindException` | 400 Bad Request | 바인딩 실패 |
| `ResourceNotFoundException` / `BookmarkNotFoundException` | 404 Not Found | 리소스 없음 |
| `DuplicateBookmarkException` | 409 Conflict | 중복 |
| `Exception` | 500 Internal Server Error | 서버 오류 |

---

## 6. 로깅 전략

### 6.1 로그 레벨

**Chat Service**:
- `IllegalArgumentException`, `IllegalStateException`, `AccessDeniedException`: `log.warn()`
- `ResourceNotFoundException`: `log.warn()`
- `Exception`: `log.error()` (스택 트레이스 포함)

**Store Service**:
- 모든 예외: `log.error()` (스택 트레이스 포함)

### 6.2 로그 내용

**포함 정보**:
- 예외 메시지
- 요청 경로
- 예외 타입

**주의사항**:
- ⚠️ Store Service의 `Exception` 핸들러에서 `detail` 필드에 예외 메시지 노출 (보안 이슈)
- 프로덕션 환경에서는 상세 정보 숨김 권장

---

## 7. 유효성 검증 오류 처리

### 7.1 Bean Validation

**사용 어노테이션**:
- `@NotBlank`, `@NotNull`, `@NotEmpty`
- `@Size`, `@Min`, `@Max`
- `@Pattern`, `@Email`

**처리 방식**:

**Chat Service**:
```java
Map<String, String> errors = new HashMap<>();
ex.getBindingResult().getAllErrors().forEach((error) -> {
    String fieldName = ((FieldError) error).getField();
    String errorMessage = error.getDefaultMessage();
    errors.put(fieldName, errorMessage);
});
```

**Store Service**:
```java
List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
    .map(fieldError -> ErrorResponse.FieldError.builder()
        .field(fieldError.getField())
        .message(fieldError.getDefaultMessage())
        .rejectedValue(fieldError.getRejectedValue())
        .build())
    .collect(Collectors.toList());
```

**차이점**:
- Chat Service: `Map<String, String>` 형식 (필드명 → 메시지)
- Store Service: `List<FieldError>` 형식 (필드명, 메시지, 거부된 값 포함)

---

## 8. Gateway 레벨 에러 처리

**현재 상태**: Gateway 서비스에는 전역 예외 핸들러가 없음

**처리 방식**:
- Spring Cloud Gateway의 기본 에러 처리
- 필터에서 발생하는 예외는 Gateway의 기본 에러 응답

**개선 필요**:
- Gateway 레벨 `GlobalExceptionHandler` 추가 권장
- 통일된 에러 응답 형식 제공

---

## 9. 문제점 및 개선 사항

### 9.1 발견된 문제점

1. **에러 응답 형식 불일치**
   - Chat Service와 Store Service의 ErrorResponse 구조가 다름
   - 클라이언트에서 일관성 없는 에러 처리 필요

2. **보안 이슈**
   - Store Service의 `Exception` 핸들러에서 `detail` 필드에 예외 메시지 노출
   - 프로덕션 환경에서 내부 구현 세부사항 노출 위험

3. **Auth/User 서비스 예외 처리 부재**
   - 전역 예외 핸들러 없음
   - 통일된 에러 응답 형식 제공 불가

4. **Gateway 레벨 예외 처리 부재**
   - Gateway에서 발생하는 예외에 대한 통일된 처리 없음

5. **ErrorCode 활용 부족**
   - `common` 모듈에 `ErrorCode` enum이 있으나 실제 사용되지 않음
   - 각 서비스가 자체 에러 코드 사용

### 9.2 개선 권장사항

#### 9.2.1 통일된 ErrorResponse 구조

**제안**:
```java
// common 모듈에 통일된 ErrorResponse 정의
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String code;              // ErrorCode enum 사용
    private String message;
    private String path;
    private List<FieldError> errors;  // 유효성 검증 오류
    // detail 필드는 프로덕션에서 제외
}
```

#### 9.2.2 ErrorCode 통합 사용

**제안**:
- 모든 서비스에서 `common` 모듈의 `ErrorCode` enum 사용
- 도메인별 코드 체계 확장 (C: Chat, S: Store 등)

#### 9.2.3 환경별 에러 메시지

**제안**:
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(
    Exception ex, 
    HttpServletRequest request,
    @Value("${spring.profiles.active}") String profile) {
    
    ErrorResponse error = ErrorResponse.builder()
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .code("INTERNAL_SERVER_ERROR")
        .message("서버 내부 오류가 발생했습니다")
        .path(request.getRequestURI())
        .build();
    
    // 개발 환경에서만 상세 정보 제공
    if ("dev".equals(profile) || "local".equals(profile)) {
        error.setDetail(ex.getMessage());
    }
    
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
}
```

#### 9.2.4 Gateway 예외 처리 추가

**제안**:
```java
@RestControllerAdvice
public class GatewayGlobalExceptionHandler {
    
    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ErrorResponse> handleConnectException(
        ConnectException ex, 
        ServerWebExchange exchange) {
        // 다운스트림 서비스 연결 실패 처리
    }
    
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeoutException(
        TimeoutException ex, 
        ServerWebExchange exchange) {
        // 타임아웃 처리
    }
}
```

#### 9.2.5 Auth/User 서비스 예외 처리 추가

**제안**:
- 각 서비스에 `GlobalExceptionHandler` 추가
- 통일된 `ErrorResponse` 사용
- `ErrorCode` enum 활용

---

## 10. 에러 처리 플로우

### 10.1 정상 플로우

```
1. Controller에서 비즈니스 로직 실행
2. Service Layer에서 예외 발생
3. GlobalExceptionHandler가 예외 캐치
4. 예외 타입에 맞는 핸들러 실행
5. ErrorResponse 생성
6. 적절한 HTTP 상태 코드와 함께 응답
```

### 10.2 유효성 검증 플로우

```
1. Controller에서 @Valid 어노테이션으로 검증
2. Bean Validation 실패
3. MethodArgumentNotValidException 발생
4. GlobalExceptionHandler가 예외 캐치
5. BindingResult에서 필드별 오류 추출
6. ErrorResponse에 validationErrors 포함
7. 400 Bad Request 응답
```

### 10.3 예상치 못한 예외 플로우

```
1. Controller/Service에서 예상치 못한 예외 발생
2. Exception 핸들러가 최종적으로 캐치
3. 로그에 스택 트레이스 기록
4. 일반적인 에러 메시지로 응답 (보안 고려)
5. 500 Internal Server Error 응답
```

---

## 11. 실제 사용 예시

### 11.1 Chat Service 예시

**예외 발생**:
```java
@GetMapping("/rooms/{roomId}")
public ResponseEntity<ChatRoom> getChatRoom(@PathVariable Long roomId) {
    ChatRoom room = chatService.getChatRoom(roomId);
    if (room == null) {
        throw new ResourceNotFoundException("채팅방을 찾을 수 없습니다: " + roomId);
    }
    return ResponseEntity.ok(room);
}
```

**에러 응답**:
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "채팅방을 찾을 수 없습니다: 123",
  "path": "/api/chat/rooms/123"
}
```

### 11.2 Store Service 예시

**예외 발생**:
```java
@PostMapping("/bookmarks")
public ResponseEntity<Bookmark> createBookmark(@Valid @RequestBody BookmarkRequest request) {
    // 중복 체크
    if (bookmarkService.exists(request.getUserId(), request.getItemId())) {
        throw new DuplicateBookmarkException("이미 저장된 항목입니다");
    }
    // ...
}
```

**에러 응답**:
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 409,
  "code": "DUPLICATE_BOOKMARK",
  "message": "이미 저장된 항목입니다",
  "path": "/api/store/bookmarks"
}
```

---

## 12. 모니터링 및 알림

### 12.1 로그 모니터링

**추적 항목**:
- 예외 발생 빈도
- 예외 타입별 통계
- 500 에러 발생 시 알림

### 12.2 메트릭 수집

**Prometheus 메트릭**:
- `http_requests_total{status="400"}`
- `http_requests_total{status="500"}`
- 예외 타입별 카운터

---

## 13. 결론

### 13.1 현재 상태

**강점**:
- ✅ 서비스별 독립적인 예외 처리
- ✅ 전역 예외 핸들러를 통한 중앙 집중식 처리
- ✅ 유효성 검증 오류 상세 정보 제공
- ✅ 적절한 HTTP 상태 코드 매핑

**약점**:
- ⚠️ 에러 응답 형식 불일치
- ⚠️ 보안 이슈 (상세 에러 메시지 노출)
- ⚠️ Auth/User/Gateway 서비스 예외 처리 부재
- ⚠️ ErrorCode 활용 부족

### 13.2 개선 우선순위

**높은 우선순위**:
1. 보안 이슈 수정 (상세 에러 메시지 숨김)
2. Auth/User 서비스 예외 처리 추가
3. 통일된 ErrorResponse 구조 정의

**중간 우선순위**:
4. ErrorCode 통합 사용
5. Gateway 예외 처리 추가
6. 환경별 에러 메시지 처리

**낮은 우선순위**:
7. 에러 응답 형식 통일
8. 모니터링 및 알림 강화

---

**문서 작성 완료**: 2025-01-15
















