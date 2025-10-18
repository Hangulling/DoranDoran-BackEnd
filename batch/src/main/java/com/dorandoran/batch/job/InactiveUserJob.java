package com.dorandoran.batch.job;

import com.dorandoran.batch.common.TimeProvider;
import com.dorandoran.batch.service.InactiveUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InactiveUserJob {

    private final InactiveUserService inactiveUserService;
    private final TimeProvider timeProvider;

    /**
     * 2. 비활성 사용자 처리 (Inactive User Management)
     * 매주 일요일 새벽 2시
     */
    @Scheduled(cron = "0 0 2 * * SUN", zone = "Asia/Seoul")
    public void processInactiveUsers() {
        var now = timeProvider.nowKst();
        log.info("[InactiveUserJob] Start at {}", now);

        int inact90 = inactiveUserService.markInactiveUsersAfter90Days();
        int notify1y = inactiveUserService.countUsersForOneYearNotification();
        int pseudo2y = inactiveUserService.pseudonymizeUsersAfter2Years();

        // 실제 이메일 발송은 별도 메일러 서비스 연동 필요. 여기서는 건수만 로깅
        log.info("[InactiveUserJob] Finished. 90d->INACTIVE={}, notify>=1y={}, pseudonymized>=2y={}", inact90, notify1y, pseudo2y);
    }
}


