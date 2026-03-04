package com.dorandoran.user.admin.controller;

import com.dorandoran.user.admin.dto.PromptTestRequest;
import com.dorandoran.user.admin.dto.PromptTestResponse;
import com.dorandoran.user.admin.dto.request.PromptActivateRequest;
import com.dorandoran.user.admin.dto.request.PromptRollbackRequest;
import com.dorandoran.user.admin.dto.request.PromptVersionCreateRequest;
import com.dorandoran.user.admin.dto.request.PromptSaveAndActivateRequest;
import com.dorandoran.user.admin.dto.response.PromptActiveResponse;
import com.dorandoran.user.admin.dto.response.PromptVersionListResponse;
import com.dorandoran.user.admin.dto.response.PromptVersionResponse;
import com.dorandoran.user.admin.dto.response.AgentTypeOption;
import com.dorandoran.user.admin.dto.response.ConceptOption;
import com.dorandoran.user.admin.dto.response.IntimacyLevelOption;
import com.dorandoran.user.admin.entity.PromptActive;
import com.dorandoran.user.admin.entity.PromptVersion;
import com.dorandoran.user.entity.User;
import com.dorandoran.user.admin.enums.AgentType;
import com.dorandoran.user.admin.enums.Concept;
import com.dorandoran.user.admin.service.AdminAuditLogService;
import com.dorandoran.user.admin.service.PromptService;
import com.dorandoran.user.admin.repository.PromptActiveRepository;
import com.dorandoran.user.repository.UserRepository;
import com.dorandoran.user.admin.enums.ActionType;
import com.dorandoran.user.admin.enums.TargetType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 프롬프트 관리 Controller
 */
@RestController
@RequestMapping("/api/admin/prompts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Prompt Management", description = "프롬프트 관리 API")
public class PromptController {

    private final PromptService promptService;
    private final PromptActiveRepository promptActiveRepository;
    private final AdminAuditLogService adminAuditLogService;
    private final UserRepository userRepository;

    /** 작성자 UUID로 이메일 조회 (표시용 createdByName) */
    private String resolveCreatorEmail(UUID createdBy) {
        if (createdBy == null) return null;
        return userRepository.findById(createdBy).map(User::getEmail).orElse(null);
    }

