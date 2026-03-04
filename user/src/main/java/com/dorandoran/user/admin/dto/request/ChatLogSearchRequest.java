package com.dorandoran.user.admin.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 채팅 로그 검색 요청 DTO
 */
@Getter
@Setter
public class ChatLogSearchRequest {

  // 검색 시작일 (필수)
  @NotNull(message = "시작일은 필수입니다")
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;

  // 검색 종료일
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  // 채팅방 ID - 특정 채팅방을 검색하는 기능이 없어서 삭제
  //  private UUID chatroomId;

  // 채팅봇 컨셉
  private String concept;

  // 친밀도 레벨
  private Integer intimacyLevel;

  // 데이터 소스 (archive 또는 chat, 기본값 archive)
  private String dataSource = "archive";

  // 페이지 번호 (0부터 시작)
  @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
  private int page = 0;

  // 페이지 크기
  @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
  private int size = 20;
}
