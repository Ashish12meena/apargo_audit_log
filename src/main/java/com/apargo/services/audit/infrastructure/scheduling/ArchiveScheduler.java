package com.apargo.services.audit.infrastructure.scheduling;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.infrastructure.config.AuditDefaults;
import com.apargo.services.audit.application.archive.ArchiveService;
import com.apargo.services.audit.application.port.out.AuditMetrics;
import com.apargo.services.audit.common.constant.LogKeys;
import com.apargo.services.audit.infrastructure.config.AuditProperties;

/** Runs the archiver on its cron, on one instance at a time. */
@Component
@ConditionalOnProperty(prefix = "audit.roles", name = "archiver", havingValue = "true")
public class ArchiveScheduler {

    public static final String JOB_NAME = "audit-archiver";
    /** Lease slightly longer than the run budget, so a slow run never overlaps the next one. */
    private static final Duration LEASE_MARGIN = Duration.ofMinutes(5);

    private static final Logger log = LoggerFactory.getLogger(ArchiveScheduler.class);

    private final ArchiveService archiveService;
    private final MongoJobLock jobLock;
    private final AuditMetrics metrics;
    private final Duration lease;

    public ArchiveScheduler(ArchiveService archiveService, MongoJobLock jobLock, AuditMetrics metrics,
                            AuditProperties properties) {
        this.archiveService = archiveService;
        this.jobLock = jobLock;
        this.metrics = metrics;
        this.lease = properties.archiver().maxRun().plus(LEASE_MARGIN);
    }

    @Scheduled(cron = AuditDefaults.Placeholders.ARCHIVER_CRON, zone = "UTC")
    public void run() {
        MDC.put(LogKeys.JOB_NAME, JOB_NAME);
        try {
            if (!jobLock.tryAcquire(JOB_NAME, lease)) {
                log.debug("archive_run_skipped reason=locked_by_other_instance");
                return;
            }
            try {
                archiveService.run();
            } finally {
                jobLock.release(JOB_NAME);
            }
        } catch (RuntimeException e) {
            metrics.archiveRun("failed");
            log.error("archive_run_failed", e);
        } finally {
            MDC.remove(LogKeys.JOB_NAME);
        }
    }
}
