package com.openforum.application.service;

import com.openforum.domain.repository.ThreadRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class ThreadArchivalService {

    private final ThreadRepository threadRepository;
    private final int archiveDays;

    public ThreadArchivalService(
            ThreadRepository threadRepository,
            @Value("${app.retention.archive-days:365}") int archiveDays) {
        this.threadRepository = threadRepository;
        this.archiveDays = archiveDays;
    }

    /**
     * Runs every night at 4 AM to archive stale threads.
     * Moves threads with no activity for 'archiveDays' to ARCHIVED status.
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void archiveStaleThreads() {
        Instant cutoff = Instant.now().minus(archiveDays, ChronoUnit.DAYS);

        int archivedCount = threadRepository.archiveStaleThreads(cutoff);

        if (archivedCount > 0) {
            System.out.println("Archived " + archivedCount + " threads due to inactivity.");
        }
    }
}
