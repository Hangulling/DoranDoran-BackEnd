# InactiveUserJob 비즈니스 요구사항 확인

> **작성일**: 2025-01-04  
> **상태**: 비즈니스 요구사항 확인 필요

---

## 현재 상태

### 발견된 기능
1. **User 상태 관리**:
   - `User.UserStatus`: ACTIVE, INACTIVE, SUSPENDED
   - `UserService.updateUserStatus()`: 상태 변경 기능
   - `UserService.deleteUser()`: 소프트 삭제 (INACTIVE로 변경)

2. **마지막 연결 시간**:
   - `User.lastConnTime`: 마지막 연결 시간 필드
   - `UserService.updateLastConnectionTime()`: 연결 시간 업데이트
   - 로그인 시 자동 업데이트됨

3. **비활성 사용자 로그인 차단**:
   - `AuthService.login()`: INACTIVE 사용자 로그인 시도 차단
   - `AuthService.oauthLogin()`: OAuth 로그인도 동일하게 차단

---

## 확인 필요 사항

### 1. 자동 비활성화 정책
**질문**: 오랫동안 접속하지 않은 사용자를 자동으로 INACTIVE로 변경해야 하나?

**옵션 A**: 자동 비활성화
- 예: 90일 이상 접속하지 않은 ACTIVE 사용자를 자동으로 INACTIVE로 변경
- 장점: 데이터 정리, 보안 강화
- 단점: 사용자가 예상치 못하게 비활성화될 수 있음

**옵션 B**: 수동 처리만
- 관리자가 수동으로만 INACTIVE로 변경
- 장점: 사용자 경험 보호
- 단점: 데이터 정리 수동 작업 필요

### 2. 비활성화 기준
**질문**: 몇 일 이상 접속하지 않으면 비활성화할까?

**제안**:
- 90일 (3개월): 일반적인 기준
- 180일 (6개월): 더 관대한 기준
- 365일 (1년): 매우 관대한 기준

### 3. 비활성화 전 알림
**질문**: 비활성화 전 사용자에게 알림을 발송해야 하나?

**제안**:
- 30일 전 알림: "30일 후 계정이 비활성화됩니다"
- 7일 전 알림: "7일 후 계정이 비활성화됩니다"
- 알림 없음: 자동 비활성화

### 4. 비활성화 후 처리
**질문**: 비활성화된 사용자의 데이터는 어떻게 처리할까?

**제안**:
- 데이터 보존: INACTIVE 상태로 유지 (데이터 보존)
- 일정 기간 후 삭제: 예) 1년 후 완전 삭제
- 즉시 삭제: 비활성화와 동시에 삭제 (권장하지 않음)

### 5. 비활성화 대상 제외
**질문**: 특정 사용자는 자동 비활성화에서 제외해야 하나?

**제안**:
- ADMIN 역할 사용자 제외
- 최근 결제한 사용자 제외
- 특정 플래그가 있는 사용자 제외

---

## 제안 구현 내용

### 기본 구현 (비즈니스 요구사항 확인 후)

```java
@Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul") // 매일 새벽 2시
public void processInactiveUsers() {
    // 1. 설정값 읽기
    int daysInactive = 90; // 비활성화 기준 (일)
    
    // 2. 대상 사용자 조회
    // - ACTIVE 상태
    // - last_conn_time이 (현재 - daysInactive) 이전
    // - ADMIN 역할 제외 (선택)
    
    // 3. 상태 변경
    // - INACTIVE로 변경
    // - UserStatusChangedEvent 발행
    
    // 4. 로깅
    // - 변경된 사용자 수 로깅
}
```

### 고급 구현 (알림 포함)

```java
@Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
public void processInactiveUsers() {
    // 1. 경고 알림 (30일 전)
    sendWarningNotifications(30);
    
    // 2. 최종 경고 알림 (7일 전)
    sendWarningNotifications(7);
    
    // 3. 비활성화 처리
    deactivateInactiveUsers(90);
}
```

---

## 다음 단계

1. **비즈니스 요구사항 확인**:
   - 자동 비활성화 정책 결정
   - 비활성화 기준 (일수) 결정
   - 알림 발송 여부 결정

2. **구현 계획 수립**:
   - 요구사항 확인 후 상세 구현 계획 작성

3. **구현**:
   - InactiveUserJob 구현
   - InactiveUserService 구현

---

## 참고 사항

- GDPR 등 개인정보 보호 규정 고려 필요
- 사용자에게 사전 알림 필요 여부 확인
- 데이터 보존 정책 확인


