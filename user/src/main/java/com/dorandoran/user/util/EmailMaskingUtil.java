package com.dorandoran.user.util;

/**
 * 이메일 마스킹 유틸리티
 */
public class EmailMaskingUtil {
    
    /**
     * 이메일 주소를 마스킹 처리
     * 
     * 마스킹 규칙:
     * - 로컬 파트(@ 앞): 첫 글자만 표시, 나머지는 ***로 마스킹
     * - 도메인 파트(@ 뒤): 첫 2글자만 표시, 나머지는 ***로 마스킹, TLD는 유지
     * 
     * @param email 원본 이메일 주소
     * @return 마스킹된 이메일 주소
     * 
     * 예시:
     * - user@example.com → u***@ex***.com
     * - test@gmail.com → t***@gm***.com
     * - admin@company.co.kr → a***@co***.co.kr
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex == -1) {
            // @가 없으면 그대로 반환 (잘못된 형식)
            return email;
        }
        
        // 로컬 파트 마스킹 (@ 앞부분)
        String localPart = email.substring(0, atIndex);
        String maskedLocalPart;
        if (localPart.length() > 0) {
            maskedLocalPart = localPart.charAt(0) + "***";
        } else {
            maskedLocalPart = "***";
        }
        
        // 도메인 파트 마스킹 (@ 뒷부분)
        String domainPart = email.substring(atIndex + 1);
        String maskedDomainPart = maskDomain(domainPart);
        
        return maskedLocalPart + "@" + maskedDomainPart;
    }
    
    /**
     * 도메인 파트 마스킹
     * 첫 2글자만 표시하고 나머지는 ***로 마스킹, TLD는 유지
     * 
     * @param domain 도메인 (예: example.com, company.co.kr)
     * @return 마스킹된 도메인
     */
    private static String maskDomain(String domain) {
        if (domain == null || domain.isEmpty()) {
            return domain;
        }
        
        // TLD 분리 (마지막 점 이후)
        int lastDotIndex = domain.lastIndexOf('.');
        if (lastDotIndex == -1) {
            // 점이 없으면 전체를 마스킹
            if (domain.length() >= 2) {
                return domain.substring(0, 2) + "***";
            } else {
                return domain.charAt(0) + "***";
            }
        }
        
        String mainDomain = domain.substring(0, lastDotIndex);
        String tld = domain.substring(lastDotIndex); // .com, .co.kr 등
        
        // 메인 도메인 마스킹
        String maskedMainDomain;
        if (mainDomain.length() >= 2) {
            maskedMainDomain = mainDomain.substring(0, 2) + "***";
        } else if (mainDomain.length() == 1) {
            maskedMainDomain = mainDomain + "***";
        } else {
            maskedMainDomain = "***";
        }
        
        return maskedMainDomain + tld;
    }
}

