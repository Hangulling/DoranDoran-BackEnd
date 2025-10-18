package com.dorandoran.batch.job;

import com.dorandoran.batch.common.TimeProvider;
import com.dorandoran.batch.service.DailyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReportJob {

    private final DailyReportService dailyReportService;
    private final TimeProvider timeProvider;

    /**
     * 4. 일간 통계 리포트 생성 (Daily Analytics Report)
     * 매일 오전 9시 (관리자에게 발송)
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void generateDailyReport() {
        var now = timeProvider.nowKst();
        log.info("[DailyReportJob] Start at {}", now);

        var report = dailyReportService.generateReportForYesterday(timeProvider.kst());
        File file = dailyReportService.writeReportToFile(report, LocalDate.now(timeProvider.kst()).minusDays(1));

        // TODO: Slack/Email 발송 연동 포인트 (현재는 파일 저장 및 로그만)
        log.info("[DailyReportJob] Report generated at: {}", file.getAbsolutePath());
    }
}


