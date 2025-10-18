package com.dorandoran.batch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InactiveUserService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 90일 이상 미접속 사용자를 INACTIVE 상태로 전환
     */
    @Transactional
    public int markInactiveUsersAfter90Days() {
        String sql = "UPDATE user_schema.app_user " +
                "SET status = 'INACTIVE', updated_at = NOW() " +
                "WHERE last_conn_time IS NOT NULL " +
                "  AND last_conn_time < NOW() - INTERVAL '90 days' " +
                "  AND status = 'ACTIVE'";
        int affected = jdbcTemplate.update(sql);
        log.info("[InactiveUserService] Marked INACTIVE (>=90d) users = {}", affected);
        return affected;
    }

    /**
     * 1년 이상 미접속 사용자에 대해 알림 이메일 발송 (여기서는 로깅으로 대체)
     */
    @Transactional(readOnly = true)
    public int countUsersForOneYearNotification() {
        String sql = "SELECT COUNT(*) FROM user_schema.app_user " +
                "WHERE last_conn_time IS NOT NULL " +
                "  AND last_conn_time < NOW() - INTERVAL '1 year'";
        Integer cnt = jdbcTemplate.queryForObject(sql, Integer.class);
        int count = cnt != null ? cnt : 0;
        log.info("[InactiveUserService] Users >=1y inactive for notification = {}", count);
        return count;
    }

    /**
     * 2년 이상 미접속 사용자 개인정보 가명처리
     * - email 가명처리, first_name/last_name/picture/info 삭제
     */
    @Transactional
    public int pseudonymizeUsersAfter2Years() {
        String sql = "UPDATE user_schema.app_user " +
                "SET email = 'deleted-' || id::text || '@example.com', " +
                "    first_name = NULL, " +
                "    last_name = NULL, " +
                "    picture = NULL, " +
                "    info = NULL, " +
                "    updated_at = NOW() " +
                "WHERE last_conn_time IS NOT NULL " +
                "  AND last_conn_time < NOW() - INTERVAL '2 years'";
        int affected = jdbcTemplate.update(sql);
        log.info("[InactiveUserService] Pseudonymized (>=2y) users = {}", affected);
        return affected;
    }
}


