package com.dorandoran.chat.service;

import com.dorandoran.chat.service.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

/**
 * Excel 파일 읽기 서비스
 * Excel 파일을 읽어서 각 Agent별 DTO 리스트로 변환
 */
@Service
@Slf4j
public class ExcelReaderService {

    /**
     * Intimacy Agent Excel 파일 읽기
     */
    public List<IntimacyExampleDto> readIntimacyExamples() {
        try {
            ClassPathResource resource = new ClassPathResource("data/intimacy agent DB.xlsx");
            return readIntimacyExcel(resource.getInputStream());
        } catch (Exception e) {
            log.error("Intimacy Excel 파일 읽기 실패", e);
            return Collections.emptyList();
        }
    }

    /**
     * Vocabulary Excel 파일 읽기
     */
    public List<VocabularyExampleDto> readVocabularyExamples() {
        try {
            ClassPathResource resource = new ClassPathResource("data/Voca DB.xlsx");
            return readVocabularyExcel(resource.getInputStream());
        } catch (Exception e) {
            log.error("Vocabulary Excel 파일 읽기 실패", e);
            return Collections.emptyList();
        }
    }

    /**
     * Conversation Excel 파일 읽기
     */
    public List<ConversationExampleDto> readConversationExamples() {
        try {
            ClassPathResource resource = new ClassPathResource("data/Conversation DB.xlsx");
            return readConversationExcel(resource.getInputStream());
        } catch (Exception e) {
            log.error("Conversation Excel 파일 읽기 실패", e);
            return Collections.emptyList();
        }
    }

    /**
     * Greeting Excel 파일 읽기
     */
    public List<GreetingExampleDto> readGreetingExamples() {
        try {
            ClassPathResource resource = new ClassPathResource("data/Greeting DB.xlsx");
            return readGreetingExcel(resource.getInputStream());
        } catch (Exception e) {
            log.error("Greeting Excel 파일 읽기 실패", e);
            return Collections.emptyList();
        }
    }

