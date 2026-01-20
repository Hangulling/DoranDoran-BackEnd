package com.dorandoran.chat.service;

import com.dorandoran.chat.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 프롬프트 로더 서비스
 * DB 우선, 파일 fallback 방식으로 프롬프트를 로드
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptLoaderService {

    private final UserServiceClient userServiceClient;

    /**
     * 프롬프트 로드 (DB 우선, 파일 fallback)
     * @param agentType 에이전트 타입 (예: INTIMACY_ANALYSIS)
     * @param concept 컨셉 (예: friend)
     * @param intimacyLevel 친밀도 레벨 (1 또는 3)
     * @param env 환경 (prod, dev, test)
     * @return 프롬프트 내용, 없으면 null
     */
    @Cacheable(value = "prompts", key = "#agentType + ':' + #concept + ':' + #intimacyLevel + ':' + #env", unless = "#result == null")
    public String loadPrompt(String agentType, String concept, Integer intimacyLevel, String env) {
        // 1. DB에서 active 프롬프트 조회 시도
        String dbPrompt = userServiceClient.getActivePrompt(agentType, concept, intimacyLevel, env);
        if (dbPrompt != null && !dbPrompt.isEmpty()) {
            log.info("DB에서 프롬프트 로드 성공: agentType={}, concept={}, intimacyLevel={}, env={}, 길이={}자",
                agentType, concept, intimacyLevel, env, dbPrompt.length());
            return dbPrompt;
        }

        // 2. DB에 없으면 파일에서 로드 (fallback)
        log.debug("DB에 프롬프트 없음, 파일에서 로드 시도: agentType={}, concept={}, intimacyLevel={}",
            agentType, concept, intimacyLevel);
        return loadPromptFromFile(agentType, concept, intimacyLevel);
    }

    /**
     * 파일에서 프롬프트 로드
     * @param agentType 에이전트 타입 (예: INTIMACY_ANALYSIS)
     * @param concept 컨셉 (예: friend)
     * @param intimacyLevel 친밀도 레벨 (1 또는 3)
     * @return 프롬프트 내용, 없으면 null
     */
    private String loadPromptFromFile(String agentType, String concept, Integer intimacyLevel) {
        // AgentType enum의 filePathPrefix를 사용하여 파일 경로 생성
        String filePathPrefix = getFilePathPrefix(agentType);
        if (filePathPrefix == null) {
            log.warn("알 수 없는 agentType: {}", agentType);
            return null;
        }

        String filename = String.format("prompts/%s/%s_%d.txt",
            filePathPrefix, concept.toLowerCase(), intimacyLevel);

        try {
            ClassPathResource resource = new ClassPathResource(filename);
            if (!resource.exists()) {
                log.debug("프롬프트 파일 없음: {}", filename);
                return null;
            }

            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("파일에서 프롬프트 로드 성공: {}, 길이={}자", filename, content.length());
            return content;

        } catch (IOException e) {
            log.warn("프롬프트 파일 로드 실패: {}", filename, e);
            return null;
        }
    }

    /**
     * AgentType에 따른 파일 경로 prefix 반환
     */
    private String getFilePathPrefix(String agentType) {
        if (agentType == null) {
            return null;
        }
        
        switch (agentType.toUpperCase()) {
            case "INTIMACY_ANALYSIS":
                return "intimacy/analysis";
            case "INTIMACY_CORRECTION":
                return "intimacy/correction";
            case "VOCABULARY_EXTRACTION":
                return "vocabulary/extraction";
            case "VOCABULARY_EXPLANATION":
                return "vocabulary/explanation";
            case "CONVERSATION":
                return "conversation";
            case "GREETING":
                return "greeting";
            default:
                return null;
        }
    }
}
