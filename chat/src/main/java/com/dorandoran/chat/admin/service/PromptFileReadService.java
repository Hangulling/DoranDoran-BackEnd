package com.dorandoran.chat.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 프롬프트 파일 읽기 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptFileReadService {

    @Value("${prompt.file.external-path:/data/prompts}")
    private String externalPath;

    @Value("${prompt.file.sync-resources:true}")
    private boolean syncResources;

    public String readPromptContent(String agentType, String concept, Integer intimacyLevel) {
        String filePathPrefix = getFilePathPrefix(agentType);
        if (filePathPrefix == null) {
            throw new IllegalArgumentException("알 수 없는 agentType: " + agentType);
        }

        String fileName = String.format("%s_%d.txt", concept.toLowerCase(), intimacyLevel);

        Path externalFile = Paths.get(externalPath, filePathPrefix, fileName);
        if (Files.exists(externalFile)) {
            try {
                return Files.readString(externalFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("외부 경로 파일 읽기 실패: {}", externalFile, e);
            }
        }

        if (syncResources) {
            String resourcePath = String.format("prompts/%s/%s", filePathPrefix, fileName);
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (resource.exists()) {
                try {
                    return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    log.error("리소스 파일 읽기 실패: {}", resourcePath, e);
                }
            }
        }

        return null;
    }

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
