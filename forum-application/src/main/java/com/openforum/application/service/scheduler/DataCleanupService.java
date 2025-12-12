package com.openforum.application.service.scheduler;

import com.openforum.domain.repository.PostRepository;
import com.openforum.domain.repository.ThreadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled service to permanently delete soft-deleted content that exceeds
 * the retention period.
 */
@Service
public class DataCleanupService {

    private static final Logger log = LoggerFactory.getLogger(DataCleanupService.class);
    private static final int BATCH_SIZE = 1000;

    private final PostRepository postRepository;
    private final ThreadRepository threadRepository;

    @Value("${app.retention.soft-delete-days:30}")
    private int retentionDays;

    public DataCleanupService(PostRepository postRepository, ThreadRepository threadRepository) {
        this.postRepository = postRepository;
        this.threadRepository = threadRepository;
    }

    /**
     * Runs nightly at 3 AM to purge old data.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldData() {
        log.info("Starting data cleanup job. Retention days: {}", retentionDays);
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        int totalPostsDeleted = 0;
        int totalThreadsDeleted = 0;

        // Cleanup Posts
        int deletedPosts;
        do {
            deletedPosts = postRepository.deleteBatch(cutoff, BATCH_SIZE);
            totalPostsDeleted += deletedPosts;
            log.debug("Deleted batch of {} posts", deletedPosts);
        } while (deletedPosts > 0);

        // Cleanup Threads
        int deletedThreads;
        do {
            deletedThreads = threadRepository.deleteBatch(cutoff, BATCH_SIZE);
            totalThreadsDeleted += deletedThreads;
            log.debug("Deleted batch of {} threads", deletedThreads);
        } while (deletedThreads > 0);

        log.info("Data cleanup job completed. Deleted {} posts and {} threads.",
                totalPostsDeleted, totalThreadsDeleted);
    }
}
