package com.dorandoran.user.admin.service;

import com.dorandoran.user.admin.client.ChatServiceClient;
import com.dorandoran.user.admin.dto.PromptTestRequest;
import com.dorandoran.user.admin.dto.PromptTestResponse;
import com.dorandoran.user.admin.entity.PromptActive;
import com.dorandoran.user.admin.entity.PromptActiveId;
import com.dorandoran.user.admin.entity.PromptVersion;
import com.dorandoran.user.admin.enums.AgentType;
import com.dorandoran.user.admin.enums.Concept;
import com.dorandoran.user.admin.repository.PromptActiveRepository;
import com.dorandoran.user.admin.repository.PromptVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 프롬프트 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptService {

    private final PromptVersionRepository promptVersionRepository;
    private final PromptActiveRepository promptActiveRepository;
    private final ChatServiceClient chatServiceClient;

    /**
     * Active 프롬프트 조회
     */
    @Transactional(readOnly = true)
    public Optional<PromptVersion> getActivePrompt(AgentType agentType, Concept concept, Integer intimacyLevel, String env) {
        validatePromptKey(agentType, concept, intimacyLevel);
        if (env == null || env.isEmpty()) {
            env = "prod";
        }
        
        Optional<PromptActive> activeOpt = promptActiveRepository.findByEnvAndAgentTypeAndConceptAndIntimacyLevel(
            env, agentType, concept, intimacyLevel
        );
        
        if (activeOpt.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(activeOpt.get().getPromptVersion());
    }

    /**
     * 프롬프트 테스트 실행
     */
    @Transactional(readOnly = true)
    public PromptTestResponse testPrompt(AgentType agentType, Concept concept, Integer intimacyLevel, String inputText, Long promptVersionId) {
        validatePromptKey(agentType, concept, intimacyLevel);
        if (inputText == null || inputText.isBlank()) {
            throw new IllegalArgumentException("inputText is required");
        }
        log.info("=== [PromptService] 프롬프트 테스트 시작 ===");
        log.info("  - agentType: {}", agentType);
        log.info("  - concept: {}", concept);
        log.info("  - intimacyLevel: {}", intimacyLevel);
        log.info("  - inputText 길이: {}자", inputText != null ? inputText.length() : 0);
        log.info("  - promptVersionId: {}", promptVersionId);
        log.info("  - ⚠️ 주의: Chat Service는 DB 우선, 파일 fallback으로 프롬프트를 읽습니다.");
        
        if (promptVersionId != null) {
            PromptVersion version = promptVersionRepository.findById(promptVersionId)
                .orElseThrow(() -> new IllegalArgumentException("PromptVersion not found: " + promptVersionId));
            ensureVersionMatches(version, agentType, concept, intimacyLevel);
        }

        // Chat Service에 직접 요청하여 테스트
        PromptTestRequest request = new PromptTestRequest(
            agentType.name(),
            concept.getValue(),
            intimacyLevel,
            inputText,
            promptVersionId
        );
        
        log.info("=== [PromptService] ChatServiceClient.testAgent() 호출 시작 ===");
        long startTime = System.currentTimeMillis();
        
        PromptTestResponse response = chatServiceClient.testAgent(request);
        
        long endTime = System.currentTimeMillis();
        log.info("=== [PromptService] ChatServiceClient.testAgent() 완료 ===");
        log.info("  - Chat Service 호출 소요 시간: {}ms", endTime - startTime);
        log.info("  - 응답 latencyMs: {}ms", response.getLatencyMs());
        log.info("  - 응답 outputText 길이: {}자", response.getOutputText() != null ? response.getOutputText().length() : 0);
        
        return response;
    }

    /**
     * 새 버전 생성
     */
    @Transactional
    public PromptVersion createVersion(AgentType agentType, Concept concept, Integer intimacyLevel, String content, String memo, UUID adminId) {
        validatePromptKey(agentType, concept, intimacyLevel);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
        if (adminId == null) {
            throw new IllegalArgumentException("adminId is required");
        }
        log.info("=== [PromptService] 프롬프트 버전 생성 시작 ===");
        log.info("  - agentType: {}", agentType);
        log.info("  - concept: {}", concept);
        log.info("  - intimacyLevel: {}", intimacyLevel);
        log.info("  - content 길이: {}자", content != null ? content.length() : 0);
        log.info("  - memo: {}", memo);
        log.info("  - adminId: {}", adminId);
        
        // 최신 버전 조회
        log.info("=== [PromptService] 최신 버전 조회 중 ===");
        Optional<PromptVersion> latestOpt = promptVersionRepository
            .findTopByAgentTypeAndConceptAndIntimacyLevelOrderByCreatedAtDesc(agentType, concept, intimacyLevel);
        
        // 버전 번호 생성 (v0.1, v0.2, ...)
        String version;
        if (latestOpt.isEmpty()) {
            version = "v0.1";
            log.info("  - 최신 버전 없음, 초기 버전 생성: {}", version);
        } else {
            String latestVersion = latestOpt.get().getVersion();
            log.info("  - 최신 버전 발견: {}", latestVersion);
            // v0.1 -> v0.2, v0.2 -> v0.3 등
            String versionNumber = latestVersion.substring(1); // "0.1"
            String[] parts = versionNumber.split("\\.");
            int minor = Integer.parseInt(parts[1]) + 1;
            version = "v" + parts[0] + "." + minor;
            log.info("  - 새 버전 번호 생성: {}", version);
        }
        
        // 파일 경로 생성
        String filePath = agentType.buildFilePath(concept.getValue(), intimacyLevel);
        log.info("  - 파일 경로: {}", filePath);
        
        PromptVersion promptVersion = PromptVersion.builder()
            .agentType(agentType)
            .concept(concept)
            .intimacyLevel(intimacyLevel)
            .version(version)
            .content(content)
            .filePath(filePath)
            .memo(memo)
            .createdBy(adminId)
            .build();
        
        log.info("=== [PromptService] DB에 버전 저장 중 ===");
        PromptVersion saved = promptVersionRepository.save(promptVersion);
        log.info("=== [PromptService] 프롬프트 버전 생성 완료 ===");
        log.info("  - 저장된 버전 ID: {}", saved.getId());
        log.info("  - 저장된 버전 번호: {}", saved.getVersion());
        log.info("  - 저장된 파일 경로: {}", saved.getFilePath());
        log.info("  - DB 기준으로 저장되었으며, 활성화 시 파일 동기화를 시도합니다.");
        
        return saved;
    }

    /**
     * Active 전환
     */
    @Transactional
    public PromptActive activatePrompt(String env, AgentType agentType, Concept concept, Integer intimacyLevel, Long versionId, UUID adminId) {
        validatePromptKey(agentType, concept, intimacyLevel);
        if (versionId == null) {
            throw new IllegalArgumentException("versionId is required");
        }
        if (adminId == null) {
            throw new IllegalArgumentException("adminId is required");
        }
        log.info("=== [PromptService] 프롬프트 활성화 시작 ===");
        log.info("  - env: {}", env);
        log.info("  - agentType: {}", agentType);
        log.info("  - concept: {}", concept);
        log.info("  - intimacyLevel: {}", intimacyLevel);
        log.info("  - versionId: {}", versionId);
        log.info("  - adminId: {}", adminId);
        
        if (env == null || env.isEmpty()) {
            env = "prod";
            log.info("  - env가 비어있어 'prod'로 설정");
        }
        
        // 버전 조회
        log.info("=== [PromptService] 활성화할 버전 조회 중 ===");
        PromptVersion version = promptVersionRepository.findById(versionId)
            .orElseThrow(() -> {
                log.error("  - 버전을 찾을 수 없음: versionId={}", versionId);
                return new RuntimeException("PromptVersion not found: " + versionId);
            });
        ensureVersionMatches(version, agentType, concept, intimacyLevel);
        log.info("  - 버전 조회 성공: version={}, filePath={}, content 길이={}자", 
            version.getVersion(), version.getFilePath(), version.getContent() != null ? version.getContent().length() : 0);
        
        // 기존 active 조회 및 삭제
        log.info("=== [PromptService] 기존 active 조회 중 ===");
        Optional<PromptActive> existingActiveOpt = promptActiveRepository.findByEnvAndAgentTypeAndConceptAndIntimacyLevel(
            env, agentType, concept, intimacyLevel
        );
        
        if (existingActiveOpt.isPresent()) {
            PromptActive existingActive = existingActiveOpt.get();
            log.info("  - 기존 active 발견: versionId={}, version={}, activatedAt={}", 
                existingActive.getPromptVersion().getId(), 
                existingActive.getPromptVersion().getVersion(),
                existingActive.getActivatedAt());
            log.info("  - 기존 active 삭제 중");
            promptActiveRepository.delete(existingActive);
            log.info("  - 기존 active 삭제 완료");
        } else {
            log.info("  - 기존 active 없음 (첫 활성화)");
        }
        
        // 새 active 생성
        log.info("=== [PromptService] 새 active 생성 중 ===");
        PromptActiveId activeId = new PromptActiveId(env, agentType, concept, intimacyLevel);
        PromptActive active = PromptActive.builder()
            .id(activeId)
            .promptVersion(version)
            .activatedBy(adminId)
            .build();
        
        PromptActive saved = promptActiveRepository.save(active);
        log.info("=== [PromptService] 프롬프트 활성화 완료 ===");
        log.info("  - 활성화된 버전 ID: {}", saved.getPromptVersion().getId());
        log.info("  - 활성화된 버전 번호: {}", saved.getPromptVersion().getVersion());
        
        // 파일 동기화 (비동기, 실패해도 DB 활성화는 유지)
        boolean synced = false;
        try {
            log.info("=== [PromptService] Chat Service에 파일 동기화 요청 시작 ===");
            synced = chatServiceClient.syncPromptFile(
                agentType.name(),
                concept.getValue(),
                intimacyLevel,
                version.getContent()
            );
            log.info("=== [PromptService] Chat Service에 파일 동기화 요청 완료 ===");
        } catch (Exception e) {
            log.warn("=== [PromptService] Chat Service에 파일 동기화 요청 실패 (DB 활성화는 유지) ===", e);
            // 파일 동기화 실패는 로그만 남기고 예외를 던지지 않음
        }
        log.info("  - 활성화된 파일 경로: {}", saved.getPromptVersion().getFilePath());
        log.info("  - 활성화 시간: {}", saved.getActivatedAt());
        if (!synced) {
            log.warn("  - ⚠️ 중요: DB에 활성화되었지만, Chat Service의 실제 파일({})은 업데이트되지 않았습니다.",
                saved.getPromptVersion().getFilePath());
            log.warn("  - ⚠️ 중요: 실제 Agent는 여전히 resources/prompts/ 디렉토리의 파일을 읽습니다.");
            log.warn("  - ⚠️ 중요: 런타임에 새 프롬프트를 적용하려면 Chat Service의 파일을 업데이트해야 합니다.");
        }
        
        return saved;
    }

    /**
     * 롤백
     */
    @Transactional
    public PromptActive rollbackPrompt(String env, AgentType agentType, Concept concept, Integer intimacyLevel, Long previousVersionId, UUID adminId) {
        return activatePrompt(env, agentType, concept, intimacyLevel, previousVersionId, adminId);
    }

    /**
     * 버전 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PromptVersion> getVersions(AgentType agentType, Concept concept, Integer intimacyLevel, Pageable pageable) {
        validatePromptKey(agentType, concept, intimacyLevel);
        return promptVersionRepository.findByAgentTypeAndConceptAndIntimacyLevel(agentType, concept, intimacyLevel, pageable);
    }

    /**
     * 특정 버전이 활성화되어 있는지 확인 (환경별)
     */
    @Transactional(readOnly = true)
    public boolean isVersionActive(Long versionId, String env) {
        if (versionId == null) {
            return false;
        }
        if (env == null || env.isEmpty()) {
            env = "prod";
        }
        
        Optional<PromptVersion> versionOpt = promptVersionRepository.findById(versionId);
        if (versionOpt.isEmpty()) {
            return false;
        }
        
        PromptVersion version = versionOpt.get();
        Optional<PromptActive> activeOpt = promptActiveRepository.findByEnvAndAgentTypeAndConceptAndIntimacyLevel(
            env, version.getAgentType(), version.getConcept(), version.getIntimacyLevel()
        );
        
        return activeOpt.isPresent() && activeOpt.get().getPromptVersion().getId().equals(versionId);
    }

    /**
     * 버전 상세 조회
     */
    @Transactional(readOnly = true)
    public Optional<PromptVersion> getVersion(Long versionId) {
        return promptVersionRepository.findById(versionId);
    }

    /**
     * 현재 Chat Service의 실제 파일 내용 조회
     */
    @Transactional(readOnly = true)
    public String getPromptFileContent(AgentType agentType, Concept concept, Integer intimacyLevel) {
        validatePromptKey(agentType, concept, intimacyLevel);
        return chatServiceClient.getPromptFileContent(
            agentType.name(),
            concept.getValue(),
            intimacyLevel
        );
    }

    /**
     * 저장 및 적용 (버전 생성 + 활성화 통합)
     */
    @Transactional
    public PromptVersion saveAndActivate(String env, AgentType agentType, Concept concept, Integer intimacyLevel, String content, String memo, UUID adminId) {
        validatePromptKey(agentType, concept, intimacyLevel);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
        if (adminId == null) {
            throw new IllegalArgumentException("adminId is required");
        }
        log.info("=== [PromptService] 저장 및 적용 시작 ===");
        log.info("  - env: {}", env);
        log.info("  - agentType: {}", agentType);
        log.info("  - concept: {}", concept);
        log.info("  - intimacyLevel: {}", intimacyLevel);
        log.info("  - content 길이: {}자", content != null ? content.length() : 0);
        log.info("  - memo: {}", memo);
        log.info("  - adminId: {}", adminId);
        
        // 1. 새 버전 생성
        log.info("=== [PromptService] 1단계: 새 버전 생성 ===");
        PromptVersion version = createVersion(agentType, concept, intimacyLevel, content, memo, adminId);
        log.info("  - 생성된 버전 ID: {}", version.getId());
        log.info("  - 생성된 버전 번호: {}", version.getVersion());
        
        // 2. 즉시 활성화
        log.info("=== [PromptService] 2단계: 즉시 활성화 ===");
        activatePrompt(env, agentType, concept, intimacyLevel, version.getId(), adminId);
        log.info("=== [PromptService] 저장 및 적용 완료 ===");
        
        return version;
    }

    private void validatePromptKey(AgentType agentType, Concept concept, Integer intimacyLevel) {
        if (agentType == null) {
            throw new IllegalArgumentException("agentType is required");
        }
        if (concept == null) {
            throw new IllegalArgumentException("concept is required");
        }
        if (intimacyLevel == null || intimacyLevel < 1 || intimacyLevel > 3) {
            throw new IllegalArgumentException("intimacyLevel must be between 1 and 3");
        }
    }

    private void ensureVersionMatches(PromptVersion version, AgentType agentType, Concept concept, Integer intimacyLevel) {
        if (!version.getAgentType().equals(agentType)
                || !version.getConcept().equals(concept)
                || !version.getIntimacyLevel().equals(intimacyLevel)) {
            throw new IllegalArgumentException("version does not match agentType/concept/intimacyLevel");
        }
    }
}
