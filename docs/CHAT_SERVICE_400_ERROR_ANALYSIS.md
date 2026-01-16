# Chat 서비스 400 오류 분석 보고서

**작성일**: 2025-11-07  
**분석 대상**: Chat 서비스 (dorandoran-chat)  
**분석 기간**: 최근 24시간

## 📋 목차

1. [요약](#요약)
2. [오류 유형별 상세 분석](#오류-유형별-상세-분석)
3. [발생 빈도 및 영향도](#발생-빈도-및-영향도)
4. [원인 분석](#원인-분석)
5. [조치 방안 및 결론](#조치-방안-및-결론)

---

## 요약

Chat 서비스에서 확인된 400 오류는 크게 두 가지 유형으로 분류됩니다:

1. **잘못된 인수 오류 (IllegalArgumentException)**: 6회 발생
   - IntimacyAgent의 concept 파싱 중 타입 불일치 발생
   - 실제 서비스에 영향 있음 (수정 필요)

2. **HTTP 프로토콜 오류**: 8회 발생
   - 외부에서 다른 프로토콜(TLS, SSH, RTSP) 접근 시도
   - Tomcat의 정상적인 보안 동작 (조치 불필요)

---

## 오류 유형별 상세 분석

### 1. 잘못된 인수 오류 (IllegalArgumentException)

#### 오류 메시지
```
잘못된 인수: d != java.lang.String
```

#### 발생 위치
- **파일**: `chat/src/main/java/com/dorandoran/chat/service/agent/IntimacyAgent.java`
- **라인**: 69번
- **메서드**: `getConceptFromChatRoom()`

```java
private String getConceptFromChatRoom(UUID chatroomId) {
    return chatRoomRepository.findById(chatroomId)
        .map(room -> {
            if (room.getSettings() != null && room.getSettings().has("concept")) {
                return room.getSettings().get("concept").asText(); // ← 69번 라인
            }
            return "FRIEND";
        })
        .orElse("FRIEND");
}
```

#### 원인
- `settings.get("concept")`가 String이 아닌 다른 타입(숫자, 객체 등)으로 저장된 경우
- `asText()` 메서드는 JsonNode가 텍스트 노드일 때만 작동
- 다른 타입에서 호출 시 `IllegalArgumentException` 발생

#### 영향받은 채팅방
다음 채팅방에서 오류 발생 확인:

| ChatRoom ID | 발생 횟수 | 발생 시각 |
|------------|----------|----------|
| `1bd213be-153a-4fac-8a4f-0a6a64316493` | 2회 | 2025-11-07 16:12:03, 16:12:22 |
| `116e48cc-399d-491f-acd6-15cd9b6d258b` | 1회 | 2025-11-07 16:13:28 |
| `0f696a35-4a92-48e9-89fe-2dbefbfbbbf3` | 1회 | 2025-11-07 16:16:02 |
| `c0baf3aa-2817-441e-9a94-1deb29bed1f3` | 1회 | 2025-11-07 17:36:12 |
| `5128354b-dec5-41e1-9915-a9fbf2f7e3f7` | 1회 | 2025-11-07 17:52:48 |

#### HTTP 상태 코드
- **400 Bad Request** (GlobalExceptionHandler에서 처리)

#### 예외 처리 흐름
```
IntimacyAgent.getConceptFromChatRoom()
  → JsonNode.asText() 호출
  → IllegalArgumentException 발생
  → GlobalExceptionHandler.handleIllegalArgumentException()
  → 400 Bad Request 반환
```

#### 영향도
- **높음**: IntimacyAgent가 정상 작동하지 않아 친밀도 분석 실패
- 사용자 메시지 처리 중 오류 발생 가능
- Multi-Agent 처리 파이프라인에 영향

---

### 2. HTTP 프로토콜 오류

#### 오류 메시지
```
Invalid character found in method name [프로토콜별 바이트 시퀀스]
Error parsing HTTP request header
```

#### 발생 위치
- **Tomcat HTTP/1.1 파서**: `org.apache.coyote.http11.Http11InputBuffer.parseRequestLine()`
- **스택 트레이스**:
  ```
  at org.apache.coyote.http11.Http11InputBuffer.parseRequestLine(Http11InputBuffer.java:407)
  at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:257)
  at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63)
  ```

#### 발견된 프로토콜 접근 시도

| 프로토콜 | 발생 횟수 | 특징 | 예시 바이트 |
|---------|----------|------|------------|
| **TLS/SSL** | 2회 | `0x16` (TLS Handshake 시작) | `0x160x030x010x00...` |
| **SSH** | 1회 | SSH 클라이언트 연결 시도 | `SSH-2.0-Go...` |
| **RTSP** | 1회 | RTSP 클라이언트 연결 시도 | `RTSP/1.0...` |

#### 발생 시각
- 2025-11-07 15:20:28 (RTSP)
- 2025-11-07 15:47:36 (TLS)
- 2025-11-07 15:49:04 (SSH)
- 2025-11-07 17:58:05 (TLS)

#### 원인 분석
1. **외부 포트 스캔/봇 활동**
   - 인터넷 스캐너가 다양한 프로토콜로 포트 스캔 시도
   - 일반적인 보안 스캔 패턴
   - 공개된 서버에서 흔히 발생하는 현상

2. **Tomcat의 정상 동작**
   - HTTP가 아닌 프로토콜 접근을 자동으로 거부
   - `IllegalArgumentException` 발생 후 400 Bad Request 반환
   - 보안상 정상적인 동작

#### HTTP 상태 코드
- **400 Bad Request** (Tomcat에서 자동 처리)

#### 현재 설정 상태
Gateway는 이미 HTTP/1.1로 강제 설정되어 있습니다:

```java
// gateway/src/main/java/com/dorandoran/gateway/config/NettyHttpClientConfig.java
.protocol(HttpProtocol.HTTP11) // HTTP/1.1 강제 (SSE 호환성)
```

#### 영향도
- **낮음**: 외부 스캔 시도에 대한 정상적인 거부 동작
- 실제 서비스에 영향 없음
- 보안상 정상 동작

---

## 발생 빈도 및 영향도

### 전체 통계 (최근 24시간)

| 오류 유형 | 발생 횟수 | 빈도 | 영향도 | 조치 필요 |
|----------|----------|------|--------|----------|
| 잘못된 인수 오류 | 6회 | 낮음 | **높음** | ✅ **필요** |
| HTTP 프로토콜 오류 | 8회 | 낮음 | 낮음 | ❌ 불필요 |

### 시간대별 발생 패턴

#### 잘못된 인수 오류
- 주로 오후 시간대 (16:00-18:00)에 집중 발생
- 사용자 활동이 활발한 시간대와 일치

#### HTTP 프로토콜 오류
- 하루 종일 산발적으로 발생
- 외부 스캐너 활동과 일치

---

## 원인 분석

### 1. 잘못된 인수 오류 원인

#### 데이터 불일치 가능성
- ChatRoom의 `settings` JSON 필드에 `concept` 값이 String이 아닌 다른 타입으로 저장된 경우
- 가능한 원인:
  1. 초기 데이터 마이그레이션 시 타입 불일치
  2. 다른 서비스에서 잘못된 형식으로 업데이트
  3. 수동 DB 수정 시 타입 오류

#### 코드 취약점
- `asText()` 호출 전 타입 검증 없음
- JsonNode의 타입을 확인하지 않고 바로 `asText()` 호출

### 2. HTTP 프로토콜 오류 원인

#### 외부 스캐너 활동
- 인터넷 상의 자동화된 포트 스캔 봇
- 다양한 프로토콜로 서비스 탐지 시도
- 공개된 서버에서 흔히 발생하는 현상

#### 보안 그룹 설정
- AWS Security Group에서 포트 8083이 공개되어 있음
- 외부에서 접근 가능한 상태

---

## 조치 방안 및 결론

### 1. 잘못된 인수 오류 - 조치 필요 ✅

#### 권장 수정 사항

**IntimacyAgent.java의 `getConceptFromChatRoom()` 메서드 수정:**

```java
private String getConceptFromChatRoom(UUID chatroomId) {
    return chatRoomRepository.findById(chatroomId)
        .map(room -> {
            if (room.getSettings() != null && room.getSettings().has("concept")) {
                JsonNode conceptNode = room.getSettings().get("concept");
                // 안전하게 String으로 변환
                if (conceptNode.isTextual()) {
                    return conceptNode.asText();
                } else if (conceptNode.isNumber()) {
                    // 숫자인 경우 기본값 반환
                    log.warn("IntimacyAgent: concept이 숫자 타입입니다. 기본값 FRIEND 사용. chatroomId={}", chatroomId);
                    return "FRIEND";
                } else {
                    // 다른 타입인 경우 기본값 반환
                    log.warn("IntimacyAgent: concept이 유효하지 않은 타입입니다. 기본값 FRIEND 사용. chatroomId={}", chatroomId);
                    return "FRIEND";
                }
            }
            return "FRIEND";
        })
        .orElse("FRIEND");
}
```

**동일한 수정을 ChatRoomResponse.java에도 적용:**

```java
private static String extractConceptFromSettings(JsonNode settings) {
    if (settings != null && settings.has("concept")) {
        JsonNode conceptNode = settings.get("concept");
        if (conceptNode.isTextual()) {
            return conceptNode.asText();
        } else {
            log.warn("ChatRoomResponse: concept이 유효하지 않은 타입입니다. 기본값 FRIEND 사용.");
            return "FRIEND";
        }
    }
    return "FRIEND";
}
```

#### 추가 조치 사항

1. **데이터 검증**
   - 영향받은 채팅방의 `settings.concept` 값 확인
   - 잘못된 타입의 데이터가 있다면 수정

2. **모니터링 강화**
   - 이 오류 발생 시 알림 설정
   - 추후 발생 빈도 모니터링

### 2. HTTP 프로토콜 오류 - 조치 불필요 ❌

#### 결론
- **정상적인 보안 동작**: Tomcat이 HTTP가 아닌 프로토콜 접근을 자동으로 거부
- **외부 스캔 시도**: 인터넷 상의 일반적인 포트 스캔 활동
- **낮은 빈도**: 24시간 동안 8회 발생 (매우 낮음)
- **서비스 영향 없음**: 실제 사용자 요청에 영향 없음

#### 선택적 조치 사항 (필요 시)

1. **로그 레벨 조정** (선택)
   - 이런 오류를 DEBUG 레벨로 낮춰 로그 노이즈 감소
   - 하지만 모니터링 관점에서는 유지해도 무방

2. **보안 그룹 확인**
   - AWS Security Group에서 불필요한 포트는 차단
   - 현재는 정상 동작 중

---

## 요약 및 결론

### 조치 필요 항목
1. ✅ **IntimacyAgent의 concept 파싱 로직 수정** (타입 안전성 강화)
2. ✅ **ChatRoomResponse의 concept 추출 로직 수정** (동일한 문제)
3. ✅ **영향받은 채팅방 데이터 검증 및 수정**

### 조치 불필요 항목
1. ❌ **HTTP 프로토콜 오류**: 정상적인 보안 동작, 추가 조치 불필요

### 최종 결론
- **잘못된 인수 오류**: 실제 문제이며 수정 필요
- **HTTP 프로토콜 오류**: 정상 동작이며 조치 불필요
- 전체적으로 서비스 안정성에 큰 영향은 없으나, IntimacyAgent의 타입 안전성 강화는 권장됨

---

## 참고 자료

- **관련 파일**:
  - `chat/src/main/java/com/dorandoran/chat/service/agent/IntimacyAgent.java`
  - `chat/src/main/java/com/dorandoran/chat/service/dto/ChatRoomResponse.java`
  - `chat/src/main/java/com/dorandoran/chat/exception/GlobalExceptionHandler.java`
  - `gateway/src/main/java/com/dorandoran/gateway/config/NettyHttpClientConfig.java`

- **로그 분석 명령어**:
  ```bash
  # 잘못된 인수 오류 확인
  docker logs --since 24h dorandoran-chat 2>&1 | grep -E '잘못된 인수'
  
  # HTTP 프로토콜 오류 확인
  docker logs --since 24h dorandoran-chat 2>&1 | grep -E 'Invalid character found'
  ```

---

**문서 버전**: 1.0  
**최종 업데이트**: 2025-11-07

