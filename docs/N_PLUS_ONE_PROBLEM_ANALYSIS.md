# N+1 문제 분석 및 해결 방안

## 📋 목차
1. [N+1 문제의 정의](#1-n1-문제의-정의)
2. [현재 문제 구체화](#2-현재-문제-구체화)
3. [해결 방안](#3-해결-방안)
4. [성능 개선 효과](#4-성능-개선-효과)
5. [추가 최적화 방안](#5-추가-최적화-방안)

---

## 1. N+1 문제의 정의

### 1.1 개념

**N+1 문제**는 데이터베이스나 외부 API를 조회할 때 발생하는 성능 문제입니다.

- **1번의 쿼리**: 메인 엔티티 리스트를 조회 (예: Store 리스트)
- **N번의 쿼리**: 각 엔티티에 대해 연관된 데이터를 개별적으로 조회 (예: 각 Store의 chatroomName)

**총 쿼리 수 = 1 + N**

### 1.2 발생 시나리오

```
1. Store 리스트 조회 (1번 쿼리)
   SELECT * FROM stores WHERE user_id = ? AND is_deleted = false

2. 각 Store에 대해 chatroomName 조회 (N번 쿼리/API 호출)
   - Store 1 → chatroomName 조회 (1번)
   - Store 2 → chatroomName 조회 (2번)
   - Store 3 → chatroomName 조회 (3번)
   ...
   - Store N → chatroomName 조회 (N번)
```

### 1.3 문제점

1. **네트워크 오버헤드**: 각 호출마다 네트워크 왕복 시간 발생
2. **응답 시간 증가**: 순차적 호출로 인한 지연 시간 누적
3. **서버 부하**: 불필요한 반복 호출로 인한 리소스 낭비
4. **확장성 저하**: 데이터가 많아질수록 성능 저하가 선형적으로 증가

---

## 2. 현재 문제 구체화

### 2.1 문제가 발생하는 코드 위치

**파일**: `store/src/main/java/com/dorandoran/store/service/StorageService.java`

#### 2.1.1 문제가 있는 메서드들

1. **`getBookmarksWithCursor`** (라인 229-237)
2. **`getBookmarks(UUID userId, Pageable pageable)`** (라인 115-131)
3. **`getBookmarks(UUID userId)`** (라인 89-110)
4. **`getBookmarksByBotType`** (라인 210-224)

### 2.2 문제 코드 분석

#### 예시 1: `getBookmarksWithCursor` 메서드

```java
@Transactional(readOnly = true)
public Page<StorageListResponse> getBookmarksWithCursor(UUID userId, UUID lastId, Pageable pageable) {
  log.info("Cursor 기반 보관함 조회: userId={}, lastId={}, size={}",
      userId, lastId, pageable.getPageSize());

  // ✅ 1번의 쿼리: Store 리스트 조회
  Page<Store> stores = storeRepository.findByUserIdWithCursor(userId, lastId, pageable);

  // ❌ N번의 Feign 호출: 각 Store마다 enrichWithChatroomName 호출
  return stores.map(store -> enrichWithChatroomName(store));
}
```

#### 예시 2: `enrichWithChatroomName` 메서드 (라인 302-344)

```java
private StorageListResponse enrichWithChatroomName(Store store) {
  StorageListResponse response = StorageListResponse.from(store);

  try {
    // ❌ 각 Store마다 Feign API 호출 발생
    ChatRoomDto chatRoom = chatServiceClient.getChatRoom(
        store.getChatroomId(),
        store.getUserId()
    );
    // ... 예외 처리 ...
  }
  return response;
}
```

### 2.3 실제 발생 시나리오

#### 시나리오: 사용자가 보관함 목록을 조회하는 경우

**가정**:
- 조회된 Store 개수: 20개
- 서로 다른 chatroomId 개수: 15개
- 같은 chatroomId를 가진 Store: 5개

**현재 동작**:
```
1. Store 리스트 조회: 1번 (DB 쿼리)
2. 각 Store에 대해 chatroomName 조회: 20번 (Feign API 호출)
   - Store 1 (chatroomId: A) → Feign 호출 1
   - Store 2 (chatroomId: B) → Feign 호출 2
   - Store 3 (chatroomId: A) → Feign 호출 3 (중복!)
   - Store 4 (chatroomId: C) → Feign 호출 4
   - Store 5 (chatroomId: A) → Feign 호출 5 (중복!)
   ...
   - Store 20 (chatroomId: O) → Feign 호출 20

총 호출: 1 + 20 = 21번
```

**문제점**:
- 같은 `chatroomId`를 가진 Store들이 여러 개 있어도 각각 호출
- `chatroomId: A`에 대한 정보를 3번 중복 호출
- 불필요한 네트워크 트래픽과 응답 시간 증가

### 2.4 로그 분석

실제 서버 로그에서 확인된 증상:

```
2025-11-07T09:43:00.730Z DEBUG ... Feign HMAC 헤더 추가: userId=..., ts=...
2025-11-07T09:43:00.733Z  WARN ... 채팅방 접근 권한 없음: chatroomId=..., userId=...
2025-11-07T09:43:00.736Z DEBUG ... Feign HMAC 헤더 추가: userId=..., ts=...
2025-11-07T09:43:00.738Z  WARN ... 채팅방 접근 권한 없음: chatroomId=..., userId=...
...
```

**관찰 사항**:
- "Feign HMAC 헤더 추가" 로그가 매우 빈번하게 반복
- 같은 `chatroomId`에 대한 "채팅방 접근 권한 없음" 경고가 반복
- 짧은 시간 내에 수십 번의 Feign 호출 발생

### 2.5 성능 영향

**측정 가능한 지표**:
- **네트워크 왕복 시간**: 각 Feign 호출당 평균 10-50ms
- **총 응답 시간**: 20개 Store × 30ms = 600ms (순차 호출 시)
- **서버 부하**: 불필요한 HTTP 요청으로 인한 리소스 소비

**실제 영향**:
- 사용자 경험 저하: 페이지 로딩 시간 증가
- 서버 리소스 낭비: CPU, 네트워크 대역폭
- Chat Service 부하 증가: 불필요한 요청 처리

---

## 3. 해결 방안

### 3.1 핵심 전략

**배치 조회(Batch Fetch) 패턴** 적용:
1. 모든 Store의 `chatroomId`를 수집
2. 중복 제거 (Set 사용)
3. 각 고유한 `chatroomId`에 대해 한 번만 조회
4. 결과를 Map으로 캐싱하여 재사용

### 3.2 구현 방법

#### 3.2.1 헬퍼 메서드 추가

`StorageService` 클래스에 다음 메서드를 추가합니다:

```java
/**
 * Store 리스트에서 chatroomId를 수집하여 한 번에 조회
 * N+1 문제 해결을 위한 배치 조회
 * 
 * @param stores Store 엔티티 리스트
 * @param userId 사용자 ID (권한 체크용)
 * @return chatroomId를 키로, chatroomName을 값으로 하는 Map
 */
private Map<UUID, String> buildChatroomNameMap(List<Store> stores, UUID userId) {
  // 1. 모든 chatroomId를 Set으로 수집 (중복 제거)
  Set<UUID> chatroomIds = stores.stream()
      .map(Store::getChatroomId)
      .collect(Collectors.toSet());
  
  log.debug("채팅방 이름 배치 조회 시작: chatroomId 개수={}, Store 개수={}", 
      chatroomIds.size(), stores.size());
  
  // 2. 각 chatroomId에 대해 한 번만 Feign 호출
  Map<UUID, String> chatroomNameMap = new HashMap<>();
  
  for (UUID chatroomId : chatroomIds) {
    try {
      ChatRoomDto chatRoom = chatServiceClient.getChatRoom(chatroomId, userId);
      
      if (chatRoom != null && chatRoom.getName() != null) {
        chatroomNameMap.put(chatroomId, chatRoom.getName());
        log.debug("채팅방 이름 조회 성공: chatroomId={}, name={}", 
            chatroomId, chatRoom.getName());
      } else {
        chatroomNameMap.put(chatroomId, "Unknown");
        log.warn("채팅방 정보가 null: chatroomId={}", chatroomId);
      }
      
    } catch (FeignException.NotFound e) {
      chatroomNameMap.put(chatroomId, "Deleted Room");
      log.warn("채팅방을 찾을 수 없음: chatroomId={}", chatroomId);
      
    } catch (FeignException.Forbidden e) {
      chatroomNameMap.put(chatroomId, "Forbidden");
      log.warn("채팅방 접근 권한 없음: chatroomId={}, userId={}", chatroomId, userId);
      
    } catch (FeignException.ServiceUnavailable e) {
      chatroomNameMap.put(chatroomId, "Unavailable");
      log.warn("Chat Service 일시적 장애: chatroomId={}", chatroomId);
      
    } catch (FeignException e) {
      chatroomNameMap.put(chatroomId, "Unknown");
      log.warn("Feign 통신 오류: chatroomId={}, status={}, message={}", 
          chatroomId, e.status(), e.getMessage());
      
    } catch (Exception e) {
      chatroomNameMap.put(chatroomId, "Unknown");
      log.error("채팅방 이름 조회 중 예상치 못한 오류: chatroomId={}", chatroomId, e);
    }
  }
  
  log.debug("채팅방 이름 배치 조회 완료: 조회된 개수={}", chatroomNameMap.size());
  return chatroomNameMap;
}
```

#### 3.2.2 메서드 수정

##### 1) `getBookmarksWithCursor` 수정

```java
@Transactional(readOnly = true)
public Page<StorageListResponse> getBookmarksWithCursor(UUID userId, UUID lastId, Pageable pageable) {
  log.info("Cursor 기반 보관함 조회: userId={}, lastId={}, size={}",
      userId, lastId, pageable.getPageSize());

  // ✅ 1번의 쿼리: Store 리스트 조회
  Page<Store> stores = storeRepository.findByUserIdWithCursor(userId, lastId, pageable);

  // ✅ N+1 문제 해결: chatroomId별로 그룹화하여 한 번만 조회
  Map<UUID, String> chatroomNameMap = buildChatroomNameMap(stores.getContent(), userId);

  // ✅ Map에서 조회하여 재사용
  return stores.map(store -> {
    StorageListResponse response = StorageListResponse.from(store);
    String chatroomName = chatroomNameMap.getOrDefault(store.getChatroomId(), "Unknown");
    response.setChatroomNameFromClient(chatroomName);
    return response;
  });
}
```

##### 2) `getBookmarks(UUID userId, Pageable pageable)` 수정

```java
@Transactional(readOnly = true)
public Page<StorageListResponse> getBookmarks(UUID userId, Pageable pageable) {
  log.info("보관함 조회 (페이징): userId={}, page={}, size={}",
      userId, pageable.getPageNumber(), pageable.getPageSize());

  // ✅ 1번의 쿼리
  Page<Store> stores = storeRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);

  // ✅ N+1 문제 해결
  Map<UUID, String> chatroomNameMap = buildChatroomNameMap(stores.getContent(), userId);

  return stores.map(store -> {
    StorageListResponse response = StorageListResponse.from(store);
    String chatroomName = chatroomNameMap.getOrDefault(store.getChatroomId(), "Unknown");
    response.setChatroomNameFromClient(chatroomName);
    return response;
  });
}
```

##### 3) `getBookmarks(UUID userId)` 수정

```java
@Transactional(readOnly = true)
public List<StorageListResponse> getBookmarks(UUID userId) {
  log.info("보관함 전체 조회: userId={}", userId);

  // ✅ 1번의 쿼리
  List<Store> stores = storeRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

  if (stores.isEmpty()) {
    log.info("보관함이 비어있음: userId={}", userId);
    return Collections.emptyList();
  }

  // ✅ N+1 문제 해결
  Map<UUID, String> chatroomNameMap = buildChatroomNameMap(stores, userId);

  return stores.stream()
      .map(store -> {
        StorageListResponse response = StorageListResponse.from(store);
        String chatroomName = chatroomNameMap.getOrDefault(store.getChatroomId(), "Unknown");
        response.setChatroomNameFromClient(chatroomName);
        return response;
      })
      .collect(Collectors.toList());
}
```

##### 4) `getBookmarksByBotType` 수정

```java
@Transactional(readOnly = true)
public List<StorageListResponse> getBookmarksByBotType(UUID userId, String botType) {
  log.info("챗봇 타입별 보관함 조회: userId={}, botType={}", userId, botType);

  // ✅ 1번의 쿼리
  List<Store> stores = storeRepository
      .findByUserIdAndBotTypeAndIsDeletedFalseOrderByCreatedAtDesc(userId, botType);

  if (stores.isEmpty()) {
    log.info("해당 챗봇 타입의 보관함이 비어있음: botType={}", botType);
    return Collections.emptyList();
  }

  // ✅ N+1 문제 해결
  Map<UUID, String> chatroomNameMap = buildChatroomNameMap(stores, userId);

  return stores.stream()
      .map(store -> {
        StorageListResponse response = StorageListResponse.from(store);
        String chatroomName = chatroomNameMap.getOrDefault(store.getChatroomId(), "Unknown");
        response.setChatroomNameFromClient(chatroomName);
        return response;
      })
      .collect(Collectors.toList());
}
```

#### 3.2.3 필요한 Import 추가

```java
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections; // getBookmarks에서 빈 리스트 반환 시 사용
```

### 3.3 수정 전후 비교

#### 수정 전 (N+1 문제 발생)

```java
// ❌ 문제 코드
Page<Store> stores = storeRepository.findByUserIdWithCursor(userId, lastId, pageable);
return stores.map(store -> enrichWithChatroomName(store)); // 각 Store마다 Feign 호출
```

**호출 횟수**: 1 (DB) + N (Feign) = 1 + 20 = 21번

#### 수정 후 (N+1 문제 해결)

```java
// ✅ 개선된 코드
Page<Store> stores = storeRepository.findByUserIdWithCursor(userId, lastId, pageable);
Map<UUID, String> chatroomNameMap = buildChatroomNameMap(stores.getContent(), userId);
return stores.map(store -> {
  StorageListResponse response = StorageListResponse.from(store);
  response.setChatroomNameFromClient(chatroomNameMap.getOrDefault(store.getChatroomId(), "Unknown"));
  return response;
});
```

**호출 횟수**: 1 (DB) + M (고유 chatroomId 개수, Feign) = 1 + 15 = 16번

---

## 4. 성능 개선 효과

### 4.1 정량적 개선

#### 시나리오: 20개 Store, 15개 고유 chatroomId

| 항목 | 수정 전 | 수정 후 | 개선율 |
|------|---------|---------|--------|
| DB 쿼리 | 1번 | 1번 | - |
| Feign 호출 | 20번 | 15번 | **25% 감소** |
| 총 호출 | 21번 | 16번 | **23.8% 감소** |
| 예상 응답 시간* | 600ms | 450ms | **25% 개선** |

*각 Feign 호출당 30ms 가정

#### 시나리오: 100개 Store, 10개 고유 chatroomId (같은 채팅방에 많은 Store)

| 항목 | 수정 전 | 수정 후 | 개선율 |
|------|---------|---------|--------|
| DB 쿼리 | 1번 | 1번 | - |
| Feign 호출 | 100번 | 10번 | **90% 감소** |
| 총 호출 | 101번 | 11번 | **89.1% 감소** |
| 예상 응답 시간* | 3000ms | 300ms | **90% 개선** |

*각 Feign 호출당 30ms 가정

### 4.2 정성적 개선

1. **네트워크 트래픽 감소**: 불필요한 HTTP 요청 제거
2. **서버 부하 감소**: Chat Service의 불필요한 요청 처리 감소
3. **응답 시간 단축**: 사용자 경험 개선
4. **확장성 향상**: 데이터가 많아져도 성능 저하가 완화됨

### 4.3 개선 효과가 큰 경우

다음과 같은 경우에 특히 효과적입니다:

- ✅ 같은 `chatroomId`를 가진 Store가 많은 경우
- ✅ 페이징 크기가 큰 경우 (예: 50개, 100개)
- ✅ 사용자가 여러 채팅방에서 보관한 항목이 많은 경우

---

## 5. 추가 최적화 방안

### 5.1 배치 API 활용 (권장)

Chat Service에 여러 `chatroomId`를 한 번에 조회하는 배치 API가 있다면 활용:

```java
// ChatServiceClient에 배치 조회 메서드 추가
@PostMapping("/api/chat/chatrooms/batch")
List<ChatRoomDto> getChatRoomsBatch(
    @RequestBody List<UUID> chatroomIds,
    @RequestParam("userId") UUID userId
);

// buildChatroomNameMap 메서드 수정
private Map<UUID, String> buildChatroomNameMap(List<Store> stores, UUID userId) {
  Set<UUID> chatroomIds = stores.stream()
      .map(Store::getChatroomId)
      .collect(Collectors.toSet());
  
  // ✅ 한 번의 API 호출로 모든 채팅방 정보 조회
  List<ChatRoomDto> chatRooms = chatServiceClient.getChatRoomsBatch(
      new ArrayList<>(chatroomIds), 
      userId
  );
  
  return chatRooms.stream()
      .collect(Collectors.toMap(
          ChatRoomDto::getId,
          chatRoom -> chatRoom.getName() != null ? chatRoom.getName() : "Unknown"
      ));
}
```

**효과**: N번 호출 → 1번 호출로 감소

### 5.2 캐싱 적용

Redis 등을 활용하여 `chatroomName`을 캐싱:

```java
@Cacheable(value = "chatroomNames", key = "#chatroomId")
private String getChatroomName(UUID chatroomId, UUID userId) {
  // Feign 호출
}
```

**효과**: 반복 조회 시 네트워크 호출 제거

### 5.3 비동기 처리

`CompletableFuture`를 활용하여 여러 Feign 호출을 병렬 처리:

```java
private Map<UUID, String> buildChatroomNameMap(List<Store> stores, UUID userId) {
  Set<UUID> chatroomIds = stores.stream()
      .map(Store::getChatroomId)
      .collect(Collectors.toSet());
  
  // ✅ 병렬 처리
  List<CompletableFuture<Map.Entry<UUID, String>>> futures = chatroomIds.stream()
      .map(chatroomId -> CompletableFuture.supplyAsync(() -> {
        try {
          ChatRoomDto chatRoom = chatServiceClient.getChatRoom(chatroomId, userId);
          String name = (chatRoom != null && chatRoom.getName() != null) 
              ? chatRoom.getName() 
              : "Unknown";
          return Map.entry(chatroomId, name);
        } catch (Exception e) {
          return Map.entry(chatroomId, "Unknown");
        }
      }))
      .collect(Collectors.toList());
  
  return futures.stream()
      .map(CompletableFuture::join)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}
```

**효과**: 순차 호출 대비 응답 시간 단축 (병렬 처리)

### 5.4 Circuit Breaker 활용

Resilience4j의 Circuit Breaker를 활용하여 장애 전파 방지:

```java
@CircuitBreaker(name = "chatService", fallbackMethod = "getChatroomNameFallback")
private String getChatroomName(UUID chatroomId, UUID userId) {
  // Feign 호출
}

private String getChatroomNameFallback(UUID chatroomId, UUID userId, Exception e) {
  return "Unavailable";
}
```

**효과**: Chat Service 장애 시 전체 서비스 영향 최소화

---

## 6. 검증 방법

### 6.1 로그 확인

수정 후 로그에서 다음을 확인:

1. **Feign 호출 횟수 감소**: "Feign HMAC 헤더 추가" 로그가 줄어들어야 함
2. **배치 조회 로그**: "채팅방 이름 배치 조회 시작/완료" 로그 확인
3. **중복 호출 제거**: 같은 `chatroomId`에 대한 반복 호출이 없어야 함

### 6.2 성능 측정

- **응답 시간**: API 응답 시간 측정 (Before/After)
- **네트워크 트래픽**: Feign 호출 횟수 모니터링
- **서버 리소스**: CPU, 메모리 사용량 비교

### 6.3 테스트 케이스

```java
@Test
void testGetBookmarksWithCursor_shouldNotCallFeignMultipleTimesForSameChatroom() {
  // Given: 같은 chatroomId를 가진 Store 10개
  UUID userId = UUID.randomUUID();
  UUID chatroomId = UUID.randomUUID();
  
  // When: 조회 실행
  Page<StorageListResponse> result = storageService.getBookmarksWithCursor(
      userId, null, PageRequest.of(0, 20)
  );
  
  // Then: Feign 호출은 고유 chatroomId 개수만큼만 발생해야 함
  verify(chatServiceClient, times(1)).getChatRoom(eq(chatroomId), eq(userId));
}
```

---

## 7. 주의사항

### 7.1 예외 처리

- Feign 호출 실패 시에도 다른 Store의 처리는 계속되어야 함
- 일부 실패해도 전체 응답은 반환되어야 함

### 7.2 메모리 사용

- Store 개수가 매우 많은 경우 (수천 개), `buildChatroomNameMap`에서 메모리 사용량 고려
- 필요 시 스트림 처리나 청크 단위 처리 고려

### 7.3 기존 메서드 유지

- `enrichWithChatroomName` 메서드는 단일 Store 처리 시 유용하므로 유지
- `getBookmarksByChatroom` 같은 메서드는 이미 최적화되어 있으므로 수정 불필요

---

## 8. 결론

N+1 문제는 **배치 조회 패턴**을 통해 효과적으로 해결할 수 있습니다. 

**핵심 원칙**:
1. 중복 제거: Set을 사용하여 고유한 `chatroomId`만 수집
2. 배치 조회: 각 고유 `chatroomId`에 대해 한 번만 호출
3. 결과 재사용: Map으로 캐싱하여 여러 Store에서 재사용

이를 통해 **25-90%의 성능 개선**을 달성할 수 있으며, 특히 같은 `chatroomId`를 가진 Store가 많을수록 효과가 큽니다.

---

**작성일**: 2025-11-07  
**작성자**: AI Assistant  
**관련 이슈**: Store Service N+1 문제 해결

