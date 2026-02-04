package com.dorandoran.user.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 친밀도 레벨 옵션 응답 DTO
 */
@Getter
@AllArgsConstructor
public class IntimacyLevelOptionResponse {

  // 친밀도 레벨
  private Integer level;

  // 레벨 설명
  private String description;
}