# 📊 Google Analytics 태깅 가이드

> DoranDoran 프로젝트의 Google Analytics 4 (GA4) 설정 및 이벤트 추적 가이드

## 📋 목차

1. [개요](#개요)
2. [GA4 설정](#ga4-설정)
3. [프론트엔드 구현](#프론트엔드-구현)
4. [백엔드 구현](#백엔드-구현)
5. [이벤트 정의](#이벤트-정의)
6. [Kafka 로그 수집 (선택사항)](#kafka-로그-수집)
7. [모니터링 및 분석](#모니터링-및-분석)

---

## 🎯 개요

### GA(Google Analytics) 태깅이란?

Google Analytics 태깅은 웹사이트나 애플리케이션에서 사용자 행동 데이터를 수집하고 분석하기 위한 설정입니다.

### 수집할 데이터 유형

#### 프론트엔드
- 페이지 뷰 (Page Views)
- 버튼 클릭 이벤트
- 사용자 상호작용 (스크롤, 시간)
- 폼 제출
- 모달 열기/닫기
- 채팅 메시지 전송
- 사용자 등록/로그인

#### 백엔드
- API 호출 로그
- 응답 시간 (Latency)
- 에러 로그
- 사용자 행동 패턴
- 비즈니스 메트릭
- 성능 메트릭

---

## 🔧 GA4 설정

### 1. Google Analytics 계정 생성

#### Step 1: GA4 계정 만들기

1. [Google Analytics](https://analytics.google.com/) 접속
2. "관리" → "계정 만들기" 클릭
3. 계정 이름 입력: `DoranDoran`
4. 속성 만들기:
   - 속성 이름: `DoranDoran Production`
   - 시간대: `대한민국`
   - 통화: `대한민국 원 (₩)`

#### Step 2: 데이터 스트림 설정

1. "관리" → "데이터 스트림" → "스트림 추가" → "웹"
2. 웹사이트 URL 입력: `https://dorandoran.com`
3. 스트림 이름: `DoranDoran Web`
4. **측정 ID** 복사: `G-XXXXXXXXXX` (이것이 중요!)

#### Step 3: 개발/스테이징 환경 설정

프로덕션과 별도로 개발 환경용 스트림도 생성:
- 개발: `DoranDoran Development` → `G-YYYYYYYYYY`
- 스테이징: `DoranDoran Staging` → `G-ZZZZZZZZZZ`

---

## 🎨 프론트엔드 구현

### 1. 패키지 설치

```bash
cd dorandoran-frontend
npm install react-ga4 @types/react-ga4
```

### 2. GA 설정 파일 생성

**`src/config/analytics.ts`** 생성:

```typescript
import ReactGA from 'react-ga4';

// 환경에 따른 측정 ID 설정
const GA_MEASUREMENT_IDS = {
  production: 'G-XXXXXXXXXX',
  staging: 'G-YYYYYYYYYY',
  development: 'G-ZZZZZZZZZZ'
};

const getEnvironment = (): 'production' | 'staging' | 'development' => {
  const hostname = window.location.hostname;
  
  if (hostname === 'dorandoran.com' || hostname === 'www.dorandoran.com') {
    return 'production';
  } else if (hostname.includes('staging')) {
    return 'staging';
  }
  return 'development';
};

// GA 초기화
export const initGA = () => {
  const env = getEnvironment();
  const measurementId = GA_MEASUREMENT_IDS[env];
  
  // 개발 환경에서는 디버그 모드 활성화
  const gaOptions = {
    debug_mode: env === 'development',
    testMode: env === 'development'
  };
  
  ReactGA.initialize(measurementId, gaOptions);
  
  console.log(`Google Analytics initialized for ${env} environment`);
};

// 페이지 뷰 추적
export const trackPageView = (path: string, title?: string) => {
  ReactGA.send({ 
    hitType: 'pageview', 
    page: path,
    title: title || document.title
  });
  
  console.log(`GA: Page view tracked - ${path}`);
};

// 이벤트 추적
export const trackEvent = (
  category: string,
  action: string,
  label?: string,
  value?: number
) => {
  ReactGA.event({
    category,
    action,
    label,
    value
  });
  
  console.log(`GA: Event tracked - ${category}/${action}`);
};

// 사용자 속성 설정
export const setUserProperties = (properties: Record<string, any>) => {
  ReactGA.set(properties);
};

// 사용자 ID 설정
export const setUserId = (userId: string) => {
  ReactGA.set({ userId });
};

// 커스텀 이벤트 타입 정의
export const GAEvents = {
  // 사용자 관련
  USER_LOGIN: { category: 'User', action: 'Login' },
  USER_LOGOUT: { category: 'User', action: 'Logout' },
  USER_REGISTER: { category: 'User', action: 'Register' },
  USER_PROFILE_VIEW: { category: 'User', action: 'View Profile' },
  USER_PROFILE_EDIT: { category: 'User', action: 'Edit Profile' },
  
  // 채팅 관련
  CHAT_ROOM_CREATE: { category: 'Chat', action: 'Create Room' },
  CHAT_MESSAGE_SEND: { category: 'Chat', action: 'Send Message' },
  CHAT_ROOM_ENTER: { category: 'Chat', action: 'Enter Room' },
  CHAT_ROOM_LEAVE: { category: 'Chat', action: 'Leave Room' },
  
  // UI 상호작용
  BUTTON_CLICK: { category: 'UI', action: 'Button Click' },
  MODAL_OPEN: { category: 'UI', action: 'Modal Open' },
  MODAL_CLOSE: { category: 'UI', action: 'Modal Close' },
  FORM_SUBMIT: { category: 'UI', action: 'Form Submit' },
  
  // 검색 관련
  SEARCH_QUERY: { category: 'Search', action: 'Search Query' },
  SEARCH_RESULT_CLICK: { category: 'Search', action: 'Result Click' },
  
  // 에러
  ERROR_OCCURRED: { category: 'Error', action: 'Error Occurred' }
};
```

### 3. App.tsx에 GA 초기화

**`src/App.tsx`** 수정:

```tsx
import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { initGA, trackPageView } from './config/analytics';

function App() {
  const location = useLocation();
  
  // GA 초기화 (앱 시작 시 한 번만)
  useEffect(() => {
    initGA();
  }, []);
  
  // 페이지 변경 추적
  useEffect(() => {
    trackPageView(location.pathname);
  }, [location]);
  
  return (
    // ... 기존 코드
  );
}

export default App;
```

### 4. 컴포넌트에서 이벤트 추적

#### 로그인 컴포넌트 예시

**`src/components/auth/LoginForm.tsx`**:

```tsx
import { trackEvent, GAEvents, setUserId } from '../../config/analytics';

const LoginForm = () => {
  const handleLogin = async (email: string, password: string) => {
    try {
      const response = await loginAPI(email, password);
      
      // 로그인 성공 시 GA 이벤트
      trackEvent(
        GAEvents.USER_LOGIN.category,
        GAEvents.USER_LOGIN.action,
        email,
        1
      );
      
      // 사용자 ID 설정
      setUserId(response.userId);
      
      // 사용자 속성 설정
      setUserProperties({
        user_role: response.role,
        account_type: response.accountType
      });
      
    } catch (error) {
      // 에러 추적
      trackEvent(
        GAEvents.ERROR_OCCURRED.category,
        GAEvents.ERROR_OCCURRED.action,
        'Login Failed',
        0
      );
    }
  };
  
  return (
    // ... JSX
  );
};
```

#### 채팅 컴포넌트 예시

**`src/components/chat/ChatRoom.tsx`**:

```tsx
import { trackEvent, GAEvents } from '../../config/analytics';

const ChatRoom = ({ roomId }: { roomId: string }) => {
  
  // 채팅방 입장 추적
  useEffect(() => {
    trackEvent(
      GAEvents.CHAT_ROOM_ENTER.category,
      GAEvents.CHAT_ROOM_ENTER.action,
      roomId
    );
    
    return () => {
      // 채팅방 퇴장 추적
      trackEvent(
        GAEvents.CHAT_ROOM_LEAVE.category,
        GAEvents.CHAT_ROOM_LEAVE.action,
        roomId
      );
    };
  }, [roomId]);
  
  const handleSendMessage = (message: string) => {
    // 메시지 전송 추적
    trackEvent(
      GAEvents.CHAT_MESSAGE_SEND.category,
      GAEvents.CHAT_MESSAGE_SEND.action,
      roomId,
      message.length // 메시지 길이를 value로
    );
    
    // ... 메시지 전송 로직
  };
  
  return (
    // ... JSX
  );
};
```

#### 버튼 클릭 추적 Hook

**`src/hooks/useGATracking.ts`** 생성:

```tsx
import { useCallback } from 'react';
import { trackEvent, GAEvents } from '../config/analytics';

export const useGATracking = () => {
  const trackButtonClick = useCallback((buttonName: string, context?: string) => {
    trackEvent(
      GAEvents.BUTTON_CLICK.category,
      GAEvents.BUTTON_CLICK.action,
      `${buttonName}${context ? ` - ${context}` : ''}`
    );
  }, []);
  
  const trackModalOpen = useCallback((modalName: string) => {
    trackEvent(
      GAEvents.MODAL_OPEN.category,
      GAEvents.MODAL_OPEN.action,
      modalName
    );
  }, []);
  
  const trackModalClose = useCallback((modalName: string) => {
    trackEvent(
      GAEvents.MODAL_CLOSE.category,
      GAEvents.MODAL_CLOSE.action,
      modalName
    );
  }, []);
  
  return {
    trackButtonClick,
    trackModalOpen,
    trackModalClose
  };
};

// 사용 예시
const MyComponent = () => {
  const { trackButtonClick } = useGATracking();
  
  return (
    <button onClick={() => {
      trackButtonClick('Submit', 'User Registration');
      // ... 실제 로직
    }}>
      회원가입
    </button>
  );
};
```

### 5. 환경 변수 설정

**`.env.production`**:
```env
VITE_GA_MEASUREMENT_ID=G-XXXXXXXXXX
VITE_ENABLE_GA=true
```

**`.env.development`**:
```env
VITE_GA_MEASUREMENT_ID=G-ZZZZZZZZZZ
VITE_ENABLE_GA=false  # 개발 시 GA 비활성화
```

---

## 🖥️ 백엔드 구현

### 1. 의존성 추가

**`build.gradle`** (각 서비스별로 추가):

```gradle
dependencies {
    // Google Analytics Data API
    implementation 'com.google.analytics:google-analytics-data:0.40.0'
    
    // Micrometer (메트릭 수집)
    implementation 'io.micrometer:micrometer-core'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    
    // Logback (로깅)
    implementation 'ch.qos.logback:logback-classic'
    implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
}
```

### 2. 백엔드 이벤트 로깅 설정

#### 공통 이벤트 로거 생성

**`common/src/main/java/com/dorandoran/common/analytics/AnalyticsEvent.java`**:

```java
package com.dorandoran.common.analytics;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AnalyticsEvent {
    private String eventName;
    private String category;
    private String action;
    private String label;
    private Integer value;
    private String userId;
    private String sessionId;
    private Map<String, Object> customDimensions;
    private LocalDateTime timestamp;
    
    public static AnalyticsEvent of(String eventName, String category, String action) {
        return AnalyticsEvent.builder()
                .eventName(eventName)
                .category(category)
                .action(action)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
```

**`common/src/main/java/com/dorandoran/common/analytics/AnalyticsLogger.java`**:

```java
package com.dorandoran.common.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsLogger {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 분석 이벤트 로깅
     */
    public void logEvent(AnalyticsEvent event) {
        try {
            // JSON 형태로 구조화된 로그 출력
            String jsonLog = objectMapper.writeValueAsString(event);
            
            // analytics 전용 로거에 기록 (logback 설정에서 분리 가능)
            log.info("ANALYTICS_EVENT: {}", jsonLog);
            
        } catch (Exception e) {
            log.error("Failed to log analytics event", e);
        }
    }
    
    /**
     * API 호출 메트릭 로깅
     */
    public void logApiCall(String endpoint, String method, int statusCode, long duration) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("endpoint", endpoint);
        metrics.put("method", method);
        metrics.put("statusCode", statusCode);
        metrics.put("duration_ms", duration);
        
        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventName("api_call")
                .category("API")
                .action(method + " " + endpoint)
                .value((int) duration)
                .customDimensions(metrics)
                .build();
        
        logEvent(event);
    }
    
    /**
     * 사용자 행동 로깅
     */
    public void logUserAction(String userId, String action, String context) {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventName("user_action")
                .category("User")
                .action(action)
                .label(context)
                .userId(userId)
                .build();
        
        logEvent(event);
    }
    
    /**
     * 비즈니스 메트릭 로깅
     */
    public void logBusinessMetric(String metricName, int value, Map<String, Object> dimensions) {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventName("business_metric")
                .category("Business")
                .action(metricName)
                .value(value)
                .customDimensions(dimensions)
                .build();
        
        logEvent(event);
    }
}
```

### 3. AOP를 활용한 자동 로깅

**`common/src/main/java/com/dorandoran/common/analytics/AnalyticsAspect.java`**:

```java
package com.dorandoran.common.analytics;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AnalyticsAspect {
    
    private final AnalyticsLogger analyticsLogger;
    
    /**
     * RestController의 모든 메서드에 대해 API 호출 로깅
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String endpoint = request.getRequestURI();
            String method = request.getMethod();
            
            try {
                Object result = joinPoint.proceed();
                long duration = System.currentTimeMillis() - startTime;
                
                // 성공적인 API 호출 로깅
                analyticsLogger.logApiCall(endpoint, method, 200, duration);
                
                return result;
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                
                // 실패한 API 호출 로깅
                analyticsLogger.logApiCall(endpoint, method, 500, duration);
                
                throw e;
            }
        }
        
        return joinPoint.proceed();
    }
}
```

### 4. 서비스별 이벤트 로깅

#### User Service 예시

**`user/src/main/java/com/dorandoran/user/service/UserService.java`**:

```java
package com.dorandoran.user.service;

import com.dorandoran.common.analytics.AnalyticsLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final AnalyticsLogger analyticsLogger;
    
    public User registerUser(RegisterRequest request) {
        // 사용자 등록 로직
        User user = createUser(request);
        userRepository.save(user);
        
        // GA 이벤트 로깅
        analyticsLogger.logUserAction(
            user.getId().toString(),
            "register",
            "New user registration"
        );
        
        // 비즈니스 메트릭 로깅
        Map<String, Object> dimensions = new HashMap<>();
        dimensions.put("registration_source", request.getSource());
        dimensions.put("user_type", request.getUserType());
        
        analyticsLogger.logBusinessMetric(
            "user_registration",
            1,
            dimensions
        );
        
        return user;
    }
    
    public User updateUserProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserById(userId);
        
        // 프로필 업데이트 로직
        user.updateProfile(request);
        userRepository.save(user);
        
        // GA 이벤트 로깅
        analyticsLogger.logUserAction(
            userId.toString(),
            "profile_update",
            "User profile updated"
        );
        
        return user;
    }
}
```

#### Chat Service 예시

**`chat/src/main/java/com/dorandoran/chat/service/ChatService.java`**:

```java
package com.dorandoran.chat.service;

import com.dorandoran.common.analytics.AnalyticsLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final AnalyticsLogger analyticsLogger;
    
    public ChatRoom createChatRoom(UUID userId, String roomName) {
        ChatRoom chatRoom = ChatRoom.create(userId, roomName);
        chatRoomRepository.save(chatRoom);
        
        // 채팅방 생성 이벤트 로깅
        analyticsLogger.logUserAction(
            userId.toString(),
            "chat_room_create",
            "Created chat room: " + roomName
        );
        
        // 비즈니스 메트릭
        Map<String, Object> dimensions = new HashMap<>();
        dimensions.put("room_id", chatRoom.getId().toString());
        dimensions.put("room_name", roomName);
        
        analyticsLogger.logBusinessMetric(
            "chat_room_created",
            1,
            dimensions
        );
        
        return chatRoom;
    }
    
    public Message sendMessage(UUID roomId, UUID userId, String content) {
        Message message = Message.create(roomId, userId, content);
        messageRepository.save(message);
        
        // 메시지 전송 이벤트 로깅
        analyticsLogger.logUserAction(
            userId.toString(),
            "message_send",
            "Sent message in room: " + roomId
        );
        
        // 비즈니스 메트릭
        Map<String, Object> dimensions = new HashMap<>();
        dimensions.put("room_id", roomId.toString());
        dimensions.put("message_length", content.length());
        
        analyticsLogger.logBusinessMetric(
            "message_sent",
            1,
            dimensions
        );
        
        return message;
    }
}
```

### 5. Logback 설정

**`src/main/resources/logback-spring.xml`**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 기본 콘솔 출력 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Analytics 전용 파일 출력 -->
    <appender name="ANALYTICS_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/analytics.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/analytics.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- JSON 형식으로 출력 -->
        </encoder>
        <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
            <evaluator>
                <expression>return message.contains("ANALYTICS_EVENT");</expression>
            </evaluator>
            <OnMatch>ACCEPT</OnMatch>
            <OnMismatch>DENY</OnMismatch>
        </filter>
    </appender>
    
    <!-- API 호출 전용 파일 출력 -->
    <appender name="API_METRICS_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/api-metrics.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/api-metrics.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
    </appender>
    
    <!-- Analytics Logger -->
    <logger name="com.dorandoran.common.analytics" level="INFO" additivity="false">
        <appender-ref ref="ANALYTICS_FILE" />
        <appender-ref ref="CONSOLE" />
    </logger>
    
    <!-- Root Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

---

## 📝 이벤트 정의

### 이벤트 카테고리 및 액션 정의서

프론트엔드와 백엔드에서 공통으로 사용할 이벤트 정의:

| 카테고리 | 액션 | 설명 | 수집 위치 | 우선순위 |
|---------|------|------|----------|---------|
| **User** | Login | 사용자 로그인 | Frontend + Backend | High |
| **User** | Logout | 사용자 로그아웃 | Frontend + Backend | High |
| **User** | Register | 사용자 회원가입 | Frontend + Backend | High |
| **User** | Profile View | 프로필 조회 | Frontend | Medium |
| **User** | Profile Edit | 프로필 수정 | Frontend + Backend | Medium |
| **Chat** | Room Create | 채팅방 생성 | Frontend + Backend | High |
| **Chat** | Room Enter | 채팅방 입장 | Frontend | High |
| **Chat** | Room Leave | 채팅방 퇴장 | Frontend | Medium |
| **Chat** | Message Send | 메시지 전송 | Frontend + Backend | High |
| **Chat** | Message Receive | 메시지 수신 | Frontend | Medium |
| **UI** | Button Click | 버튼 클릭 | Frontend | Low |
| **UI** | Modal Open | 모달 열기 | Frontend | Low |
| **UI** | Modal Close | 모달 닫기 | Frontend | Low |
| **UI** | Form Submit | 폼 제출 | Frontend | Medium |
| **UI** | Scroll | 스크롤 | Frontend | Low |
| **Search** | Query | 검색 쿼리 | Frontend | Medium |
| **Search** | Result Click | 검색 결과 클릭 | Frontend | Medium |
| **API** | Call | API 호출 | Backend | High |
| **API** | Error | API 에러 | Backend | High |
| **Error** | Occurred | 에러 발생 | Frontend + Backend | High |
| **Performance** | Page Load | 페이지 로드 시간 | Frontend | Medium |
| **Performance** | API Latency | API 응답 시간 | Backend | High |

### 이벤트 명명 규칙

```
[category]_[action]_[optional_context]

예시:
- user_login_success
- user_login_failed
- chat_message_send
- chat_room_create
- api_call_user_service
- error_404_not_found
```

---

## 🚀 Kafka 로그 수집 (선택사항)

대량의 로그를 처리하고 실시간 분석이 필요한 경우 Kafka를 도입할 수 있습니다.

### 1. Kafka 설정

**`docker/docker-compose.yml`**에 Kafka 추가:

```yaml
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
  
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    depends_on:
      - kafka
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```

### 2. Kafka Producer 설정

**`build.gradle`**에 의존성 추가:

```gradle
dependencies {
    implementation 'org.springframework.kafka:spring-kafka'
}
```

**`application.yml`**:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.type.mapping: analyticsEvent:com.dorandoran.common.analytics.AnalyticsEvent
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.dorandoran.common.analytics
      group-id: analytics-consumer-group
```

### 3. Kafka를 활용한 Analytics Service

**`common/src/main/java/com/dorandoran/common/analytics/KafkaAnalyticsProducer.java`**:

```java
package com.dorandoran.common.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAnalyticsProducer {
    
    private static final String ANALYTICS_TOPIC = "analytics-events";
    
    private final KafkaTemplate<String, AnalyticsEvent> kafkaTemplate;
    
    /**
     * Kafka로 분석 이벤트 전송
     */
    public void sendEvent(AnalyticsEvent event) {
        try {
            kafkaTemplate.send(ANALYTICS_TOPIC, event.getEventName(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Analytics event sent to Kafka: {}", event.getEventName());
                    } else {
                        log.error("Failed to send analytics event to Kafka", ex);
                    }
                });
        } catch (Exception e) {
            log.error("Error sending analytics event to Kafka", e);
        }
    }
}
```

**Kafka Consumer (별도 Analytics Service 또는 Batch Service)**:

```java
package com.dorandoran.batch.consumer;

import com.dorandoran.common.analytics.AnalyticsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventConsumer {
    
    private final AnalyticsRepository analyticsRepository;
    
    @KafkaListener(topics = "analytics-events", groupId = "analytics-consumer-group")
    public void consumeAnalyticsEvent(AnalyticsEvent event) {
        try {
            log.info("Received analytics event: {}", event.getEventName());
            
            // 데이터베이스에 저장
            analyticsRepository.save(event);
            
            // 실시간 분석 처리
            processRealTimeAnalytics(event);
            
            // 필요시 외부 시스템으로 전송 (BigQuery, Elasticsearch 등)
            forwardToExternalSystem(event);
            
        } catch (Exception e) {
            log.error("Error processing analytics event", e);
        }
    }
    
    private void processRealTimeAnalytics(AnalyticsEvent event) {
        // 실시간 대시보드용 데이터 처리
        // Redis에 카운터 업데이트 등
    }
    
    private void forwardToExternalSystem(AnalyticsEvent event) {
        // BigQuery, Elasticsearch, S3 등으로 전송
    }
}
```

### 4. AnalyticsLogger 업데이트 (Kafka 통합)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsLogger {
    
    private final ObjectMapper objectMapper;
    private final KafkaAnalyticsProducer kafkaProducer;
    
    @Value("${analytics.kafka.enabled:false}")
    private boolean kafkaEnabled;
    
    public void logEvent(AnalyticsEvent event) {
        try {
            // 로그 파일에 기록
            String jsonLog = objectMapper.writeValueAsString(event);
            log.info("ANALYTICS_EVENT: {}", jsonLog);
            
            // Kafka 활성화 시 Kafka로도 전송
            if (kafkaEnabled) {
                kafkaProducer.sendEvent(event);
            }
            
        } catch (Exception e) {
            log.error("Failed to log analytics event", e);
        }
    }
}
```

---

## 📊 모니터링 및 분석

### 1. Grafana 대시보드 설정

Grafana를 활용하여 실시간 분석 데이터 시각화:

**대시보드 패널 예시**:
- API 호출 횟수 (시간별, 엔드포인트별)
- 사용자 활동 (로그인, 회원가입, 메시지 전송)
- 에러율
- 응답 시간 (p50, p95, p99)
- 채팅방 생성/삭제 추이
- 사용자 동시 접속자 수

### 2. 로그 분석 쿼리

**Loki/Elasticsearch 쿼리 예시**:

```
# 시간당 API 호출 횟수
{job="dorandoran"} |= "ANALYTICS_EVENT" | json | category="API" | unwrap duration_ms | sum by (endpoint)

# 사용자 등록 추이
{job="dorandoran"} |= "user_registration" | json | count_over_time[1h]

# 에러율 계산
rate({job="dorandoran"} |= "error_occurred" [5m]) * 100
```

### 3. 데이터 활용 방안

#### PM/기획자에게 제공할 데이터

1. **사용자 행동 분석**
   - 일별/주별/월별 활성 사용자 수 (DAU/WAU/MAU)
   - 사용자 유입 경로
   - 사용자 이탈률 (Churn Rate)
   - 세션 지속 시간

2. **기능 사용 통계**
   - 채팅방 생성/사용 빈도
   - 메시지 전송 패턴
   - 주요 기능 클릭률
   - A/B 테스트 결과

3. **성능 지표**
   - 페이지 로드 시간
   - API 응답 시간
   - 에러율 및 에러 유형

4. **비즈니스 메트릭**
   - 전환율 (Conversion Rate)
   - 리텐션 (Retention)
   - 코호트 분석 (Cohort Analysis)

### 4. 리포트 생성 자동화

**`batch/src/main/java/com/dorandoran/batch/job/AnalyticsReportJob.java`**:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsReportJob {
    
    private final AnalyticsRepository analyticsRepository;
    
    /**
     * 매일 오전 9시에 일일 리포트 생성
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void generateDailyReport() {
        log.info("Generating daily analytics report...");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // 통계 수집
        DailyReport report = DailyReport.builder()
            .date(yesterday)
            .totalUsers(analyticsRepository.countUniqueUsers(yesterday))
            .newUsers(analyticsRepository.countNewUsers(yesterday))
            .totalLogins(analyticsRepository.countLogins(yesterday))
            .totalMessages(analyticsRepository.countMessages(yesterday))
            .totalChatRooms(analyticsRepository.countChatRooms(yesterday))
            .errorRate(analyticsRepository.calculateErrorRate(yesterday))
            .avgResponseTime(analyticsRepository.calculateAvgResponseTime(yesterday))
            .build();
        
        // 리포트 저장
        saveReport(report);
        
        // 이메일 전송 (PM/기획자에게)
        sendReportEmail(report);
        
        log.info("Daily report generated successfully");
    }
}
```

---

## 📋 체크리스트

### 프론트엔드 담당자

- [ ] GA4 계정 및 측정 ID 설정
- [ ] `react-ga4` 패키지 설치
- [ ] Analytics 설정 파일 생성 (`analytics.ts`)
- [ ] App.tsx에 GA 초기화 코드 추가
- [ ] 주요 이벤트 추적 코드 구현
  - [ ] 로그인/로그아웃
  - [ ] 회원가입
  - [ ] 페이지 뷰
  - [ ] 버튼 클릭
  - [ ] 모달 열기/닫기
  - [ ] 채팅방 입장/퇴장
  - [ ] 메시지 전송
- [ ] 환경별 측정 ID 분리 (dev/staging/prod)
- [ ] 개발 환경에서 GA 테스트
- [ ] GA Realtime 리포트로 이벤트 확인

### 백엔드 담당자

- [ ] Analytics 의존성 추가 (Micrometer, Logback)
- [ ] AnalyticsLogger 공통 컴포넌트 구현
- [ ] AnalyticsEvent 모델 정의
- [ ] AOP를 활용한 자동 로깅 설정
- [ ] 서비스별 이벤트 로깅 구현
  - [ ] User Service: 회원가입, 로그인, 프로필 수정
  - [ ] Chat Service: 채팅방 생성, 메시지 전송
  - [ ] API 호출 메트릭
- [ ] Logback 설정 (analytics.log 분리)
- [ ] (선택) Kafka 설정 및 Producer/Consumer 구현
- [ ] (선택) Batch Job으로 일일 리포트 생성
- [ ] Grafana 대시보드 설정
- [ ] 로그 모니터링 환경 구축

### PM/데이터 분석가

- [ ] 수집할 이벤트 목록 정의
- [ ] KPI 지표 정의
- [ ] GA4 대시보드 설정
- [ ] 주간/월간 리포트 템플릿 작성
- [ ] 데이터 활용 방안 수립

---

## 🎓 학습 자료

### Google Analytics 4

- [GA4 공식 문서](https://support.google.com/analytics/answer/9304153)
- [GA4 이벤트 측정](https://developers.google.com/analytics/devguides/collection/ga4/events)
- [React-GA4 GitHub](https://github.com/codler/react-ga4)

### Kafka

- [Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Spring Kafka](https://docs.spring.io/spring-kafka/reference/html/)
- [Kafka 실전 가이드](https://www.confluent.io/blog/)

### Micrometer & Prometheus

- [Micrometer 공식 문서](https://micrometer.io/docs)
- [Prometheus 가이드](https://prometheus.io/docs/introduction/overview/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

## 🙋‍♂️ FAQ

### Q1: 개발 환경에서 GA를 테스트하려면?

**A**: GA4의 "DebugView"를 활용하세요.
1. Chrome 확장 프로그램 "Google Analytics Debugger" 설치
2. 개발 환경에서 애플리케이션 실행
3. GA4 → DebugView에서 실시간 이벤트 확인

### Q2: 프론트엔드와 백엔드 중 어디서 이벤트를 수집해야 하나요?

**A**: 양쪽 모두에서 수집하되, 목적에 따라 다릅니다.
- **프론트엔드**: UI 상호작용, 사용자 경험, 클릭 이벤트
- **백엔드**: 비즈니스 로직, 성능 메트릭, 보안 로그

### Q3: Kafka가 꼭 필요한가요?

**A**: 초기에는 불필요합니다.
- **파일 로그 → Loki/Elasticsearch**: 중소 규모에 적합
- **Kafka**: 대량 트래픽 (일 수백만 이벤트 이상)일 때 고려

### Q4: PM에게 어떤 형식으로 데이터를 제공하나요?

**A**: 
1. **GA4 대시보드 공유** (실시간 확인 가능)
2. **주간/월간 PDF/Excel 리포트**
3. **Grafana 대시보드 링크** (기술 지표)

### Q5: GDPR, 개인정보 보호는 어떻게 하나요?

**A**:
1. GA4 설정에서 IP 익명화 활성화
2. 사용자 동의 팝업 구현 (Cookie Consent)
3. 개인 식별 정보(PII) 수집 금지
4. 데이터 보관 기간 설정 (GA4: 최대 14개월)

---

## ✅ 다음 단계

1. **GA4 계정 생성 및 측정 ID 발급** (30분)
2. **프론트엔드 GA 설정** (2-3시간)
3. **백엔드 로깅 설정** (2-3시간)
4. **주요 이벤트 구현 및 테스트** (1-2일)
5. **Grafana 대시보드 설정** (1일)
6. **(선택) Kafka 도입** (2-3일)
7. **PM과 데이터 리뷰 및 피드백** (지속적)

---

**작성일**: 2025-10-20  
**작성자**: DoranDoran 개발팀  
**문서 버전**: 1.0

