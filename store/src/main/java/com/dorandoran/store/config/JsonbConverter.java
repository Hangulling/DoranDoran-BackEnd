package com.dorandoran.store.config;

import com.dorandoran.store.dto.AiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL JSONB 컬럼 변환기
 * AiResponse DTO <-> JSONB 문자열
 */
@Converter
@Slf4j
public class JsonbConverter implements AttributeConverter<AiResponse, String> {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Entity -> DB (AiResponse -> JSONB 문자열)
   */
  @Override
  public String convertToDatabaseColumn(AiResponse attribute) {
    if (attribute == null) {
      return null;
    }

    try {
      String json = objectMapper.writeValueAsString(attribute);
      log.debug("AiResponse -> JSONB: {}", json);
      return json;
    } catch (JsonProcessingException e) {
      log.error("AiResponse 직렬화 실패", e);
      throw new IllegalArgumentException("AiResponse 직렬화 실패", e);
    }
  }

  /**
   * DB -> Entity (JSONB 문자열 -> AiResponse)
   */
  @Override
  public AiResponse convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isEmpty()) {
      return null;
    }

    try {
      AiResponse aiResponse = objectMapper.readValue(dbData, AiResponse.class);
      log.debug("JSONB -> AiResponse: {}", aiResponse);
      return aiResponse;
    } catch (JsonProcessingException e) {
      log.error("JSONB 역직렬화 실패: {}", dbData, e);
      throw new IllegalArgumentException("JSONB 역직렬화 실패", e);
    }
  }
}