    @GetMapping("/active")
    @Operation(summary = "Active 프롬프트 조회", description = "현재 활성화된 프롬프트를 조회합니다.")
    public ResponseEntity<PromptActiveResponse> getActivePrompt(
            @RequestParam String agentType,
            @RequestParam String concept,
            @RequestParam Integer intimacyLevel,
            @RequestParam(required = false, defaultValue = "prod") String env,
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            AgentType agentTypeEnum = AgentType.valueOf(agentType.toUpperCase());
            Concept conceptEnum = Concept.fromString(concept);
            
            Optional<PromptActive> activeOpt = promptActiveRepository.findByEnvAndAgentTypeAndConceptAndIntimacyLevel(
                env, agentTypeEnum, conceptEnum, intimacyLevel
            );
            
            if (activeOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            PromptActive active = activeOpt.get();
            PromptVersion version = active.getPromptVersion();
            PromptActiveResponse response = PromptActiveResponse.builder()
                .env(env)
                .agentType(agentTypeEnum.name())
                .concept(conceptEnum.getValue())
                .intimacyLevel(intimacyLevel)
                .versionId(version.getId())
                .version(version.getVersion())
                .content(version.getContent())
                .activatedAt(active.getActivatedAt())
                .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Active 프롬프트 조회 유효성 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Active 프롬프트 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/test")
    @Operation(summary = "프롬프트 테스트 실행", description = "프롬프트를 테스트합니다.")
    public ResponseEntity<PromptTestResponse> testPrompt(
            @RequestBody PromptTestRequest request,
            @RequestHeader("X-User-Id") String userId
    ) {
        log.info("=== [User Service Controller] 프롬프트 테스트 요청 수신 ===");
        log.info("  - userId: {}", userId);
        log.info("  - agentType: {}", request.getAgentType());
        log.info("  - concept: {}", request.getConcept());
        log.info("  - intimacyLevel: {}", request.getIntimacyLevel());
        log.info("  - inputText 길이: {}자", request.getInputText() != null ? request.getInputText().length() : 0);
        log.info("  - inputText 미리보기: {}", request.getInputText() != null && request.getInputText().length() > 100 
            ? request.getInputText().substring(0, 100) + "..." : request.getInputText());
        
        try {
            AgentType agentTypeEnum = AgentType.valueOf(request.getAgentType().toUpperCase());
            Concept conceptEnum = Concept.fromString(request.getConcept());
            
            log.info("=== [User Service Controller] PromptService.testPrompt() 호출 시작 ===");
            long startTime = System.currentTimeMillis();
            
            PromptTestResponse response = promptService.testPrompt(
                agentTypeEnum,
                conceptEnum,
                request.getIntimacyLevel(),
                request.getInputText(),
                request.getPromptVersionId()
            );
            
            long endTime = System.currentTimeMillis();
            log.info("=== [User Service Controller] PromptService.testPrompt() 완료 ===");
            log.info("  - 총 소요 시간: {}ms", endTime - startTime);
            log.info("  - 응답 latencyMs: {}ms", response.getLatencyMs());
            log.info("  - 응답 outputText 길이: {}자", response.getOutputText() != null ? response.getOutputText().length() : 0);
            log.info("  - 응답 tokens: {}", response.getTokens());
            log.info("  - 응답 outputText 미리보기: {}", response.getOutputText() != null && response.getOutputText().length() > 200 
                ? response.getOutputText().substring(0, 200) + "..." : response.getOutputText());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("프롬프트 테스트 유효성 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("=== [User Service Controller] 프롬프트 테스트 실패 ===", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/versions")
    @Operation(summary = "새 버전 생성", description = "새로운 프롬프트 버전을 생성합니다.")
    public ResponseEntity<PromptVersionResponse> createVersion(
            @RequestBody PromptVersionCreateRequest request,
            @RequestHeader("X-User-Id") String userId,
            HttpServletRequest httpRequest
    ) {
        log.info("=== [User Service Controller] 프롬프트 버전 생성 요청 수신 ===");
        log.info("  - userId: {}", userId);
        log.info("  - agentType: {}", request.getAgentType());
        log.info("  - concept: {}", request.getConcept());
        log.info("  - intimacyLevel: {}", request.getIntimacyLevel());
        log.info("  - content 길이: {}자", request.getContent() != null ? request.getContent().length() : 0);
        log.info("  - memo: {}", request.getMemo());
        
        try {
            AgentType agentTypeEnum = AgentType.valueOf(request.getAgentType().toUpperCase());
            Concept conceptEnum = Concept.fromString(request.getConcept());
            UUID adminId = UUID.fromString(userId);
            
            log.info("=== [User Service Controller] PromptService.createVersion() 호출 시작 ===");
            long startTime = System.currentTimeMillis();
            
            PromptVersion version = promptService.createVersion(
                agentTypeEnum,
                conceptEnum,
                request.getIntimacyLevel(),
                request.getContent(),
                request.getMemo(),
                adminId
            );
            
            long endTime = System.currentTimeMillis();
            log.info("=== [User Service Controller] PromptService.createVersion() 완료 ===");
            log.info("  - 생성된 버전 ID: {}", version.getId());
            log.info("  - 생성된 버전 번호: {}", version.getVersion());
            log.info("  - 파일 경로: {}", version.getFilePath());
            log.info("  - 소요 시간: {}ms", endTime - startTime);
            
            // 감사 로그 기록
            Map<String, Object> afterJson = new HashMap<>();
            afterJson.put("versionId", version.getId());
            afterJson.put("version", version.getVersion());
            afterJson.put("agentType", version.getAgentType().name());
            afterJson.put("concept", version.getConcept().getValue());
            afterJson.put("intimacyLevel", version.getIntimacyLevel());
            
            adminAuditLogService.logAction(
                ActionType.PROMPT_CREATE,
                TargetType.PROMPT_VERSION,
                version.getId(),
                String.format("%s %s 레벨%d v%s 생성", agentTypeEnum.name(), conceptEnum.getValue(), request.getIntimacyLevel(), version.getVersion()),
                null,
                afterJson,
                adminId,
                httpRequest
            );
            
            PromptVersionResponse response = PromptVersionResponse.builder()
                .id(version.getId())
                .agentType(version.getAgentType().name())
                .concept(version.getConcept().getValue())
                .intimacyLevel(version.getIntimacyLevel())
                .version(version.getVersion())
                .content(version.getContent())
                .filePath(version.getFilePath())
                .memo(version.getMemo())
                .createdBy(version.getCreatedBy())
                .createdByName(resolveCreatorEmail(version.getCreatedBy()))
                .createdAt(version.getCreatedAt())
                .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("프롬프트 버전 생성 유효성 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("프롬프트 버전 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/activate")
    @Operation(summary = "Active 전환", description = "프롬프트 버전을 활성화합니다.")
    public ResponseEntity<Void> activatePrompt(
            @RequestBody PromptActivateRequest request,
            @RequestHeader("X-User-Id") String userId,
            HttpServletRequest httpRequest
    ) {
        log.info("=== [User Service Controller] 프롬프트 활성화 요청 수신 ===");
        log.info("  - userId: {}", userId);
        log.info("  - env: {}", request.getEnv());
        log.info("  - agentType: {}", request.getAgentType());
        log.info("  - concept: {}", request.getConcept());
        log.info("  - intimacyLevel: {}", request.getIntimacyLevel());
        log.info("  - versionId: {}", request.getVersionId());
        
        try {
            AgentType agentTypeEnum = AgentType.valueOf(request.getAgentType().toUpperCase());
            Concept conceptEnum = Concept.fromString(request.getConcept());
            UUID adminId = UUID.fromString(userId);
            
            log.info("=== [User Service Controller] PromptService.activatePrompt() 호출 시작 ===");
            long startTime = System.currentTimeMillis();
            
            PromptActive active = promptService.activatePrompt(
                request.getEnv(),
                agentTypeEnum,
                conceptEnum,
                request.getIntimacyLevel(),
                request.getVersionId(),
                adminId
            );
            
            long endTime = System.currentTimeMillis();
            log.info("=== [User Service Controller] PromptService.activatePrompt() 완료 ===");
            log.info("  - 활성화된 버전 ID: {}", active.getPromptVersion().getId());
            log.info("  - 활성화된 버전 번호: {}", active.getPromptVersion().getVersion());
            log.info("  - 활성화된 파일 경로: {}", active.getPromptVersion().getFilePath());
            log.info("  - 활성화 시간: {}", active.getActivatedAt());
            log.info("  - 소요 시간: {}ms", endTime - startTime);
            log.warn("  - ⚠️ 주의: DB에 활성화되었지만, Chat Service의 실제 파일은 업데이트되지 않았습니다.");
            log.warn("  - ⚠️ 실제 Agent는 여전히 resources/prompts/ 디렉토리의 파일을 읽습니다.");
            
            // 감사 로그 기록
            Map<String, Object> afterJson = new HashMap<>();
            afterJson.put("versionId", active.getPromptVersion().getId());
            afterJson.put("version", active.getPromptVersion().getVersion());
            afterJson.put("env", active.getId().getEnv());
            
            adminAuditLogService.logAction(
                ActionType.PROMPT_ACTIVATE,
                TargetType.PROMPT_VERSION,
                active.getPromptVersion().getId(),
                String.format("%s %s 레벨%d v%s 활성화", agentTypeEnum.name(), conceptEnum.getValue(), request.getIntimacyLevel(), active.getPromptVersion().getVersion()),
                null,
                afterJson,
                adminId,
                httpRequest
            );
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("프롬프트 활성화 유효성 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("프롬프트 활성화 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/rollback")
    @Operation(summary = "롤백", description = "이전 버전으로 롤백합니다.")
    public ResponseEntity<Void> rollbackPrompt(
            @RequestBody PromptRollbackRequest request,
            @RequestHeader("X-User-Id") String userId,
            HttpServletRequest httpRequest
    ) {
        try {
            AgentType agentTypeEnum = AgentType.valueOf(request.getAgentType().toUpperCase());
            Concept conceptEnum = Concept.fromString(request.getConcept());
            UUID adminId = UUID.fromString(userId);
            
            PromptActive active = promptService.rollbackPrompt(
                request.getEnv(),
                agentTypeEnum,
                conceptEnum,
                request.getIntimacyLevel(),
                request.getPreviousVersionId(),
                adminId
            );
            
            // 감사 로그 기록
            Map<String, Object> afterJson = new HashMap<>();
            afterJson.put("versionId", active.getPromptVersion().getId());
            afterJson.put("version", active.getPromptVersion().getVersion());
            afterJson.put("env", active.getId().getEnv());
            
            adminAuditLogService.logAction(
                ActionType.PROMPT_ROLLBACK,
                TargetType.PROMPT_VERSION,
                active.getPromptVersion().getId(),
                String.format("%s %s 레벨%d v%s로 롤백", agentTypeEnum.name(), conceptEnum.getValue(), request.getIntimacyLevel(), active.getPromptVersion().getVersion()),
                null,
                afterJson,
                adminId,
                httpRequest
            );
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("프롬프트 롤백 유효성 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("프롬프트 롤백 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/versions")
    @Operation(summary = "버전 목록 조회", description = "프롬프트 버전 목록을 조회합니다.")
    public ResponseEntity<PromptVersionListResponse> getVersions(
            @RequestParam String agentType,
            @RequestParam String concept,
            @RequestParam Integer intimacyLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "prod") String env,
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            AgentType agentTypeEnum = AgentType.valueOf(agentType.toUpperCase());
            Concept conceptEnum = Concept.fromString(concept);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<PromptVersion> versionPage = promptService.getVersions(
                agentTypeEnum, conceptEnum, intimacyLevel, pageable
            );
            
            // 현재 활성화된 버전 조회
            Optional<PromptActive> activeOpt = promptActiveRepository.findByEnvAndAgentTypeAndConceptAndIntimacyLevel(
                env, agentTypeEnum, conceptEnum, intimacyLevel
            );
            Long activeVersionId = activeOpt.map(active -> active.getPromptVersion().getId()).orElse(null);
            
            PromptVersionListResponse response = PromptVersionListResponse.builder()
                .content(versionPage.getContent().stream()
                    .map(v -> {
                        boolean isActive = activeVersionId != null && activeVersionId.equals(v.getId());
                        return PromptVersionResponse.builder()
                            .id(v.getId())
                            .agentType(v.getAgentType().name())
                            .concept(v.getConcept().getValue())
                            .intimacyLevel(v.getIntimacyLevel())
                            .version(v.getVersion())
                            .content(v.getContent())
                            .filePath(v.getFilePath())
                            .memo(v.getMemo())
                            .createdBy(v.getCreatedBy())
                            .createdByName(resolveCreatorEmail(v.getCreatedBy()))
                            .createdAt(v.getCreatedAt())
                            .isActive(isActive)
                            .build();
                    })
                    .collect(Collectors.toList()))
                .page(PromptVersionListResponse.PageInfo.builder()
                    .number(versionPage.getNumber())
                    .size(versionPage.getSize())
                    .totalPages(versionPage.getTotalPages())
                    .totalElements(versionPage.getTotalElements())
                    .build())
                .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("프롬프트 버전 목록 유효성 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("프롬프트 버전 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/versions/{versionId}")
    @Operation(summary = "버전 상세 조회", description = "프롬프트 버전 상세 정보를 조회합니다.")
    public ResponseEntity<PromptVersionResponse> getVersion(
            @PathVariable Long versionId,
            @RequestParam(required = false, defaultValue = "prod") String env,
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            Optional<PromptVersion> versionOpt = promptService.getVersion(versionId);
            
            if (versionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            PromptVersion version = versionOpt.get();
            boolean isActive = promptService.isVersionActive(versionId, env);
            
            PromptVersionResponse response = PromptVersionResponse.builder()
                .id(version.getId())
                .agentType(version.getAgentType().name())
                .concept(version.getConcept().getValue())
                .intimacyLevel(version.getIntimacyLevel())
                .version(version.getVersion())
                .content(version.getContent())
                .filePath(version.getFilePath())
                .memo(version.getMemo())
                .createdBy(version.getCreatedBy())
                .createdByName(resolveCreatorEmail(version.getCreatedBy()))
                .createdAt(version.getCreatedAt())
                .isActive(isActive)
                .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("프롬프트 버전 상세 유효성 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("프롬프트 버전 상세 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/file-content")
    @Operation(summary = "파일 내용 조회", description = "현재 Chat Service의 실제 파일 내용을 조회합니다.")
    public ResponseEntity<Map<String, String>> getPromptFileContent(
            @RequestParam String agentType,
            @RequestParam String concept,
            @RequestParam Integer intimacyLevel,
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            AgentType agentTypeEnum = AgentType.valueOf(agentType.toUpperCase());
            Concept conceptEnum = Concept.fromString(concept);
            
            String content = promptService.getPromptFileContent(
                agentTypeEnum, conceptEnum, intimacyLevel
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("content", content);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("프롬프트 파일 내용 유효성 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("프롬프트 파일 내용 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/options/agent-types")
    @Operation(summary = "AgentType 옵션 조회", description = "에이전트 타입 드롭다운 옵션을 조회합니다.")
    public ResponseEntity<List<AgentTypeOption>> getAgentTypeOptions(
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            List<AgentTypeOption> options = Arrays.stream(AgentType.values())
                .map(agentType -> {
                    String label = switch (agentType) {
                        case INTIMACY_ANALYSIS -> "친밀도 분석";
                        case INTIMACY_CORRECTION -> "친밀도 교정";
                        case VOCABULARY_EXTRACTION -> "어휘 추출";
                        case VOCABULARY_EXPLANATION -> "어휘 설명";
                        case CONVERSATION -> "대화";
                        case GREETING -> "인사";
                    };
                    return AgentTypeOption.builder()
                        .value(agentType.name())
                        .label(label)
                        .build();
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(options);
        } catch (Exception e) {
            log.error("AgentType 옵션 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/options/concepts")
    @Operation(summary = "Concept 옵션 조회", description = "컨셉 드롭다운 옵션을 조회합니다.")
    public ResponseEntity<List<ConceptOption>> getConceptOptions(
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            List<ConceptOption> options = Arrays.stream(Concept.values())
                .map(concept -> {
                    String label = switch (concept) {
                        case FRIEND -> "친구";
                        case COWORKER -> "동료";
                        case BOSS -> "상사";
                        case SENIOR -> "선배";
                        case HONEY -> "연인";
                    };
                    return ConceptOption.builder()
                        .value(concept.getValue())
                        .label(label)
                        .build();
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(options);
        } catch (Exception e) {
            log.error("Concept 옵션 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/options/intimacy-levels")
    @Operation(summary = "IntimacyLevel 옵션 조회", description = "친밀도 레벨 드롭다운 옵션을 조회합니다.")
    public ResponseEntity<List<IntimacyLevelOption>> getIntimacyLevelOptions(
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            List<IntimacyLevelOption> options = List.of(
                IntimacyLevelOption.builder()
                    .value(1)
                    .label("레벨 1")
                    .build(),
                IntimacyLevelOption.builder()
                    .value(3)
                    .label("레벨 3")
                    .build()
            );
            
            return ResponseEntity.ok(options);
        } catch (Exception e) {
            log.error("IntimacyLevel 옵션 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/save-and-activate")
    @Operation(summary = "저장 및 적용", description = "프롬프트를 저장하고 즉시 활성화합니다.")
    public ResponseEntity<PromptVersionResponse> saveAndActivate(
            @RequestBody PromptSaveAndActivateRequest request,
            @RequestHeader("X-User-Id") String userId,
            HttpServletRequest httpRequest
    ) {
        log.info("=== [User Service Controller] 저장 및 적용 요청 수신 ===");
        log.info("  - userId: {}", userId);
        log.info("  - env: {}", request.getEnv());
        log.info("  - agentType: {}", request.getAgentType());
        log.info("  - concept: {}", request.getConcept());
        log.info("  - intimacyLevel: {}", request.getIntimacyLevel());
        log.info("  - content 길이: {}자", request.getContent() != null ? request.getContent().length() : 0);
        log.info("  - memo: {}", request.getMemo());
        
        try {
            AgentType agentTypeEnum = AgentType.valueOf(request.getAgentType().toUpperCase());
            Concept conceptEnum = Concept.fromString(request.getConcept());
            UUID adminId = UUID.fromString(userId);
            
            log.info("=== [User Service Controller] PromptService.saveAndActivate() 호출 시작 ===");
            long startTime = System.currentTimeMillis();
            
            PromptVersion version = promptService.saveAndActivate(
                request.getEnv(),
                agentTypeEnum,
                conceptEnum,
                request.getIntimacyLevel(),
                request.getContent(),
                request.getMemo(),
                adminId
            );
            
            long endTime = System.currentTimeMillis();
            log.info("=== [User Service Controller] PromptService.saveAndActivate() 완료 ===");
            log.info("  - 생성된 버전 ID: {}", version.getId());
            log.info("  - 생성된 버전 번호: {}", version.getVersion());
            log.info("  - 소요 시간: {}ms", endTime - startTime);
            
            // 감사 로그 기록
            Map<String, Object> afterJson = new HashMap<>();
            afterJson.put("versionId", version.getId());
            afterJson.put("version", version.getVersion());
            afterJson.put("env", request.getEnv());
            afterJson.put("agentType", agentTypeEnum.name());
            afterJson.put("concept", conceptEnum.getValue());
            afterJson.put("intimacyLevel", request.getIntimacyLevel());
            
            adminAuditLogService.logAction(
                ActionType.PROMPT_CREATE,
                TargetType.PROMPT_VERSION,
                version.getId(),
                String.format("%s %s 레벨%d v%s 저장 및 적용", agentTypeEnum.name(), conceptEnum.getValue(), request.getIntimacyLevel(), version.getVersion()),
                null,
                afterJson,
                adminId,
                httpRequest
            );
            
            PromptVersionResponse response = PromptVersionResponse.builder()
                .id(version.getId())
                .agentType(version.getAgentType().name())
                .concept(version.getConcept().getValue())
                .intimacyLevel(version.getIntimacyLevel())
                .version(version.getVersion())
                .content(version.getContent())
                .filePath(version.getFilePath())
                .memo(version.getMemo())
                .createdBy(version.getCreatedBy())
                .createdByName(resolveCreatorEmail(version.getCreatedBy()))
                .createdAt(version.getCreatedAt())
                .isActive(true)  // 저장 및 적용이므로 항상 활성화됨
                .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("저장 및 적용 유효성 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("저장 및 적용 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/versions/{versionId}/active-status")
    @Operation(summary = "버전 활성화 상태 조회", description = "특정 버전이 활성화되어 있는지 확인합니다.")
    public ResponseEntity<Map<String, Object>> getVersionActiveStatus(
            @PathVariable Long versionId,
            @RequestParam(required = false, defaultValue = "prod") String env,
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            Optional<PromptVersion> versionOpt = promptService.getVersion(versionId);
            
            if (versionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            PromptVersion version = versionOpt.get();
            boolean isActive = promptService.isVersionActive(versionId, env);
            
            Map<String, Object> response = new HashMap<>();
            response.put("versionId", versionId);
            response.put("isActive", isActive);
            response.put("env", env);
            
            if (isActive) {
                Optional<PromptActive> activeOpt = promptActiveRepository.findByEnvAndAgentTypeAndConceptAndIntimacyLevel(
                    env, version.getAgentType(), version.getConcept(), version.getIntimacyLevel()
                );
                if (activeOpt.isPresent()) {
                    response.put("activatedAt", activeOpt.get().getActivatedAt());
                    response.put("activatedBy", activeOpt.get().getActivatedBy());
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("버전 활성화 상태 유효성 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("버전 활성화 상태 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
