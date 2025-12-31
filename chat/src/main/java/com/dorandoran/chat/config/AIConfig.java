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
    
    // Agent별 설정
    private AgentConfig agents = new AgentConfig();
    
    @Getter
    @Setter
    public static class AgentConfig {
        private VocabularyConfig vocabulary = new VocabularyConfig();
        private IntimacyConfig intimacy = new IntimacyConfig();
    }
    
    @Getter
    @Setter
    public static class VocabularyConfig {
        private ExtractionConfig extraction = new ExtractionConfig();
        private ExplanationConfig explanation = new ExplanationConfig();
    }
    
    @Getter
    @Setter
    public static class ExtractionConfig {
        private Double temperature = 0.2;  // 정확한 추출을 위해 낮게 설정
        private Integer maxTokens = 300;
    }
    
    @Getter
    @Setter
    public static class ExplanationConfig {
        private Double temperature = 0.5;  // 자연스러운 설명을 위해 적당히 설정
        private Integer maxTokens = 200;
    }
    
    @Getter
    @Setter
    public static class IntimacyConfig {
        private AnalysisConfig analysis = new AnalysisConfig();
        private CorrectionConfig correction = new CorrectionConfig();
    }
    
    @Getter
    @Setter
    public static class AnalysisConfig {
        private Double temperature = 0.2;  // 정확한 분석을 위해 낮게 설정
        private Integer maxTokens = 400;
    }
    
    @Getter
    @Setter
    public static class CorrectionConfig {
        private Double temperature = 0.5;  // 자연스러운 교정을 위해 적당히 설정
        private Integer maxTokens = 300;
    }
}
