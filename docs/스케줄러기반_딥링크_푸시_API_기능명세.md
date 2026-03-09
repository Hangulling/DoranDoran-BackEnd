# 일일 관심 주제 기반 푸시 알림 API 명세

> 사용자의 관심 주제를 기반으로 채팅방별 딥링크 푸시 알림을 주기적으로 발송하는 스케줄러 API

## 📋 목차

1. [개요](#개요)
2. [API 명세](#api-명세)
3. [기능 흐름](#기능-흐름)
4. [데이터 모델](#데이터-모델)
5. [비즈니스 로직](#비즈니스-로직)
6. [설정](#설정)
7. [예외 처리](#예외-처리)
8. [모니터링 및 로깅](#모니터링-및-로깅)

---

## 개요

### 목적

사용자의 관심 주제를 기반으로 기존 채팅방에 대한 푸시 알림을 주기적으로 발송하여 사용자 재참여를 유도합니다.

### 핵심 기능

1. **자동 스케줄 실행**: Spring `@Scheduled`를 통한 주기적 실행
2. **대상 사용자 필터링**: 푸시 알림이 활성화된 사용자만 대상
3. **관심 주제 기반 메시지 생성**: 사용자의 관심 주제를 활용한 개인화된 메시지
4. **채팅방별 중복 방지**: 같은 날 같은 채팅방에 중복 발송 방지
5. **딥링크 포함**: 푸시 클릭 시 해당 채팅방으로 바로 이동

---

## API 명세

### 기본 정보

- **타입**: 내부 스케줄러 (외부 API 엔드포인트 없음)
- **실행 방식**: Spring `@Scheduled` 어노테이션 기반 자동 실행
- **서비스**: User Service
- **클래스**: `NotificationDispatchService`
- **메서드**: `sendDailyInterestNotifications()`

### 스케줄 설정

```java
@Scheduled(cron = "${notification.daily.cron:0 0 9 * * *}")
public void sendDailyInterestNotifications()
```

**Cron 표현식**:
- 기본값: `0 0 9 * * *` (매일 오전 9시)
- 설정: `application.yml` 또는 환경 변수 `NOTIFICATION_DAILY_CRON`으로 변경 가능

**Cron 표현식 예시**:
- `0 0 9 * * *` - 매일 오전 9시
- `0 0 9,18 * * *` - 매일 오전 9시, 오후 6시
- `0 0 9 * * MON-FRI` - 평일 오전 9시
- `0 0 */6 * * *` - 6시간마다

---

## 기능 흐름

### 전체 흐름도

```
┌─────────────────────────────────────────────────────────────┐
│  Spring Scheduler (@Scheduled)                              │
│  매일 지정된 시간에 자동 실행                                │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  1. 대상 사용자 조회                                         │
│     - UserNotificationSetting.pushEnabled = true            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  2. 사용자별 처리 루프                                       │
│     for each user:                                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        ▼                             ▼
┌───────────────┐          ┌──────────────────────┐
│ 2-1. 관심 주제 │          │ 2-2. 채팅방 목록 조회 │
│     조회       │          │     (Chat Service)   │
└───────┬───────┘          └──────────┬───────────┘
        │                             │
        │                             │
        └──────────────┬──────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  3. 채팅방별 처리 루프                                       │
│     for each chatroom:                                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  4. 중복 발송 체크                                           │
│     PushDeliveryLog 조회                                     │
│     (userId, chatroomId, 오늘 날짜)                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        ▼                             ▼
    이미 발송됨                  발송 가능
        │                             │
        │                             ▼
        │              ┌──────────────────────────────┐
        │              │ 5. 푸시 메시지 생성          │
        │              │    - 제목: "채팅방"          │
        │              │    - 본문: 관심 주제 기반   │
        │              └──────────────┬───────────────┘
        │                             │
        │                             ▼
        │              ┌──────────────────────────────┐
        │              │ 6. 푸시 발송                  │
        │              │    - FCM 토큰 조회           │
        │              │    - 딥링크 포함              │
        │              │    - PushNotificationService │
        │              └──────────────┬───────────────┘
        │                             │
        │                             ▼
        │              ┌──────────────────────────────┐
        │              │ 7. 발송 로그 저장            │
        │              │    PushDeliveryLog 저장       │
        │              └──────────────────────────────┘
        │
        └─────────────────────────────┘
                       │
                       ▼
              다음 채팅방 처리
```

### 상세 처리 단계

#### 1단계: 대상 사용자 조회

```java
List<UserNotificationSetting> targets = 
    userNotificationSettingRepository.findByPushEnabledTrue();
```

- **조건**: `pushEnabled = true`인 사용자만 조회
- **결과**: 푸시 알림을 받기로 설정한 사용자 목록

#### 2단계: 사용자별 관심 주제 조회

```java
List<UserInterestTopic> topics = 
    userInterestTopicRepository.findByIdUserId(userId);
```

- **조건**: 사용자가 설정한 관심 주제
- **처리**: 관심 주제가 없으면 해당 사용자 스킵

#### 3단계: 관심 주제 라벨 매핑

```java
Map<String, String> labels = 
    interestTopicRepository.findAllById(keys).stream()
        .collect(Collectors.toMap(...));
```

- **목적**: `topicKey` → `label` 변환 (예: "travel" → "여행")
- **사용**: 메시지 본문 생성에 활용

#### 4단계: 채팅방 목록 조회

```java
List<ChatRoomSummary> chatrooms = 
    chatRoomClient.listChatRooms(userId);
```

- **API 호출**: Chat Service의 `GET /api/chat/chatrooms`
- **인증**: HMAC 기반 서비스 간 인증
- **조건**: 채팅방이 없으면 해당 사용자 스킵

#### 5단계: 중복 발송 체크

```java
boolean alreadySent = pushDeliveryLogRepository
    .existsByUserIdAndChatroomIdAndSentDate(
        userId, chatroomId, today
    );
```

- **체크 조건**: 
  - `userId`
  - `chatroomId`
  - `sentDate` (오늘 날짜)
- **결과**: 이미 발송된 경우 스킵

#### 6단계: 푸시 메시지 본문 생성

```java
String body = buildBody(keys, labels);
```

**메시지 생성 규칙**:
- 관심 주제가 1개: `"{주제} 주제로 이야기 이어가요."`
- 관심 주제가 2개 이상: `"오늘은 {주제1}, {주제2} 이야기 어때요?"`
- 관심 주제가 없음: `"대화를 이어가볼까요?"`

**예시**:
- `"여행 주제로 이야기 이어가요."`
- `"오늘은 여행, 음식 이야기 어때요?"`

#### 7단계: 푸시 알림 발송

```java
pushNotificationService.sendToUser(
    userId,
    "채팅방",           // 제목
    body,              // 본문 (관심 주제 기반)
    chatroomId,        // 채팅방 ID
    null               // messageId (없음)
);
```

**푸시 페이로드**:
```json
{
  "notification": {
    "title": "채팅방",
    "body": "오늘은 여행, 음식 이야기 어때요?"
  },
  "data": {
    "deeplink": "dorandoran://chat?roomId={chatroomId}",
    "universalLink": "https://www.doran-chat.com/chat?roomId={chatroomId}",
    "chatroomId": "{chatroomId}",
    "messageId": "",
    "startMessage": "오늘은 여행, 음식 이야기 어때요?",
    "sentAt": "2026-02-17T09:00:00+09:00"
  }
}
```

#### 8단계: 발송 로그 저장

```java
pushDeliveryLogRepository.save(
    PushDeliveryLog.builder()
        .userId(userId)
        .chatroomId(chatroomId)
        .sentDate(today)
        .build()
);
```

- **목적**: 중복 발송 방지
- **저장 정보**: 사용자 ID, 채팅방 ID, 발송 날짜

---

## 데이터 모델

### UserNotificationSetting

```java
@Entity
@Table(name = "user_notification_settings")
public class UserNotificationSetting {
    @Id
    private UUID userId;
    
    @Column(nullable = false)
    private boolean pushEnabled;  // 푸시 알림 활성화 여부
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### UserInterestTopic

```java
@Entity
@Table(name = "user_interest_topics")
public class UserInterestTopic {
    @EmbeddedId
    private UserInterestTopicId id;  // userId + topicKey
    
    private LocalDateTime createdAt;
}
```

### InterestTopic

```java
@Entity
@Table(name = "interest_topics")
public class InterestTopic {
    @Id
    private String topicKey;  // 예: "travel", "food"
    
    private String label;      // 예: "여행", "음식"
}
```

### PushDeliveryLog

```java
@Entity
@Table(name = "push_delivery_logs")
public class PushDeliveryLog {
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private UUID chatroomId;
    
    @Column(nullable = false)
    private LocalDate sentDate;  // 발송 날짜 (중복 체크용)
    
    private LocalDateTime createdAt;
}
```

### ChatRoomSummary (Chat Service 응답)

```java
public record ChatRoomSummary(
    UUID id,
    UUID lastMessageId
) {}
```

---

## 비즈니스 로직

### 대상 사용자 필터링

1. **푸시 알림 활성화 여부**: `pushEnabled = true`만 대상
2. **관심 주제 존재 여부**: 관심 주제가 없는 사용자는 스킵
3. **채팅방 존재 여부**: 채팅방이 없는 사용자는 스킵

### 중복 발송 방지

- **체크 기준**: `(userId, chatroomId, sentDate)` 조합
- **체크 시점**: 각 채팅방 처리 전
- **결과**: 같은 날 같은 채팅방에 이미 발송된 경우 스킵

### 메시지 생성 로직

```java
private String buildBody(List<String> keys, Map<String, String> labels) {
    if (keys == null || keys.isEmpty()) {
        return "대화를 이어가볼까요?";
    }
    
    List<String> resolved = keys.stream()
        .map(k -> labels.getOrDefault(k, k))
        .collect(Collectors.toList());
    
    if (resolved.size() == 1) {
        return resolved.get(0) + " 주제로 이야기 이어가요.";
    }
    
    String first = resolved.get(0);
    String second = resolved.get(1);
    return "오늘은 " + first + ", " + second + " 이야기 어때요?";
}
```

**규칙**:
1. 관심 주제가 없으면 기본 메시지 반환
2. 관심 주제가 1개면 단일 주제 메시지
3. 관심 주제가 2개 이상이면 첫 2개 주제를 조합한 메시지

### 딥링크 생성

**스킴 딥링크**:
```
dorandoran://chat?roomId={chatroomId}
```

**유니버설 링크**:
```
https://www.doran-chat.com/chat?roomId={chatroomId}
```

---

## 설정

### application.yml

```yaml
notification:
  daily:
    cron: ${NOTIFICATION_DAILY_CRON:0 0 9 * * *}
```

### 환경 변수

```bash
# 매일 오전 9시 실행 (기본값)
NOTIFICATION_DAILY_CRON=0 0 9 * * *

# 매일 오전 9시, 오후 6시 실행
NOTIFICATION_DAILY_CRON=0 0 9,18 * * *

# 평일 오전 9시 실행
NOTIFICATION_DAILY_CRON=0 0 9 * * MON-FRI

# 6시간마다 실행
NOTIFICATION_DAILY_CRON=0 0 */6 * * *
```

### 스케줄러 활성화

`UserApplication` 클래스에 `@EnableScheduling` 어노테이션이 필요합니다:

```java
@SpringBootApplication
@EnableScheduling
public class UserApplication {
    // ...
}
```

---

## 예외 처리

### 조용한 실패 (Silent Failure)

대부분의 예외는 로그만 남기고 다음 항목 처리를 계속합니다:

1. **관심 주제 조회 실패**: 해당 사용자 스킵
2. **채팅방 목록 조회 실패**: 빈 리스트 반환, 해당 사용자 스킵
3. **푸시 발송 실패**: 로그 기록 후 다음 채팅방 처리 계속

### 로깅

```java
log.warn("채팅방 목록 조회 실패: userId={}, error={}", userId, e.getMessage());
log.warn("푸시 전송 실패: userId={}, tokenId={}, error={}", userId, tokenId, e.getMessage());
```

### FCM 토큰 없음

- FCM 토큰이 없는 사용자는 푸시 발송을 스킵합니다
- `PushNotificationService.sendToUser()` 내부에서 처리

---

## 모니터링 및 로깅

### 로그 레벨

- **INFO**: 정상 실행 완료, 푸시 발송 성공
- **WARN**: 조회 실패, 푸시 발송 실패
- **DEBUG**: 상세 디버깅 정보 (필요시)

### 주요 로그 포인트

1. **스케줄러 시작**: (Spring Scheduler 기본 로그)
2. **대상 사용자 수**: (직접 로깅 없음, 필요시 추가 가능)
3. **푸시 발송 성공**: `PushNotificationService`에서 로깅
4. **푸시 발송 실패**: `PushNotificationService`에서 로깅

### 모니터링 지표 (권장)

다음 지표를 모니터링하는 것을 권장합니다:

1. **실행 횟수**: 스케줄러가 정상적으로 실행되는지
2. **대상 사용자 수**: `pushEnabled = true`인 사용자 수
3. **발송 성공률**: 푸시 발송 성공/실패 비율
4. **중복 방지 효과**: 중복 체크로 스킵된 채팅방 수
5. **FCM 토큰 부재**: 토큰이 없어 스킵된 사용자 수

### 개선 제안

향후 다음 기능 추가를 고려할 수 있습니다:

1. **메트릭 수집**: Micrometer를 통한 메트릭 수집
2. **실행 시간 로깅**: 전체 실행 시간 측정 및 로깅
3. **통계 정보**: 처리된 사용자 수, 발송된 푸시 수 등
4. **알림**: 실패율이 높을 경우 알림 발송

---

## 관련 문서

- [API_SPEC_PUSH_AND_DEEPLINK.md](./API_SPEC_PUSH_AND_DEEPLINK.md) - 푸시 및 딥링크 전체 명세
- [PUSH_AND_DEEPLINK_CLIENT_GUIDE.md](./PUSH_AND_DEEPLINK_CLIENT_GUIDE.md) - 클라이언트 구현 가이드
- [API_PUSH_DEEPLINK_CONSISTENCY_CHECK.md](./API_PUSH_DEEPLINK_CONSISTENCY_CHECK.md) - 정합성 검증

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|----------|
| 2026-02-17 | 1.0 | 초기 문서 작성 |
