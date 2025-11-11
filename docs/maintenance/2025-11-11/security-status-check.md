# 보안 상태 점검 보고서

**작성일**: 2025년 11월 11일  
**점검 항목**: API 외부 접근 차단 및 관리자 API 접근 제어

---

## 1. 질문 1: API에 외부에서 잘못 접근할 가능성은 차단되어 있는가?

### 1.1 현재 상태 분석

#### SecurityConfig 설정
```java
.pathMatchers("/api/**").permitAll()  // 모든 /api/** 경로 허용
```

#### JwtAuthFilter 설정
```java
// 제외 경로가 아닌 경우 JWT 인증 필요
if (isExcludedPath(path)) {
    return chain.filter(exchange);  // 제외 경로는 통과
}
// 제외 경로가 아니면 JWT 토큰 검증
```

#### 실제 동작

**시나리오 1: JWT 토큰 없는 외부 요청**
```
요청: GET /api/chat/chatrooms/all (JWT 토큰 없음)
↓
SecurityConfig: permitAll() → 허용
↓
JwtAuthFilter: 제외 목록에 없음 → JWT 토큰 검증 시도
↓
JWT 토큰 없음 → HTTP 401 반환
```
**결과**: ✅ 차단됨 (JwtAuthFilter가 차단)

**시나리오 2: JWT 토큰 있는 외부 요청**
```
요청: GET /api/chat/chatrooms/all (유효한 JWT 토큰 포함)
↓
SecurityConfig: permitAll() → 허용
↓
JwtAuthFilter: JWT 토큰 검증 → 통과
↓
요청 처리
```
**결과**: ✅ 허용됨 (정상 사용자)

**시나리오 3: 공개 API 요청**
```
요청: POST /api/auth/login (JWT 토큰 없음)
↓
SecurityConfig: permitAll() → 허용
↓
JwtAuthFilter: 제외 목록에 포함 → 통과
↓
요청 처리
```
**결과**: ✅ 허용됨 (의도한 동작)

### 1.2 결론

**답변**: ✅ **부분적으로 차단되어 있음**

**상세 설명**:
- ✅ **JWT 토큰 없는 요청**: JwtAuthFilter가 HTTP 401로 차단
- ✅ **공개 API**: 의도적으로 허용 (로그인, 회원가입 등)
- ⚠️ **문제점**: SecurityConfig의 `permitAll()` 설정이 혼란스러움
  - 실제로는 JwtAuthFilter가 인증을 담당
  - SecurityConfig 설정이 무의미함

**보안 수준**: 중간
- 기능적으로는 차단되지만, 설정이 일관되지 않음

---

## 2. 질문 2: 관리자만 blacklist API를 접근할 수 있게 되어 있는가?

### 2.1 현재 상태 분석

#### SecurityConfig 설정
```java
.pathMatchers("/api/**").permitAll()  // /api/admin/blacklist도 포함
```

#### JwtAuthFilter 설정
```java
// /api/admin/blacklist는 제외 목록에 없음
private boolean isExcludedPath(String path) {
    return path.startsWith("/actuator") || 
           // ... 다른 제외 경로들
           // /api/admin/blacklist는 없음
}
```

#### BlacklistController
```java
@RestController
@RequestMapping("/api/admin/blacklist")
public class BlacklistController {
    // 관리자 권한 체크 없음
    // JWT 토큰만 있으면 접근 가능
}
```

#### 실제 동작

**시나리오 1: JWT 토큰 없는 요청**
```
요청: GET /api/admin/blacklist (JWT 토큰 없음)
↓
SecurityConfig: permitAll() → 허용
↓
JwtAuthFilter: 제외 목록에 없음 → JWT 토큰 검증 시도
↓
JWT 토큰 없음 → HTTP 401 반환
```
**결과**: ✅ 차단됨

**시나리오 2: 일반 사용자 JWT 토큰**
```
요청: GET /api/admin/blacklist (일반 사용자 JWT 토큰)
↓
SecurityConfig: permitAll() → 허용
↓
JwtAuthFilter: JWT 토큰 검증 → 통과 (토큰만 유효하면 OK)
↓
BlacklistController: 권한 체크 없음 → 접근 허용
↓
블랙리스트 조회 성공
```
**결과**: ❌ **일반 사용자도 접근 가능** (보안 취약점)

