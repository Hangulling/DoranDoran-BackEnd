# 문제 해결 가이드 (Troubleshooting Guide)

## 목차
1. [Hibernate 프록시 객체 순환 참조 문제](#hibernate-프록시-객체-순환-참조-문제)
2. [SSE 이벤트 전송 실패](#sse-이벤트-전송-실패)
3. [AI 프롬프트 파싱 오류](#ai-프롬프트-파싱-오류)
4. [데이터베이스 연결 문제](#데이터베이스-연결-문제)
5. [Docker 배포 문제](#docker-배포-문제)

---

## Hibernate 프록시 객체 순환 참조 문제

### 증상
- 애플리케이션 실행 중 `NullPointerException`과 스택 오버플로우 발생
- 로그에서 `User$HibernateProxy$Dyp9SYgE.toString` 무한 반복 확인
- 애플리케이션이 정상적으로 시작되지 않음

### 원인
양방향 관계를 가진 엔티티에서 Lombok `@Data`가 생성한 `toString()` 메서드가 순환 참조를 일으킴

### 해결 방법
```java
// User 엔티티
@ToString(exclude = {"chatRooms"})
public class User {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ChatRoom> chatRooms = new ArrayList<>();
}

// ChatRoom 엔티티
@ToString(exclude = {"user", "chatbot", "messages", "lastMessage"})
public class ChatRoom {
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Chatbot chatbot;
    
    @OneToMany(mappedBy = "chatRoom", fetch = FetchType.LAZY)
    private List<Message> messages;
    
    @OneToOne(fetch = FetchType.LAZY)
    private Message lastMessage;
}
```

### 예방 방법
- 양방향 관계가 있는 엔티티에서는 항상 `@ToString(exclude = {...})` 사용
- 연관 엔티티 필드들을 toString()에서 제외

---

## SSE 이벤트 전송 실패

### 증상
- `greeting_bot_message` 또는 `greeting_guide_message` 이벤트가 클라이언트에 전달되지 않음
- SSE 연결은 정상이지만 특정 이벤트만 누락

### 원인
1. AI 응답 파싱 실패로 인한 null 값 전달
2. 프롬프트 JSON 형식 오류
3. null 체크 부족

### 해결 방법

#### 1. AI 프롬프트 JSON 형식 수정
```java
// 올바른 JSON 형식
{
    "botMessage": "인트로 메시지",
    "guideMessage": "대화 문구를 제안"
}

// 잘못된 형식 (제거해야 함)
{
    "intimacyLevel": "AI가 감지한 친밀도(0~3)",
    "botMessage": 인트로 메시지,  // 따옴표 없음
    "guideMessage": 대화 문구를 제안,  // 따옴표 없음
    "detectedLevel": 0
}
```

#### 2. null 체크 추가
```java
private GreetingResponse parseAIResponse(String aiResponse) {
    try {
        JsonNode jsonNode = objectMapper.readTree(aiResponse);
        String botMessage = jsonNode.get("botMessage").asText();
        String guideMessage = jsonNode.get("guideMessage").asText();
        
        // null 체크 추가
        if (botMessage == null || guideMessage == null) {
            log.warn("AI 응답에서 null 값 발견: botMessage={}, guideMessage={}", botMessage, guideMessage);
            throw new RuntimeException("AI 응답에 null 값이 포함됨");
        }
        
        return new GreetingResponse(botMessage, guideMessage);
    } catch (Exception e) {
        log.error("AI 응답 파싱 실패: {}", aiResponse, e);
        throw new RuntimeException("AI 응답 파싱 실패", e);
    }
}
```

#### 3. SSE 전송 시 안전성 강화
```java
private void sendGreetingViaSSE(UUID chatroomId, Message botMessage, Message guideMessage) {
    try {
        // bot 메시지 전송
        if (botMessage != null && botMessage.getId() != null && 
            botMessage.getContent() != null && botMessage.getCreatedAt() != null) {
            sseManager.send(chatroomId, "greeting_bot_message", Map.of(
                "messageId", botMessage.getId(),
                "content", botMessage.getContent(),
                "senderType", "bot",
                "timestamp", botMessage.getCreatedAt()
            ));
        } else {
            log.warn("botMessage가 null이거나 필수 필드가 누락됨: botMessage={}", botMessage);
        }
        
        // guide 메시지 전송
        if (guideMessage != null && guideMessage.getId() != null && 
            guideMessage.getContent() != null && guideMessage.getCreatedAt() != null) {
            sseManager.send(chatroomId, "greeting_guide_message", Map.of(
                "messageId", guideMessage.getId(),
                "content", guideMessage.getContent(),
                "senderType", "system",
                "timestamp", guideMessage.getCreatedAt()
            ));
        } else {
            log.warn("guideMessage가 null이거나 필수 필드가 누락됨: guideMessage={}", guideMessage);
        }
        
    } catch (Exception e) {
        log.error("SSE 인사 메시지 전송 실패: chatroomId={}", chatroomId, e);
    }
}
```

---

## AI 프롬프트 파싱 오류

### 증상
- AI가 올바른 JSON 형식으로 응답하지 않음
- `JsonProcessingException` 발생
- `parseAIResponse` 메서드에서 파싱 실패

### 원인
1. 프롬프트에서 요구하는 JSON 형식이 일관되지 않음
2. 예시 시나리오에서 잘못된 JSON 형식 사용
3. 불필요한 필드(`intimacyLevel`, `detectedLevel`) 요구

### 해결 방법

#### 1. 프롬프트 JSON 형식 통일
```java
// 모든 프롬프트에서 동일한 형식 사용
- 다음 JSON 형식으로 정확히 답변:
{
    "botMessage": "인트로 메시지",
    "guideMessage": "대화 문구를 제안"
}
```

#### 2. 예시 시나리오 수정
```java
// 올바른 예시
{
    "botMessage": "지금 뭐해?",
    "guideMessage": "Let's continue the conversation about what fun or interesting things you're doing right now!"
}

// 잘못된 예시 (제거해야 함)
{
    "botMessage": 지금 뭐해?,  // 따옴표 없음
    "guideMessage": Let's continue...,  // 따옴표 없음
    "detectedLevel": 0  // 불필요한 필드
}
```

#### 3. 불필요한 필드 제거
- `intimacyLevel`: AI가 받는 입력 파라미터이므로 출력에 포함할 필요 없음
- `detectedLevel`: 사용되지 않는 필드이므로 제거

---

## 데이터베이스 연결 문제

### 증상
- `HikariPool` 연결 실패
- `Connection refused` 오류
- 애플리케이션 시작 시 데이터베이스 연결 타임아웃

### 원인
1. 데이터베이스 서버가 실행되지 않음
2. 네트워크 연결 문제
3. 잘못된 연결 정보 (URL, 사용자명, 비밀번호)
4. 방화벽 설정 문제

### 해결 방법

#### 1. 연결 정보 확인
```yaml
# application-docker.yml
spring:
  datasource:
    url: jdbc:postgresql://dorandoran-postgres.cpw00a6ga2uv.us-east-2.rds.amazonaws.com:5432/dorandoran
    username: doran
    password: DoranDoran123!
```

#### 2. 네트워크 연결 확인
```bash
# 데이터베이스 서버 연결 테스트
telnet dorandoran-postgres.cpw00a6ga2uv.us-east-2.rds.amazonaws.com 5432
```

#### 3. Docker 네트워크 확인
```bash
# Docker 네트워크 목록 확인
docker network ls

# 컨테이너가 올바른 네트워크에 연결되어 있는지 확인
docker inspect dorandoran-chat | grep NetworkMode
```

---

## Docker 배포 문제

### 증상
- Docker 이미지 빌드 실패
- 컨테이너 실행 실패
- 서버로 이미지 전송 실패

### 원인
1. 빌드 오류 (컴파일 실패, 의존성 문제)
2. Docker 이미지 크기 초과
3. 네트워크 연결 문제
4. 권한 문제

### 해결 방법

#### 1. 로컬 빌드 확인
```bash
# Gradle 빌드 테스트
./gradlew :chat:build -x test

# Docker 이미지 빌드 테스트
docker build -f docker/Dockerfile.chat -t dorandoran-chat:latest .
```

#### 2. 이미지 전송 최적화
```bash
# 이미지를 tar 파일로 저장
docker save dorandoran-chat:latest -o dorandoran-chat.tar

# 압축하여 전송 (선택사항)
gzip dorandoran-chat.tar
scp -i "$KEY" dorandoran-chat.tar.gz ec2-user@server:~/

# 서버에서 압축 해제 및 로드
gunzip dorandoran-chat.tar.gz
docker load -i dorandoran-chat.tar
```

#### 3. 컨테이너 상태 확인
```bash
# 실행 중인 컨테이너 확인
docker ps | grep dorandoran-chat

# 컨테이너 로그 확인
docker logs --since 10m dorandoran-chat

# 컨테이너 상태 확인
docker inspect dorandoran-chat | grep Status
```

---

## 일반적인 디버깅 방법

### 1. 로그 확인
```bash
# 실시간 로그 모니터링
docker logs -f dorandoran-chat

# 특정 시간대 로그 확인
docker logs --since 1h dorandoran-chat

# 오류 로그만 필터링
docker logs dorandoran-chat 2>&1 | grep -i error
```

### 2. 네트워크 연결 테스트
```bash
# 포트 연결 확인
telnet localhost 8083

# 외부에서 접근 테스트
curl http://3.21.177.186:8083/actuator/health
```

### 3. 리소스 사용량 확인
```bash
# 컨테이너 리소스 사용량
docker stats dorandoran-chat

# 서버 리소스 사용량
top
df -h
free -h
```

### 4. 데이터베이스 연결 테스트
```bash
# PostgreSQL 클라이언트로 직접 연결 테스트
psql -h dorandoran-postgres.cpw00a6ga2uv.us-east-2.rds.amazonaws.com -U doran -d dorandoran
```

---

## 문제 보고 시 포함할 정보

문제를 보고할 때 다음 정보를 포함해주세요:

1. **오류 메시지**: 정확한 오류 메시지와 스택 트레이스
2. **로그**: 관련 로그 파일 또는 로그 스니펫
3. **환경 정보**: 
   - 운영체제 및 버전
   - Java 버전
   - Docker 버전
   - Spring Boot 버전
4. **재현 단계**: 문제를 재현하는 단계별 과정
5. **예상 동작**: 정상적으로 동작해야 하는 방식
6. **실제 동작**: 실제로 발생하는 문제

---

## 추가 리소스

- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Hibernate 공식 문서](https://hibernate.org/orm/documentation/)
- [Docker 공식 문서](https://docs.docker.com/)
- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)
