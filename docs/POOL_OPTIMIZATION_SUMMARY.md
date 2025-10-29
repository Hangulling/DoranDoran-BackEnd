# Connection Pool 최적화 요약

## 📊 최종 설정

| 서비스 | 풀 크기 | 사용 패턴 | DB 용도 |
|--------|--------|----------|---------|
| **Chat** | 20개 | 실시간 메시지 처리 중심 | 메시지 저장, 채팅방 정보 |
| **User** | 15개 | 사용자 정보 조회 | 사용자 CRUD |
| **Auth** | 10개 | 토큰 검증 (JWT 기반) | RefreshToken 저장 |
| **Store** | 10개 | 보관함 조회 | 보관함 CRUD |
| **Batch** | 5개 | 하루 한 번 새벽 실행 | 집계 작업 |

**총 연결**: 60개 (여유 40개)

---

## 🎯 각 서비스 역할과 DB 사용 이유

### Auth Service
- **역할**: 토큰 검증 및 RefreshToken 관리
- **DB 사용**: 
  - RefreshToken 저장 (로그아웃 시 블랙리스트)
  - 토큰 로테이션 추적
- **Redis 활용**: 토큰 블랙리스트 캐싱
- **풀 10개 충분한 이유**: 토큰 검증은 JWT 기반, DB는 RefreshToken 저장만

### User Service  
- **역할**: 사용자 정보 관리 (CRUD)
- **DB 사용**: 
  - 사용자 정보 조회
  - 프로필 수정
- **Redis 활용**: 사용자 정보 캐싱 (TTL: 10분)
- **풀 15개 이유**: 사용자 조회가 빈번하지만 캐싱으로 DB 부하 감소

### Store Service
- **역할**: 보관함 기능 (표현 저장)
- **DB 사용**: 
  - 보관함 조회 (페이징)
  - 보관함 저장/삭제
- **Redis 활용**: 보관함 목록 캐싱 (TTL: 5분)
- **풀 10개 이유**: 조회 중심, 캐싱으로 실질적 DB 부하 낮음

### Batch Service
- **역할**: 새벽 배치 작업
- **DB 사용**: 
  - 사용량 통계 집계
  - 데이터 정리
- **풀 5개 (min-idle: 1)**: 하루 한 번 실행, 평소 유휴 연결 최소화

### Chat Service
- **역할**: 실시간 메시지 처리
- **DB 사용**: 
  - 메시지 저장 (빈번)
  - 채팅방 정보 조회
  - SSE 연결 관리
- **Redis 활용**: 채팅방 메타데이터 캐싱
- **풀 20개**: 실시간 처리 중심, SSE 연결로 인한 긴 연결 보유

---

## 💡 Redis 캐싱으로 DB 부하 감소

### 예상 효과

| 데이터 유형 | 캐싱 전 DB 호출 | 캐싱 후 DB 호출 | 감소율 |
|------------|--------------|--------------|--------|
| **사용자 정보** | 100% | 10% | 90% ↓ |
| **보관함 목록** | 100% | 20% | 80% ↓ |
| **채팅방 메타** | 100% | 30% | 70% ↓ |

### 메모리 영향
- Redis 캐시: 약 **50-100MB** (추가 메모리)
- DB 연결 감소로 절약: 약 **200MB**

**순 증가**: 거의 없음 또는 음수 (효율적)

---

## 📈 동시 접속자 처리 능력

### 캐싱 없이 (기존)
```
60개 연결 = 동시 60명 처리
병목: User/Store 조회가 DB 부하
```

### 캐싱 적용 후 (최적화)
```
60개 연결 = 동시 100-150명 처리
이유: 캐시 적중 시 DB 호출 없음
```

---

## 🔧 구현 권장사항

### 1. User Service 캐싱
```java
@Cacheable(value = "users", key = "#userId")
public UserDto findById(UUID userId) {
    return userRepository.findById(userId)
        .map(UserDto::from)
        .orElseThrow();
}
```

### 2. Store Service 캐싱
```java
@Cacheable(value = "bookmarks", key = "#userId")
public List<StorageListResponse> getBookmarks(UUID userId) {
    // 캐시 미스 시 DB 조회
}
```

### 3. Chat Service 캐싱
```java
@Cacheable(value = "chatrooms", key = "#chatroomId")
public ChatRoom getChatRoomById(UUID chatroomId) {
    // 채팅방 메타데이터 캐싱
}
```

---

## ✅ 최종 결론

**현재 설정**: 메모리 효율적으로 최적화됨
- 총 연결: 60개 (여유 40개)
- 각 서비스의 역할에 맞게 풀 크기 조정
- Batch는 최소화 (하루 한 번)
- Redis 캐싱으로 실질적 처리 능력 향상

**예상 동시 접속자**: 30-50명 (DB 관점), 100-150명 (캐싱 고려)