**시나리오 3: 관리자 JWT 토큰 (가정)**
```
요청: GET /api/admin/blacklist (관리자 JWT 토큰)
↓
SecurityConfig: permitAll() → 허용
↓
JwtAuthFilter: JWT 토큰 검증 → 통과
↓
BlacklistController: 권한 체크 없음 → 접근 허용
↓
블랙리스트 조회 성공
```
**결과**: ✅ 접근 가능 (하지만 일반 사용자도 가능)

### 2.2 결론

**답변**: ❌ **아니요, 관리자만 접근할 수 있게 되어 있지 않습니다**

**상세 설명**:
- ✅ JWT 토큰은 필요함 (인증)
- ❌ 관리자 권한 체크가 없음 (인가)
- ❌ 일반 사용자도 JWT 토큰만 있으면 접근 가능

**보안 수준**: 낮음
- 인증은 되지만 인가가 없음
- **심각한 보안 취약점**

---

## 3. 보안 취약점 요약

### 3.1 발견된 문제점

| 문제 | 심각도 | 설명 |
|------|--------|------|
| 관리자 API 권한 체크 없음 | 🔴 높음 | 일반 사용자도 접근 가능 |
| SecurityConfig 정책 불일치 | 🟡 중간 | permitAll() 설정이 혼란스러움 |

### 3.2 보안 수준 평가

**API 외부 접근 차단**: 🟡 중간
- JWT 인증으로 차단되지만, 설정이 일관되지 않음

**관리자 API 접근 제어**: 🔴 낮음
- 인증은 되지만 인가가 없음
- 일반 사용자도 접근 가능

---

## 4. 권장 수정 사항

### 4.1 즉시 수정 필요 🔴

#### 관리자 권한 체크 추가

**BlacklistController에 권한 체크 추가**:
```java
// JWT 토큰에서 역할(role) 클레임 확인
// ROLE_ADMIN 권한이 있는 사용자만 접근 허용
```

**또는 JwtAuthFilter에서 관리자 경로 체크**:
```java
if (path.startsWith("/api/admin/")) {
    // JWT에서 role 클레임 확인
    // ROLE_ADMIN이 아니면 HTTP 403 반환
}
```

### 4.2 단기 개선 사항 🟡

#### SecurityConfig 정책 수정

```java
.pathMatchers("/api/admin/**").authenticated()  // 관리자 API는 인증 필요
.pathMatchers("/api/auth/**").permitAll()       // Auth API는 허용
.pathMatchers("/api/**").authenticated()        // 나머지는 인증 필요
```

---

## 5. 테스트 시나리오

### 5.1 현재 상태 테스트

#### 테스트 1: 일반 사용자가 관리자 API 접근
```bash
# 일반 사용자 JWT 토큰으로 시도
curl -X GET http://localhost:8080/api/admin/blacklist \
  -H "Authorization: Bearer <일반사용자_JWT_토큰>"

# 예상 결과: HTTP 200 (현재는 접근 가능) ❌
```

#### 테스트 2: JWT 토큰 없이 관리자 API 접근
```bash
curl -X GET http://localhost:8080/api/admin/blacklist

# 예상 결과: HTTP 401 ✅
```

---

## 6. 결론

### 6.1 질문 1 답변

**Q: API에 외부에서 잘못 접근할 가능성은 차단되어 있는가?**  
**A: ✅ 부분적으로 차단되어 있음**

- JWT 토큰 없는 요청은 차단됨
- 하지만 SecurityConfig 설정이 혼란스러움

### 6.2 질문 2 답변

**Q: 관리자만 blacklist API를 접근할 수 있게 되어 있는가?**  
**A: ❌ 아니요, 관리자만 접근할 수 있게 되어 있지 않습니다**

- JWT 토큰만 있으면 누구나 접근 가능
- 관리자 권한 체크가 없음
- **즉시 수정 필요**

---

**작성자**: AI Assistant  
**작성일**: 2025년 11월 11일  
**긴급 조치 필요**: 관리자 API 권한 체크 추가

