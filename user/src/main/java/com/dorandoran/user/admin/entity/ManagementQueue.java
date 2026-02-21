package com.dorandoran.user.admin.entity;

import com.dorandoran.user.admin.enums.QueueStatus;
import com.dorandoran.user.admin.enums.QueueType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리 필요 내역 큐
 */
@Entity
@Table(name = "management_queue", schema = "archive_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ManagementQueue {

  @Id
  @GeneratedValue
  private UUID id;

  /**
   * 큐 타입
   * - CORRECTION: 교정 작업
   * - DELETION: 삭제 작업
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "queue_type", nullable = false, length = 50)
  private QueueType queueType;

  /**
   * 상태
   * - PENDING: 대기 중
   * - COMPLETED: 처리 완료
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private QueueStatus status;

  /**
   * 요청 데이터 (JSONB)
   * - messageId, chatroomId, items, memo, details 등
   * - String으로 저장 후 ObjectMapper로 파싱
   */
  @Column(name = "request_data", columnDefinition = "jsonb", nullable = false)
  private String requestData;

  /**
   * 처리 결과 데이터 (JSONB)
   * - processedBy, processedAt, action, note 등
   * - 처리 완료 시에만 저장
   */
  @Column(name = "result_data", columnDefinition = "jsonb")
  private String resultData;

  /**
   * 관리자 이메일
   * - 작업을 수행한 관리자
   * - 감사 로그용
   */
  @Column(name = "admin_name", length = 100)
  private String adminName;

  /**
   * 관리자 접속 IP
   * - IPv4 또는 IPv6
   * - 감사 로그용
   */
  @Column(name = "admin_ip", length = 45)
  private String adminIp;

  // 생성 시각
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  // 수정 시간
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // 완료 시간
  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  // 에러 메세지
  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  /**
   * Entity 저장 전 자동 호출
   * - createdAt 자동 설정
   */
  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  /**
   * Entity 수정 전 자동 호출
   * - updatedAt 자동 설정
   */
  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * 처리 완료 처리
   *
   * @param processedBy 처리자 이메일
   * @param note 처리 노트
   */
  public void complete(String processedBy, String note) {
    this.status = QueueStatus.COMPLETED;
    this.completedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * 메모 수정
   *
   * @param newMemo 새로운 메모 내용
   */
  public void updateMemo(String newMemo) {
    // request_data의 memo 필드를 수정
    // Service에서 JSON 파싱 후 업데이트
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * result_data 설정
   *
   * @param resultData 처리 결과 JSON 문자열
   */
  public void setResultData(String resultData) {
    this.resultData = resultData;
  }

  /**
   * request_data 업데이트
   *
   * @param requestData 수정된 요청 데이터 JSON 문자열
   */
  public void updateRequestData(String requestData) {
    this.requestData = requestData;
    this.updatedAt = LocalDateTime.now();
  }
}