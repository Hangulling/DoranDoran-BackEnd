package com.dorandoran.chat.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 프롬프트 파일 동기화 서비스
 * DB의 active 프롬프트를 파일 시스템에 동기화
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptFileSyncService {

    @Value("${prompt.file.external-path:/data/prompts}")
    private String externalPath;

    @Value("${prompt.file.sync-resources:true}")
    private boolean syncResources;

    /**
     * 프롬프트를 파일로 동기화
     * @param agentType 에이전트 타입 (예: INTIMACY_ANALYSIS)
     * @param concept 컨셉 (예: friend)
     * @param intimacyLevel 친밀도 레벨 (1 또는 3)
     * @param content 프롬프트 내용
     */
    @CacheEvict(value = "prompts", allEntries = true)
    public void syncPromptToFile(String agentType, String concept, Integer intimacyLevel, String content) {
        log.info("프롬프트 파일 동기화 시작: agentType={}, concept={}, intimacyLevel={}, content 길이={}자",
            agentType, concept, intimacyLevel, content != null ? content.length() : 0);

        // 파일 경로 생성
        String filePathPrefix = getFilePathPrefix(agentType);
        if (filePathPrefix == null) {
            log.error("알 수 없는 agentType: {}", agentType);
            throw new IllegalArgumentException("알 수 없는 agentType: " + agentType);
        }

        String fileName = String.format("%s_%d.txt", concept.toLowerCase(), intimacyLevel);
        String relativePath = String.format("%s/%s", filePathPrefix, fileName);

        boolean success = false;

        // 1. 외부 디렉토리에 저장 (운영용)
        try {
            Path externalDir = Paths.get(externalPath, filePathPrefix);
            Files.createDirectories(externalDir);
            Path externalFile = externalDir.resolve(fileName);
            Files.writeString(externalFile, content);
            log.info("외부 디렉토리에 파일 저장 성공: {}", externalFile);
            success = true;
        } catch (IOException e) {
            log.error("외부 디렉토리에 파일 저장 실패: {}", externalPath, e);
        }

        // 2. resources 디렉토리에 저장 (개발용, 선택적)
        if (syncResources) {
            try {
                // resources 디렉토리는 프로젝트 루트 기준
                Path resourcesDir = Paths.get("chat/src/main/resources/prompts", filePathPrefix);
                Files.createDirectories(resourcesDir);
                Path resourcesFile = resourcesDir.resolve(fileName);
                Files.writeString(resourcesFile, content);
                log.info("resources 디렉토리에 파일 저장 성공: {}", resourcesFile);
            } catch (IOException e) {
                log.warn("resources 디렉토리에 파일 저장 실패 (무시): {}", e.getMessage());
                // resources 디렉토리 저장 실패는 무시 (외부 디렉토리만 성공해도 OK)
            }
        }

        if (!success) {
            throw new RuntimeException("프롬프트 파일 동기화 실패: 외부 디렉토리 저장 실패");
        }

        log.info("프롬프트 파일 동기화 완료: agentType={}, concept={}, intimacyLevel={}", 
            agentType, concept, intimacyLevel);
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
