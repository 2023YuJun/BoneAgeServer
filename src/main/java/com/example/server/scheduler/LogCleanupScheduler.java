package com.example.server.scheduler;

import com.example.server.repository.RequestLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LogCleanupScheduler {
    private final RequestLogRepository logRepository;

    public LogCleanupScheduler(RequestLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    // 每天凌晨1点执行清理
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanOldLogs() {
        logRepository.deleteOldLogs();
    }
}