    private List<IntimacyExampleDto> readIntimacyExcel(InputStream inputStream) throws Exception {
        List<IntimacyExampleDto> examples = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            // 각 시트별로 읽기 (concept별로 시트가 분리되어 있을 수 있음)
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String concept = sheet.getSheetName().toLowerCase();
                
                // 헤더 행 찾기
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;
                
                // 컬럼 인덱스 매핑
                Map<String, Integer> columnMap = new HashMap<>();
                for (Cell cell : headerRow) {
                    String cellValue = getCellValueAsString(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        columnMap.put(cellValue.trim().toLowerCase(), cell.getColumnIndex());
                    }
                }
                
                log.debug("Intimacy Excel 시트 '{}' 컬럼 매핑: {}", sheet.getSheetName(), columnMap);
                
                // 데이터 행 읽기
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;
                    
                    try {
                        IntimacyExampleDto dto = parseIntimacyRow(row, columnMap, concept);
                        if (dto != null && dto.getUserMessage() != null && !dto.getUserMessage().trim().isEmpty()) {
                            examples.add(dto);
                        }
                    } catch (Exception e) {
                        log.warn("Intimacy Excel 행 {} 파싱 실패: {}", rowIndex + 1, e.getMessage());
                    }
                }
            }
        }
        
        log.info("Intimacy Excel 파일 읽기 완료: {} 개 예시", examples.size());
        return examples;
    }

    private IntimacyExampleDto parseIntimacyRow(Row row, Map<String, Integer> columnMap, String concept) {
        IntimacyExampleDto.IntimacyExampleDtoBuilder builder = IntimacyExampleDto.builder()
            .concept(concept);
        
        // 컬럼명 매핑 (다양한 변형 지원)
        Integer intimacyLevelCol = findColumn(columnMap, "intimacy_level", "intimacylevel", "level", "unnamed: 1");
        Integer userMessageCol = findColumn(columnMap, "usermessage", "user_message", "user message", "unnamed: 2");
        Integer correctedCol = findColumn(columnMap, "correctedsentence", "corrected_sentence", "corrected sentence", "출력값", "output");
        Integer koCol = findColumn(columnMap, "ko", "korean", "unnamed: 4");
        Integer enCol = findColumn(columnMap, "en", "english", "unnamed: 5");
        Integer criticalCol = findColumn(columnMap, "critical", "문제 사항", "problem");
        
        if (intimacyLevelCol != null) {
            String levelStr = getCellValueAsString(row.getCell(intimacyLevelCol));
            if (levelStr != null && !levelStr.trim().isEmpty() && !levelStr.equalsIgnoreCase("no")) {
                try {
                    builder.intimacyLevel(Integer.parseInt(levelStr.trim()));
                } catch (NumberFormatException e) {
                    // 숫자가 아닌 경우 무시
                }
            }
        }
        
        if (userMessageCol != null) {
            builder.userMessage(getCellValueAsString(row.getCell(userMessageCol)));
        }
        
        if (correctedCol != null) {
            builder.correctedSentence(getCellValueAsString(row.getCell(correctedCol)));
        }
        
        if (koCol != null) {
            builder.ko(getCellValueAsString(row.getCell(koCol)));
        }
        
        if (enCol != null) {
            builder.en(getCellValueAsString(row.getCell(enCol)));
        }
        
        if (criticalCol != null) {
            builder.critical(getCellValueAsString(row.getCell(criticalCol)));
        }
        
        return builder.build();
    }

    private List<VocabularyExampleDto> readVocabularyExcel(InputStream inputStream) throws Exception {
        List<VocabularyExampleDto> examples = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;
                
                Map<String, Integer> columnMap = new HashMap<>();
                for (Cell cell : headerRow) {
                    String cellValue = getCellValueAsString(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        columnMap.put(cellValue.trim().toLowerCase(), cell.getColumnIndex());
                    }
                }
                
                log.debug("Vocabulary Excel 시트 '{}' 컬럼 매핑: {}", sheet.getSheetName(), columnMap);
                
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;
                    
                    try {
                        VocabularyExampleDto dto = parseVocabularyRow(row, columnMap);
                        if (dto != null && dto.getWord() != null && !dto.getWord().trim().isEmpty()) {
                            examples.add(dto);
                        }
                    } catch (Exception e) {
                        log.warn("Vocabulary Excel 행 {} 파싱 실패: {}", rowIndex + 1, e.getMessage());
                    }
                }
            }
        }
        
        log.info("Vocabulary Excel 파일 읽기 완료: {} 개 예시", examples.size());
        return examples;
    }

    private VocabularyExampleDto parseVocabularyRow(Row row, Map<String, Integer> columnMap) {
        VocabularyExampleDto.VocabularyExampleDtoBuilder builder = VocabularyExampleDto.builder();
        
        Integer relationCol = findColumn(columnMap, "relation", "concept");
        Integer contentCol = findColumn(columnMap, "content", "sentence", "text");
        Integer wordCol = findColumn(columnMap, "word");
        Integer difficultyCol = findColumn(columnMap, "difficulty", "level");
        Integer romaCol = findColumn(columnMap, "roma", "romanization", "roman");
        Integer koCol = findColumn(columnMap, "ko", "korean");
        Integer enCol = findColumn(columnMap, "en", "english");
        
        if (relationCol != null) {
            builder.relation(getCellValueAsString(row.getCell(relationCol)));
        }
        
        if (contentCol != null) {
            builder.content(getCellValueAsString(row.getCell(contentCol)));
        }
        
        if (wordCol != null) {
            builder.word(getCellValueAsString(row.getCell(wordCol)));
        }
        
        if (difficultyCol != null) {
            String diffStr = getCellValueAsString(row.getCell(difficultyCol));
            if (diffStr != null) {
                try {
                    builder.difficulty(Integer.parseInt(diffStr.trim()));
                } catch (NumberFormatException e) {
                    // 무시
                }
            }
        }
        
        if (romaCol != null) {
            builder.roma(getCellValueAsString(row.getCell(romaCol)));
        }
        
        if (koCol != null) {
            builder.ko(getCellValueAsString(row.getCell(koCol)));
        }
        
        if (enCol != null) {
            builder.en(getCellValueAsString(row.getCell(enCol)));
        }
        
        return builder.build();
    }

    private List<ConversationExampleDto> readConversationExcel(InputStream inputStream) throws Exception {
        List<ConversationExampleDto> examples = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String concept = sheet.getSheetName();
                
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;
                
                Map<String, Integer> columnMap = new HashMap<>();
                for (Cell cell : headerRow) {
                    String cellValue = getCellValueAsString(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        columnMap.put(cellValue.trim().toLowerCase(), cell.getColumnIndex());
                    }
                }
                
                log.debug("Conversation Excel 시트 '{}' 컬럼 매핑: {}", sheet.getSheetName(), columnMap);
                
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;
                    
                    try {
                        ConversationExampleDto dto = parseConversationRow(row, columnMap, concept);
                        if (dto != null && dto.getUserMessage() != null && !dto.getUserMessage().trim().isEmpty()) {
                            examples.add(dto);
                        }
                    } catch (Exception e) {
                        log.warn("Conversation Excel 행 {} 파싱 실패: {}", rowIndex + 1, e.getMessage());
                    }
                }
            }
        }
        
        log.info("Conversation Excel 파일 읽기 완료: {} 개 예시", examples.size());
        return examples;
    }

    private ConversationExampleDto parseConversationRow(Row row, Map<String, Integer> columnMap, String concept) {
        ConversationExampleDto.ConversationExampleDtoBuilder builder = ConversationExampleDto.builder()
            .concept(concept);
        
        Integer situationCol = findColumn(columnMap, "상황", "situation", "context");
        Integer levelCol = findColumn(columnMap, "intimacy level", "intimacylevel", "level");
        Integer userMsgCol = findColumn(columnMap, "user message", "usermessage", "user_message", "user");
        Integer botMsgCol = findColumn(columnMap, "bot message", "botmessage", "bot_message", "bot");
        
        if (situationCol != null) {
            builder.situation(getCellValueAsString(row.getCell(situationCol)));
        }
        
        if (levelCol != null) {
            String levelStr = getCellValueAsString(row.getCell(levelCol));
            if (levelStr != null) {
                try {
                    builder.intimacyLevel(Integer.parseInt(levelStr.trim()));
                } catch (NumberFormatException e) {
                    // 무시
                }
            }
        }
        
        if (userMsgCol != null) {
            builder.userMessage(getCellValueAsString(row.getCell(userMsgCol)));
        }
        
        if (botMsgCol != null) {
            builder.botMessage(getCellValueAsString(row.getCell(botMsgCol)));
        }
        
        return builder.build();
    }

    private List<GreetingExampleDto> readGreetingExcel(InputStream inputStream) throws Exception {
        List<GreetingExampleDto> examples = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String concept = sheet.getSheetName();
                
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;
                
                Map<String, Integer> columnMap = new HashMap<>();
                for (Cell cell : headerRow) {
                    String cellValue = getCellValueAsString(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        columnMap.put(cellValue.trim().toLowerCase(), cell.getColumnIndex());
                    }
                }
                
                log.debug("Greeting Excel 시트 '{}' 컬럼 매핑: {}", sheet.getSheetName(), columnMap);
                
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;
                    
                    try {
                        GreetingExampleDto dto = parseGreetingRow(row, columnMap, concept);
                        if (dto != null && dto.getBotMessage() != null && !dto.getBotMessage().trim().isEmpty()) {
                            examples.add(dto);
                        }
                    } catch (Exception e) {
                        log.warn("Greeting Excel 행 {} 파싱 실패: {}", rowIndex + 1, e.getMessage());
                    }
                }
            }
        }
        
        log.info("Greeting Excel 파일 읽기 완료: {} 개 예시", examples.size());
        return examples;
    }

    private GreetingExampleDto parseGreetingRow(Row row, Map<String, Integer> columnMap, String concept) {
        GreetingExampleDto.GreetingExampleDtoBuilder builder = GreetingExampleDto.builder()
            .concept(concept);
        
        Integer topicCol = findColumn(columnMap, "topic", "주제");
        Integer levelCol = findColumn(columnMap, "intimacylevel", "intimacy level", "level");
        Integer botMsgCol = findColumn(columnMap, "botmessage", "bot_message", "bot message", "bot");
        Integer guideCol = findColumn(columnMap, "guidemessage", "guide_message", "guide message", "guide");
        
        if (topicCol != null) {
            builder.topic(getCellValueAsString(row.getCell(topicCol)));
        }
        
        if (levelCol != null) {
            String levelStr = getCellValueAsString(row.getCell(levelCol));
            if (levelStr != null) {
                try {
                    builder.intimacyLevel(Integer.parseInt(levelStr.trim()));
                } catch (NumberFormatException e) {
                    // 무시
                }
            }
        }
        
        if (botMsgCol != null) {
            builder.botMessage(getCellValueAsString(row.getCell(botMsgCol)));
        }
        
        if (guideCol != null) {
            builder.guideMessage(getCellValueAsString(row.getCell(guideCol)));
        }
        
        return builder.build();
    }

    /**
     * 컬럼명 찾기 (여러 가능한 이름 시도)
     */
    private Integer findColumn(Map<String, Integer> columnMap, String... possibleNames) {
        for (String name : possibleNames) {
            Integer index = columnMap.get(name.toLowerCase());
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    /**
     * Cell 값을 String으로 변환
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        CellType cellType = cell.getCellType();
        if (cellType == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cellType == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toString();
            } else {
                // 정수인 경우 소수점 제거
                double numValue = cell.getNumericCellValue();
                if (numValue == (long) numValue) {
                    return String.valueOf((long) numValue);
                } else {
                    return String.valueOf(numValue);
                }
            }
        } else if (cellType == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (cellType == CellType.FORMULA) {
            return cell.getCellFormula();
        } else if (cellType == CellType.BLANK) {
            return null;
        } else {
            return null;
        }
    }
}

