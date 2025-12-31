package com.dorandoran.chat.service.agent;

import java.util.Set;

/**
 * 어휘 카테고리 Enum
 * 컨셉별로 적용 가능한 어휘 카테고리를 정의
 */
public enum VocabularyCategory {
    // 공통 카테고리 (모든 컨셉에 적용 가능)
    BASIC_DAILY("일상 기본 어휘", Set.of()),
    HANJA_BASED("한자어 기반 어휘", Set.of()),
    IDIOMATIC_EXPRESSION("관용 표현", Set.of()),
    
    // 컨셉별 특화 카테고리
    MZ_GENERATION("MZ세대 유행어", Set.of("FRIEND", "HONEY")),
    SLANG("속어/신조어", Set.of("FRIEND", "HONEY")),
    BUSINESS_TERMS("비즈니스 용어", Set.of("COWORKER", "BOSS")),
    UNIVERSITY_TERMS("대학교 용어", Set.of("SENIOR")),
    FORMAL_EXPRESSIONS("격식 표현", Set.of("COWORKER", "BOSS", "SENIOR"));
    
    private final String description;
    private final Set<String> applicableConcepts;
    
    VocabularyCategory(String description, Set<String> applicableConcepts) {
        this.description = description;
        this.applicableConcepts = applicableConcepts;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 해당 컨셉에 적용 가능한 카테고리인지 확인
     */
    public boolean isApplicableTo(String concept) {
        if (concept == null) {
            return false;
        }
        String normalizedConcept = concept.toUpperCase();
        // applicableConcepts가 비어있으면 모든 컨셉에 적용 가능
        return applicableConcepts.isEmpty() || applicableConcepts.contains(normalizedConcept);
    }
    
    /**
     * 컨셉별 우선순위 반환 (낮을수록 높은 우선순위)
     */
    public int getPriorityForConcept(String concept) {
        if (concept == null) {
            return 999;
        }
        String normalizedConcept = concept.toUpperCase();
        
        return switch (normalizedConcept) {
            case "FRIEND" -> switch (this) {
                case MZ_GENERATION -> 1;
                case SLANG -> 2;
                case IDIOMATIC_EXPRESSION -> 3;
                case HANJA_BASED -> 4;
                case BASIC_DAILY -> 5;
                default -> 999;
            };
            case "COWORKER", "BOSS" -> switch (this) {
                case BUSINESS_TERMS -> 1;
                case FORMAL_EXPRESSIONS -> 2;
                case HANJA_BASED -> 3;
                case IDIOMATIC_EXPRESSION -> 4;
                case BASIC_DAILY -> 5;
                default -> 999;
            };
            case "SENIOR" -> switch (this) {
                case UNIVERSITY_TERMS -> 1;
                case FORMAL_EXPRESSIONS -> 2;
                case HANJA_BASED -> 3;
                case IDIOMATIC_EXPRESSION -> 4;
                case BASIC_DAILY -> 5;
                default -> 999;
            };
            case "HONEY" -> switch (this) {
                case MZ_GENERATION -> 1;
                case SLANG -> 2;
                case IDIOMATIC_EXPRESSION -> 3;
                case HANJA_BASED -> 4;
                case BASIC_DAILY -> 5;
                default -> 999;
            };
            default -> 999;
        };
    }
}

