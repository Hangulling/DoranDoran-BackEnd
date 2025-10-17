package com.dorandoran.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotDirectivesRequest {
    private DirectiveSetting concept;
    private DirectiveSetting intimacy;
    private LanguageDirectiveSetting language;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectiveSetting {
        private Boolean enabled;
        private String custom;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageDirectiveSetting {
        private Boolean enabled;
        private String defaultLang;
        private Boolean allowUserOverride;
    }
}
