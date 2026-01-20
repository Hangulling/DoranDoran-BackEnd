package com.dorandoran.store.service;

import com.dorandoran.store.dto.response.BookmarkCountByBotResponse;
import com.dorandoran.store.entity.Store;
import com.dorandoran.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * StorageService 테스트 - 보관함 누적 수 계산
 * Mock 데이터를 사용한 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StorageService - 보관함 누적 수 계산 테스트")
class StorageServiceCountTest {

  @Mock
  private StoreRepository storeRepository;

  @InjectMocks
  private StorageService storageService;

  private UUID testUserId;
  private UUID testChatroomId;

  @BeforeEach
  void setUp() {
    testUserId = UUID.fromString("99999999-9999-9999-9999-999999999999");
    testChatroomId = UUID.fromString("88888888-8888-8888-8888-888888888888");
  }

  // ========== 봇 타입별 보관 수 조회 테스트 ==========

  @Test
  @DisplayName("봇 타입별 보관 수 조회 - 정상 케이스")
  void countBookmarksByBotType_Success() {
    // given: 각 봇 타입별로 다른 개수의 북마크가 있음
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "friend"))
        .willReturn(15L);
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "honey"))
        .willReturn(8L);
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "coworker"))
        .willReturn(3L);
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "senior"))
        .willReturn(12L);

    // when: 봇 타입별 보관 수 조회
    BookmarkCountByBotResponse response = storageService.countBookmarksByBotType(testUserId);

    // then: 각 봇 타입별 개수가 정확히 반환됨
    assertThat(response).isNotNull();
    assertThat(response.getFriendCount()).isEqualTo(15L);
    assertThat(response.getHoneyCount()).isEqualTo(8L);
    assertThat(response.getCoworkerCount()).isEqualTo(3L);
    assertThat(response.getSeniorCount()).isEqualTo(12L);
    assertThat(response.getTotalCount()).isEqualTo(38L); // 15 + 8 + 3 + 12 = 38

    // Repository 호출 검증
    verify(storeRepository, times(1))
        .countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "friend");
    verify(storeRepository, times(1))
        .countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "honey");
    verify(storeRepository, times(1))
        .countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "coworker");
    verify(storeRepository, times(1))
        .countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "senior");
  }

  @Test
  @DisplayName("봇 타입별 보관 수 조회 - 모든 봇 타입이 0인 경우")
  void countBookmarksByBotType_AllZero() {
    // given: 모든 봇 타입의 북마크가 0개
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "friend"))
        .willReturn(0L);
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "honey"))
        .willReturn(0L);
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "coworker"))
        .willReturn(0L);
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "senior"))
        .willReturn(0L);

    // when: 봇 타입별 보관 수 조회
    BookmarkCountByBotResponse response = storageService.countBookmarksByBotType(testUserId);

    // then: 모든 개수가 0으로 반환됨
    assertThat(response).isNotNull();
    assertThat(response.getFriendCount()).isEqualTo(0L);
    assertThat(response.getHoneyCount()).isEqualTo(0L);
    assertThat(response.getCoworkerCount()).isEqualTo(0L);
    assertThat(response.getSeniorCount()).isEqualTo(0L);
    assertThat(response.getTotalCount()).isEqualTo(0L);
  }

  @Test
  @DisplayName("봇 타입별 보관 수 조회 - 특정 봇만 사용한 경우")
  void countBookmarksByBotType_OnlyFriend() {
    // given: friend 봇만 사용하고 나머지는 0개
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "friend"))
        .willReturn(25L);
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "honey"))
        .willReturn(0L);
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "coworker"))
        .willReturn(0L);
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "senior"))
        .willReturn(0L);

    // when: 봇 타입별 보관 수 조회
    BookmarkCountByBotResponse response = storageService.countBookmarksByBotType(testUserId);

    // then: friend만 25개, 나머지 0개
    assertThat(response).isNotNull();
    assertThat(response.getFriendCount()).isEqualTo(25L);
    assertThat(response.getHoneyCount()).isEqualTo(0L);
    assertThat(response.getCoworkerCount()).isEqualTo(0L);
    assertThat(response.getSeniorCount()).isEqualTo(0L);
    assertThat(response.getTotalCount()).isEqualTo(25L);
  }

  @Test
  @DisplayName("봇 타입별 보관 수 조회 - totalCount 계산 검증")
  void countBookmarksByBotType_TotalCountCalculation() {
    // given: 각 봇 타입에 다양한 개수
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "friend"))
        .willReturn(100L);
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "honey"))
        .willReturn(50L);
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "coworker"))
        .willReturn(30L);
    given(storeRepository.countByUserIdAndBotTypeAndIsDeletedFalse(testUserId, "senior"))
        .willReturn(20L);

    // when: 봇 타입별 보관 수 조회
    BookmarkCountByBotResponse response = storageService.countBookmarksByBotType(testUserId);

    // then: totalCount가 정확히 계산됨 (100 + 50 + 30 + 20 = 200)
    assertThat(response.getTotalCount()).isEqualTo(200L);
  }


  // ========== Mock 데이터 생성 헬퍼 메서드 ==========

  /**
   * Mock Store 리스트 생성
   *
   * @param chatroomId 채팅방 ID
   * @param botType 봇 타입
   * @param count 생성할 개수
   * @return Mock Store 리스트
   */
  private List<Store> createMockStores(UUID chatroomId, String botType, int count) {
    List<Store> stores = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Store store = Store.builder()
          .id(UUID.randomUUID())
          .userId(testUserId)
          .chatroomId(chatroomId)
          .messageId(UUID.randomUUID())
          .botType(botType)
          .content("Test content " + i)
          .correctedContent("Corrected content " + i)
          .isDeleted(false)
          .build();
      stores.add(store);
    }
    return stores;
  }
}