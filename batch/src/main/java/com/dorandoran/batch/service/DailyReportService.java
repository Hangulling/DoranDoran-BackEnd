package com.dorandoran.batch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyReportService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public Map<String, Object> generateReportForYesterday(ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = today.atStartOfDay();

        Map<String, Object> report = new HashMap<>();
        report.put("date", yesterday.format(DateTimeFormatter.ISO_DATE));

        // 신규 가입자 수 (user_schema.app_user.created_at)
        Integer newUsers = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_schema.app_user WHERE created_at >= ? AND created_at < ?",
            Integer.class, start, end);
        report.put("new_users", newUsers == null ? 0 : newUsers);

        // DAU: 어제 메시지를 보낸 고유 사용자 수 (sender_type='user')
        Integer dau = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT sender_id) FROM chat_schema.messages WHERE sender_type = 'user' AND sender_id IS NOT NULL AND created_at >= ? AND created_at < ?",
            Integer.class, start, end);
        report.put("dau", dau == null ? 0 : dau);

        // 총 메시지 수, 평균 응답 시간(ms)
        Integer totalMessages = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_schema.messages WHERE created_at >= ? AND created_at < ?",
            Integer.class, start, end);
        Double avgProcessingMs = jdbcTemplate.queryForObject(
            "SELECT AVG(processing_time_ms) FROM chat_schema.messages WHERE processing_time_ms IS NOT NULL AND created_at >= ? AND created_at < ?",
            Double.class, start, end);
        report.put("total_messages", totalMessages == null ? 0 : totalMessages);
        report.put("avg_processing_ms", avgProcessingMs == null ? 0.0 : avgProcessingMs);

        // AI 토큰 사용량 (billing.ai_usage_events)
        Long inputTokens = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(input_tokens),0) FROM billing.ai_usage_events WHERE event_time >= ? AND event_time < ?",
            Long.class, start, end);
        Long outputTokens = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(output_tokens),0) FROM billing.ai_usage_events WHERE event_time >= ? AND event_time < ?",
            Long.class, start, end);
        report.put("input_tokens", inputTokens == null ? 0L : inputTokens);
        report.put("output_tokens", outputTokens == null ? 0L : outputTokens);

        // 비용 항목은 스키마 의존적이므로 선택: 존재시 합계, 없으면 0
        try {
            Double costUsd = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(cost_usd),0) FROM billing.ai_usage_events WHERE event_time >= ? AND event_time < ?",
                Double.class, start, end);
            report.put("cost_usd", costUsd == null ? 0.0 : costUsd);
        } catch (Exception e) {
            report.put("cost_usd", 0.0);
        }

        // 친밀도 레벨 변화 추이: 전날 업데이트 건수
        Integer intimacyUpdates = 0;
        try {
            intimacyUpdates = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_schema.intimacy_progress WHERE updated_at >= ? AND updated_at < ?",
                Integer.class, start, end);
        } catch (Exception ignore) {
            // 컬럼 미존재 시 0
        }
        report.put("intimacy_updates", intimacyUpdates == null ? 0 : intimacyUpdates);

        return report;
    }

    public File writeReportToFile(Map<String, Object> report, LocalDate forDate) {
        String ts = forDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        File dir = new File("./reports/daily");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, "daily-report-" + ts + ".json");
        try {
            byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(report);
            Files.write(out.toPath(), json);
            return out;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write daily report", e);
        }
    }
}


