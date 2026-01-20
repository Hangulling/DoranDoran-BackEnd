# 푸시 알림 개발 방안

> Capacitor 기반 모바일 앱을 위한 AI 기반 푸시 알림 시스템 개발 계획서

## 📋 목차

1. [개요](#개요)
2. [요구사항 분석](#요구사항-분석)
3. [현재 시스템 분석](#현재-시스템-분석)
4. [아키텍처 설계](#아키텍처-설계)
5. [상세 구현 방안](#상세-구현-방안)
6. [데이터베이스 설계](#데이터베이스-설계)
7. [API 설계](#api-설계)
8. [프론트엔드 구현](#프론트엔드-구현)
9. [딥링크 처리](#딥링크-처리)
10. [개발 일정](#개발-일정)

---

## 개요

### 목적

서비스 내 AI Agent를 활용하여 개인화된 푸시 알림을 생성하고, 사용자가 알림을 클릭하면 해당 주제와 챗봇 기반으로 채팅방이 자동 생성되어 대화가 시작되도록 구현합니다.

### 핵심 기능

1. **AI 기반 푸시 텍스트 생성**: 서비스 내 Agent를 활용하여 개인화된 푸시 알림 텍스트 생성
2. **주제 기반 채팅방 자동 생성**: 푸시 알림 클릭 시 주제와 챗봇 정보를 기반으로 채팅방 생성 및 대화 시작
3. **컨텍스트 기억**: 생성된 채팅방에서 챗봇이 해당 주제를 기억하고 대화 진행

---

## 요구사항 분석

### 기능 요구사항

#### 1. 푸시 알림 텍스트 생성
- **입력**: 사용자 정보, 챗봇 정보, 대화 주제, 친밀도 레벨
- **출력**: 개인화된 푸시 알림 텍스트 (제목 + 본문)
- **요구사항**:
  - 사용자의 친밀도 레벨에 맞는 어투 사용
  - 챗봇의 컨셉(FRIEND, HONEY, SENIOR, COWORKER)에 맞는 톤
  - 주제에 맞는 자연스러운 문구

#### 2. 푸시 알림 발송
- **대상**: 특정 사용자 또는 사용자 그룹
- **타이밍**: 
  - 정기적 발송 (예: 하루 1회, 주 3회)
  - 이벤트 기반 발송 (예: 장기 미접속 사용자)
- **플랫폼**: iOS (APNs), Android (FCM)

#### 3. 딥링크 처리
- **URL 형식**: `dorandoran://chatroom/create?chatbotId={id}&topic={topic}&concept={concept}&intimacyLevel={level}`
- **동작**: 
  1. 앱 실행 또는 포그라운드 전환
  2. 딥링크 파라미터 파싱
  3. 채팅방 생성 또는 기존 채팅방 조회
  4. 주제 정보를 contextData에 저장
  5. 인사말 생성 (주제 포함)
  6. 채팅 화면으로 이동

#### 4. 주제 기억 및 대화 시작
- **주제 저장**: ChatRoom의 `contextData.sessionData.currentTopic`에 저장
- **프롬프트 주입**: PromptService가 주제 정보를 시스템 프롬프트에 포함
- **인사말 생성**: GreetingService가 주제를 고려하여 인사말 생성

---

## 현재 시스템 분석

### 1. 채팅방 생성 로직

**위치**: `chat/src/main/java/com/dorandoran/chat/service/ChatService.java`

```java
public ChatRoom getOrCreateRoom(UUID userId, UUID chatbotId, String name, 
                                String concept, Integer intimacyLevel, String testModel)
```

**특징**:
- 기존 채팅방이 있으면 조회, 없으면 생성
- `settings` JSONB 필드에 `concept` 저장
- `IntimacyProgress` 초기화

**확장 필요**:
- `contextData`에 주제 정보 저장 기능 추가

### 2. 인사말 생성 로직

**위치**: `chat/src/main/java/com/dorandoran/chat/service/GreetingService.java`

**특징**:
- 컨셉별 주제 목록 제공 (30개)
- 랜덤 주제 선택
- AI로 인사말 생성 (주제 포함)
- `botMessage`와 `guideMessage` 생성

**활용 방안**:
- 푸시 알림에서 전달받은 주제를 사용하여 인사말 생성
- 주제를 `contextData.sessionData.currentTopic`에 저장

### 3. 시스템 프롬프트 생성

**위치**: `chat/src/main/java/com/dorandoran/chat/service/PromptService.java`

**특징**:
- `buildSystemPrompt()`: ChatRoom의 contextData를 기반으로 프롬프트 생성
- `appendRoomContext()`: contextData의 `currentTopic`을 프롬프트에 포함
- 주제 정보가 이미 프롬프트에 반영됨 ✅

**확인 사항**:
- `contextData.sessionData.currentTopic`이 이미 프롬프트에 포함됨
- 추가 작업 불필요

### 4. Agent 구조

**현재 Agent 목록**:
- `IntimacyAgent`: 친밀도 분석 및 교정
- `VocabularyAgent`: 어휘 추출 및 설명
- `ConversationAgent`: 대화 생성
- `SummarizerAgent`: 대화 요약
- `GreetingAgent`: 인사말 생성

**푸시 알림용 Agent 필요**:
- 새로운 `PushNotificationAgent` 생성 또는 기존 Agent 활용
- `GreetingService`의 로직을 재사용 가능

---

## 아키텍처 설계

### 전체 흐름도

```
[푸시 알림 스케줄러/트리거]
    ↓
[PushNotificationService]
    ↓
[PushNotificationAgent] (AI 텍스트 생성)
    ↓
[FCMService] (푸시 발송)
    ↓
[사용자 디바이스] (푸시 알림 수신)
    ↓
[딥링크 클릭]
    ↓
[모바일 앱] (딥링크 파싱)
    ↓
[ChatService.getOrCreateRoom()] (채팅방 생성/조회)
    ↓
[ChatRoom.contextData에 주제 저장]
    ↓
[GreetingService.sendGreeting()] (주제 포함 인사말 생성)
    ↓
[채팅 화면 표시]
```

### 컴포넌트 구조

```
chat-service/
├── service/
│   ├── PushNotificationService.java      # 푸시 알림 발송 서비스
│   ├── PushNotificationAgent.java        # AI 기반 푸시 텍스트 생성 Agent
│   ├── FCMService.java                   # FCM 발송 서비스
│   └── DeepLinkService.java             # 딥링크 처리 서비스
├── controller/
│   └── PushNotificationController.java   # 푸시 알림 API
└── entity/
    └── PushNotification.java             # 푸시 알림 엔티티 (선택적)
```

---

## 상세 구현 방안

### 1. PushNotificationAgent 구현

**목적**: 사용자, 챗봇, 주제 정보를 기반으로 개인화된 푸시 알림 텍스트 생성

**위치**: `chat/src/main/java/com/dorandoran/chat/service/agent/PushNotificationAgent.java`

**구현 예시**:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationAgent {
    private final OpenAIClient openAIClient;
    private final PromptLoaderService promptLoaderService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 푸시 알림 텍스트 생성
     * 
     * @param userId 사용자 ID
     * @param chatbotId 챗봇 ID
     * @param topic 대화 주제
     * @param concept 컨셉 (FRIEND, HONEY, SENIOR, BOSS, COWORKER)
     * @param intimacyLevel 친밀도 레벨 (1-3)
     * @return PushNotificationText (title, body)
     */
    public Mono<PushNotificationText> generateNotificationText(
            UUID userId, UUID chatbotId, String topic, 
            String concept, int intimacyLevel) {
        
        // 1. 프롬프트 로드 (DB 우선, 파일 fallback)
        String systemPrompt = promptLoaderService.loadPrompt(
            "PUSH_NOTIFICATION", concept.toUpperCase(), intimacyLevel, "prod"
        );
        
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            // Fallback 프롬프트
            systemPrompt = buildDefaultPushPrompt(concept, intimacyLevel);
        }
        
        // 2. 사용자 메시지 구성
        String userMessage = String.format(
            "다음 주제로 푸시 알림 텍스트를 생성해주세요:\n" +
            "- 주제: %s\n" +
            "- 컨셉: %s\n" +
            "- 친밀도 레벨: %d\n\n" +
            "제목(title)과 본문(body)을 JSON 형식으로 반환해주세요.",
            topic, concept, intimacyLevel
        );
        
        // 3. OpenAI API 호출
        return Mono.fromCallable(() -> {
            String response = openAIClient.simpleCompletion(
                systemPrompt, userMessage, null
            );
            return parseNotificationText(response);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    private String buildDefaultPushPrompt(String concept, int intimacyLevel) {
        return String.format("""
            당신은 도란도란 서비스의 푸시 알림 텍스트 생성 전문가입니다.
            
            [컨셉]: %s
            [친밀도 레벨]: %d
            
            다음 규칙을 따라 푸시 알림 텍스트를 생성하세요:
            1. 제목(title): 20자 이내, 간결하고 흥미롭게
            2. 본문(body): 50자 이내, 주제에 대한 자연스러운 문구
            3. 친밀도 레벨에 맞는 어투 사용
            4. 사용자의 관심을 끌 수 있는 문구
            
            응답 형식:
            {
              "title": "제목",
              "body": "본문"
            }
            """, concept, intimacyLevel);
    }
    
    private PushNotificationText parseNotificationText(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            String title = jsonNode.get("title").asText();
            String body = jsonNode.get("body").asText();
            return new PushNotificationText(title, body);
        } catch (Exception e) {
            log.error("푸시 알림 텍스트 파싱 실패", e);
            // Fallback
            return new PushNotificationText("새로운 대화를 시작해볼까요?", "지금 바로 대화를 시작해보세요!");
        }
    }
}

// DTO
public record PushNotificationText(String title, String body) {}
```

### 2. PushNotificationService 구현

**목적**: 푸시 알림 발송 로직 관리

**위치**: `chat/src/main/java/com/dorandoran/chat/service/PushNotificationService.java`

**구현 예시**:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {
    private final PushNotificationAgent pushNotificationAgent;
    private final FCMService fcmService;
    private final FcmTokenRepository fcmTokenRepository;
    private final ChatbotRepository chatbotRepository;
    private final UserRepository userRepository;
    
    /**
     * 푸시 알림 발송 (단일 사용자)
     */
    public Mono<Void> sendPushNotification(
            UUID userId, UUID chatbotId, String topic, 
            String concept, Integer intimacyLevel) {
        
        return Mono.fromCallable(() -> {
            // 1. 사용자 및 챗봇 정보 조회
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            Chatbot chatbot = chatbotRepository.findById(chatbotId)
                .orElseThrow(() -> new RuntimeException("Chatbot not found: " + chatbotId));
            
            // 2. AI로 푸시 텍스트 생성
            PushNotificationText text = pushNotificationAgent
                .generateNotificationText(userId, chatbotId, topic, concept, intimacyLevel)
                .block(); // Mono를 Block (동기 처리)
            
            // 3. FCM 토큰 조회
            List<FcmToken> tokens = fcmTokenRepository.findByUserId(userId);
            if (tokens.isEmpty()) {
                log.warn("FCM 토큰이 없습니다: userId={}", userId);
                return null;
            }
            
            // 4. 딥링크 URL 생성
            String deepLink = buildDeepLink(chatbotId, topic, concept, intimacyLevel);
            
            // 5. FCM 발송
            for (FcmToken token : tokens) {
                fcmService.sendNotification(
                    token.getToken(),
                    text.title(),
                    text.body(),
                    deepLink,
                    token.getPlatform()
                );
            }
            
            log.info("푸시 알림 발송 완료: userId={}, chatbotId={}, topic={}", 
                userId, chatbotId, topic);
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    /**
     * 딥링크 URL 생성
     */
    private String buildDeepLink(UUID chatbotId, String topic, 
                                  String concept, Integer intimacyLevel) {
        // URL 인코딩
        String encodedTopic = URLEncoder.encode(topic, StandardCharsets.UTF_8);
        return String.format(
            "dorandoran://chatroom/create?chatbotId=%s&topic=%s&concept=%s&intimacyLevel=%d",
            chatbotId, encodedTopic, concept, intimacyLevel
        );
    }
    
    /**
     * 정기 푸시 알림 발송 (배치 작업)
     */
    @Scheduled(cron = "0 0 10 * * *") // 매일 오전 10시
    public void sendDailyPushNotifications() {
        // 1. 발송 대상 사용자 조회 (예: 24시간 이상 미접속 사용자)
        List<UUID> targetUserIds = findInactiveUsers(Duration.ofHours(24));
        
        // 2. 각 사용자에게 랜덤 챗봇 + 주제로 푸시 발송
        for (UUID userId : targetUserIds) {
            try {
                // 랜덤 챗봇 선택
                Chatbot chatbot = selectRandomChatbot(userId);
                
                // 컨셉별 랜덤 주제 선택
                String topic = selectRandomTopic(chatbot.getConcept());
                
                // 친밀도 레벨 조회 (기본값: 2)
                int intimacyLevel = getDefaultIntimacyLevel(userId, chatbot.getId());
                
                // 푸시 발송
                sendPushNotification(
                    userId, chatbot.getId(), topic, 
                    chatbot.getConcept(), intimacyLevel
                ).block();
                
            } catch (Exception e) {
                log.error("정기 푸시 알림 발송 실패: userId={}", userId, e);
            }
        }
    }
}
```

### 3. ChatService 확장

**목적**: 주제 정보를 contextData에 저장하는 기능 추가

**수정 위치**: `chat/src/main/java/com/dorandoran/chat/service/ChatService.java`

**추가 메서드**:

```java
/**
 * 채팅방 생성 또는 조회 (주제 포함)
 */
@Transactional
public ChatRoom getOrCreateRoomWithTopic(
        UUID userId, UUID chatbotId, String name, 
        String concept, Integer intimacyLevel, String topic) {
    
    // 1. 기존 채팅방 조회 또는 생성
    ChatRoom room = getOrCreateRoom(userId, chatbotId, name, concept, intimacyLevel, null);
    
    // 2. contextData에 주제 저장
    if (topic != null && !topic.isBlank()) {
        updateRoomTopic(room.getId(), topic);
    }
    
    return room;
}

/**
 * 채팅방의 주제 정보 업데이트
 */
@Transactional
public void updateRoomTopic(UUID chatroomId, String topic) {
    ChatRoom room = chatRoomRepository.findById(chatroomId)
        .orElseThrow(() -> new RuntimeException("ChatRoom not found: " + chatroomId));
    
    // contextData 초기화 (없으면)
    ObjectNode contextData = (room.getContextData() != null && !room.getContextData().isNull())
        ? (ObjectNode) room.getContextData()
        : objectMapper.createObjectNode();
    
    // sessionData 초기화
    ObjectNode sessionData = contextData.has("sessionData")
        ? (ObjectNode) contextData.get("sessionData")
        : objectMapper.createObjectNode();
    
    // currentTopic 저장
    sessionData.put("currentTopic", topic);
    contextData.set("sessionData", sessionData);
    
    room.setContextData(contextData);
    room.setUpdatedAt(LocalDateTime.now());
    chatRoomRepository.save(room);
    
    log.info("채팅방 주제 업데이트: chatroomId={}, topic={}", chatroomId, topic);
}
```

### 4. GreetingService 확장

**목적**: 주제 정보를 활용하여 인사말 생성

**수정 위치**: `chat/src/main/java/com/dorandoran/chat/service/GreetingService.java`

**수정 내용**:

```java
@Transactional
public GreetingResponse sendGreeting(UUID chatroomId, UUID userId, 
                                     ChatRoomConcept concept, int intimacyLevel) {
    try {
        ChatRoom chatRoom = chatService.getChatRoomById(chatroomId);
        UUID chatbotId = chatRoom.getChatbot().getId();
        
        // contextData에서 주제 추출
        String topic = extractTopicFromContextData(chatRoom.getContextData());
        
        // AI로 인사말 생성 (주제 포함)
        GreetingResponse greetingResponse = generateAIGreeting(
            chatroomId, concept, intimacyLevel, topic
        );
        
        // ... 기존 로직 ...
    } catch (Exception e) {
        // ... 에러 처리 ...
    }
}

private String extractTopicFromContextData(JsonNode contextData) {
    if (contextData == null || contextData.isNull()) {
        return null;
    }
    
    try {
        if (contextData.has("sessionData")) {
            JsonNode sessionData = contextData.get("sessionData");
            if (sessionData.has("currentTopic")) {
                return sessionData.get("currentTopic").asText();
            }
        }
    } catch (Exception e) {
        log.warn("주제 추출 실패", e);
    }
    
    return null;
}

private GreetingResponse generateAIGreeting(UUID chatroomId, 
                                           ChatRoomConcept concept, 
                                           int intimacyLevel, 
                                           String topic) {
    String systemPrompt = buildGreetingSystemPrompt(concept, intimacyLevel);
    
    // 주제가 있으면 해당 주제 사용, 없으면 랜덤 선택
    String selectedTopic = (topic != null && !topic.isBlank())
        ? topic
        : selectRandomTopic(concept);
    
    // userMessage에 주제 포함
    String userMessage = String.format(
        "첫 인사말을 작성해주세요. 이번에는 주제 '%s'을 사용하여 인사말을 생성하세요.",
        selectedTopic
    );
    
    // ... 기존 로직 ...
}
```

---

## 데이터베이스 설계

### 1. FCM 토큰 테이블

**위치**: `user` 서비스 또는 새로운 `notification` 서비스

```sql
CREATE TABLE fcm_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token TEXT NOT NULL,
    platform VARCHAR(20) NOT NULL, -- 'ios' or 'android'
    device_id VARCHAR(255), -- 디바이스 식별자 (선택적)
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, token)
);

CREATE INDEX idx_fcm_tokens_user_id ON fcm_tokens(user_id);
CREATE INDEX idx_fcm_tokens_platform ON fcm_tokens(platform);
```

### 2. 푸시 알림 발송 이력 테이블 (선택적)

```sql
CREATE TABLE push_notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    chatbot_id UUID NOT NULL,
    topic VARCHAR(255),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    deep_link TEXT,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    clicked_at TIMESTAMP, -- 딥링크 클릭 시각
    chatroom_id UUID, -- 생성된 채팅방 ID
    status VARCHAR(20) NOT NULL DEFAULT 'sent', -- 'sent', 'clicked', 'failed'
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_push_logs_user_id ON push_notification_logs(user_id);
CREATE INDEX idx_push_logs_sent_at ON push_notification_logs(sent_at);
CREATE INDEX idx_push_logs_status ON push_notification_logs(status);
```

### 3. ChatRoom 테이블 확장 (이미 존재)

**확인 사항**: `context_data` JSONB 필드에 `sessionData.currentTopic` 저장 가능

**예시 데이터**:
```json
{
  "sessionData": {
    "currentTopic": "요리/음식"
  },
  "conversationSummary": "...",
  "userPreferences": {...}
}
```

---

## API 설계

### 1. FCM 토큰 등록 API

**엔드포인트**: `POST /api/notifications/register`

**요청**:
```json
{
  "token": "fcm_token_string",
  "platform": "android" // or "ios"
}
```

**응답**: `204 No Content`

**구현 위치**: `user` 서비스 또는 새로운 `notification` 서비스

### 2. 푸시 알림 발송 API (관리자용)

**엔드포인트**: `POST /api/notifications/send`

**요청**:
```json
{
  "userId": "uuid",
  "chatbotId": "uuid",
  "topic": "요리/음식",
  "concept": "FRIEND",
  "intimacyLevel": 2
}
```

**응답**: `200 OK`

**구현 위치**: `chat` 서비스

### 3. 딥링크 처리 API

**엔드포인트**: `GET /api/deeplink/chatroom/create`

**쿼리 파라미터**:
- `chatbotId`: 챗봇 ID
- `topic`: 대화 주제
- `concept`: 컨셉 (FRIEND, HONEY, etc.)
- `intimacyLevel`: 친밀도 레벨

**응답**: `200 OK` (채팅방 정보)

**구현 위치**: `chat` 서비스

---

## 프론트엔드 구현

### 1. Capacitor Push Notifications 설정

**설치**:
```bash
npm install @capacitor/push-notifications
npx cap sync
```

**구현 예시**: `src/services/pushNotificationService.ts`

```typescript
import { PushNotifications } from '@capacitor/push-notifications';
import { Capacitor } from '@capacitor/core';

export class PushNotificationService {
  async initialize() {
    if (!Capacitor.isNativePlatform()) {
      return;
    }

    // 권한 요청
    let permStatus = await PushNotifications.checkPermissions();
    if (permStatus.receive === 'prompt') {
      permStatus = await PushNotifications.requestPermissions();
    }
    
    if (permStatus.receive !== 'granted') {
      throw new Error('푸시 알림 권한이 거부되었습니다.');
    }

    // FCM 토큰 등록
    await PushNotifications.register();

    // 이벤트 리스너
    PushNotifications.addListener('registration', async (token) => {
      console.log('FCM 토큰:', token.value);
      await this.registerTokenToBackend(token.value);
    });

    PushNotifications.addListener('pushNotificationActionPerformed', (action) => {
      console.log('푸시 알림 클릭:', action);
      this.handlePushNotificationClick(action.notification);
    });
  }

  private async registerTokenToBackend(token: string) {
    try {
      await fetch('/api/notifications/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${await this.getAccessToken()}`,
        },
        body: JSON.stringify({
          token,
          platform: Capacitor.getPlatform(), // 'ios' or 'android'
        }),
      });
    } catch (error) {
      console.error('토큰 등록 실패:', error);
    }
  }

  private handlePushNotificationClick(notification: any) {
    // 딥링크 URL 추출
    const deepLink = notification.data?.deepLink || notification.data?.url;
    
    if (deepLink) {
      // 딥링크 처리
      this.handleDeepLink(deepLink);
    }
  }

  private handleDeepLink(url: string) {
    // URL 파싱: dorandoran://chatroom/create?chatbotId=...&topic=...
    const urlObj = new URL(url);
    const params = new URLSearchParams(urlObj.search);
    
    const chatbotId = params.get('chatbotId');
    const topic = params.get('topic');
    const concept = params.get('concept');
    const intimacyLevel = params.get('intimacyLevel');
    
    // 채팅방 생성 API 호출
    this.createChatroomFromDeepLink({
      chatbotId,
      topic,
      concept,
      intimacyLevel: intimacyLevel ? parseInt(intimacyLevel) : 2,
    });
  }

  private async createChatroomFromDeepLink(params: {
    chatbotId: string;
    topic: string | null;
    concept: string | null;
    intimacyLevel: number;
  }) {
    try {
      // 채팅방 생성 API 호출
      const response = await fetch('/api/chatrooms', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${await this.getAccessToken()}`,
        },
        body: JSON.stringify({
          chatbotId: params.chatbotId,
          name: '새 대화',
          concept: params.concept || 'FRIEND',
          intimacyLevel: params.intimacyLevel,
          topic: params.topic, // 주제 정보 전달
        }),
      });

      const chatroom = await response.json();
      
      // 채팅 화면으로 이동
      this.navigateToChatroom(chatroom.id);
    } catch (error) {
      console.error('채팅방 생성 실패:', error);
    }
  }
}
```

### 2. 딥링크 처리 (App.tsx 또는 라우터)

```typescript
import { App } from '@capacitor/app';

useEffect(() => {
  // 앱이 딥링크로 열렸을 때 처리
  App.addListener('appUrlOpen', (event) => {
    const url = event.url;
    // 딥링크 처리 로직
    handleDeepLink(url);
  });
}, []);
```

---

## 딥링크 처리

### 1. 딥링크 URL 형식

```
dorandoran://chatroom/create?chatbotId={uuid}&topic={encoded_topic}&concept={concept}&intimacyLevel={level}
```

**예시**:
```
dorandoran://chatroom/create?chatbotId=123e4567-e89b-12d3-a456-426614174000&topic=%EC%9A%94%EB%A6%AC%2F%EC%9D%8C%EC%8B%9D&concept=FRIEND&intimacyLevel=2
```

### 2. 백엔드 딥링크 처리 API

**엔드포인트**: `POST /api/chatrooms` (기존 API 확장)

**요청에 topic 필드 추가**:
```json
{
  "chatbotId": "uuid",
  "name": "새 대화",
  "concept": "FRIEND",
  "intimacyLevel": 2,
  "topic": "요리/음식"  // 새로 추가
}
```

**처리 로직**:
1. `ChatService.getOrCreateRoomWithTopic()` 호출
2. 주제 정보를 `contextData.sessionData.currentTopic`에 저장
3. `GreetingService.sendGreeting()` 호출 (주제 포함)
4. 채팅방 정보 반환

---

## 개발 일정

### Phase 1: 백엔드 기본 구현 (3일)

- [ ] FCM 토큰 테이블 생성 및 마이그레이션
- [ ] FCM 토큰 등록 API 구현
- [ ] FCMService 구현 (Firebase Admin SDK 통합)
- [ ] PushNotificationAgent 구현
- [ ] PushNotificationService 구현

### Phase 2: 채팅방 생성 확장 (2일)

- [ ] ChatService에 `getOrCreateRoomWithTopic()` 메서드 추가
- [ ] `updateRoomTopic()` 메서드 구현
- [ ] GreetingService에 주제 추출 및 활용 로직 추가
- [ ] 딥링크 처리 API 구현

### Phase 3: 프론트엔드 구현 (2일)

- [ ] Capacitor Push Notifications 플러그인 설치 및 설정
- [ ] FCM 토큰 등록 로직 구현
- [ ] 딥링크 처리 로직 구현
- [ ] 채팅방 생성 및 이동 로직 구현

### Phase 4: 통합 테스트 및 최적화 (2일)

- [ ] 푸시 알림 발송 테스트
- [ ] 딥링크 클릭 테스트
- [ ] 채팅방 생성 및 주제 기억 테스트
- [ ] 에러 처리 및 로깅 개선

**총 예상 시간: 9일**

---

## 참고 사항

### 1. 프롬프트 관리

- `PUSH_NOTIFICATION` 타입의 프롬프트를 DB에 추가
- 컨셉별, 친밀도 레벨별 프롬프트 관리
- `PromptLoaderService`를 통해 로드

### 2. 에러 처리

- FCM 토큰 등록 실패 시 재시도 로직
- 푸시 발송 실패 시 로깅 및 알림
- 딥링크 파싱 실패 시 기본 동작

### 3. 성능 최적화

- 푸시 알림 텍스트 생성은 비동기 처리
- FCM 발송은 배치 처리 고려
- 딥링크 처리 시 캐싱 활용

### 4. 보안

- FCM 토큰은 암호화 저장 (선택적)
- 딥링크 파라미터 검증
- 사용자 인증 확인

---

**문서 작성일**: 2025-01-XX  
**작성자**: AI Assistant  
**검토 필요**: 백엔드 개발자, 프론트엔드 개발자, 팀 리더
