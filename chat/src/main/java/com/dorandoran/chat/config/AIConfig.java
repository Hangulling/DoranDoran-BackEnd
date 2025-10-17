package com.dorandoran.chat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.openai")
@Getter
@Setter
public class AIConfig {

	private String apiKey;
	private String baseUrl;
	private String model;

    // 제한/정책
    private Integer maxPromptChars = 8000; // 시스템 프롬프트 최대 길이
    private Integer maxOutputTokens = 800; // 출력 토큰 상한
    private Double pricePer1kInput = 0.0;  // 비용 로깅용 (USD)
    private Double pricePer1kOutput = 0.0; // 비용 로깅용 (USD)
}
