package com.openforum.admin.service;

import com.openforum.admin.dto.BulkImportRequest;
import com.openforum.admin.dto.BulkImportResponse;
import com.openforum.admin.dto.ImportPostDto;
import com.openforum.admin.dto.ImportThreadDto;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.aggregate.ThreadFactory;
import com.openforum.domain.aggregate.ThreadFactory.ImportedPostData;
import com.openforum.domain.aggregate.ThreadStatus;
import com.openforum.domain.repository.ThreadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for bulk import operations.
 * Handles transaction boundaries and orchestrates the import process.
 */
@Service
public class BulkImportService {

    private final ThreadRepository threadRepository;

    public BulkImportService(ThreadRepository threadRepository) {
        this.threadRepository = threadRepository;
    }

    /**
     * Imports threads in bulk without generating domain events.
     * This prevents notification storms during migration.
     * 
     * @param request Bulk import request with threads and posts
     * @return Import statistics
     */
    @Transactional
    public BulkImportResponse importThreads(BulkImportRequest request) {
        // Convert DTOs to domain aggregates using the event-less factory
        List<Thread> threads = request.threads().stream()
                .map(this::toDomainThread)
                .toList();

        // Batch save all threads (events will be empty, so outbox stays clean)
        threadRepository.saveAll(threads);

        // Calculate statistics
        int totalPosts = request.threads().stream()
                .mapToInt(dto -> dto.posts().size())
                .sum();

        return BulkImportResponse.success(threads.size(), totalPosts);
    }

    private Thread toDomainThread(ImportThreadDto dto) {
        // Convert post DTOs to domain value objects
        List<ImportedPostData> posts = dto.posts().stream()
                .map(this::toImportedPostData)
                .toList();

        // Use ThreadFactory.createImported to bypass event generation
        return ThreadFactory.createImported(
                dto.id(),
                dto.tenantId(),
                dto.authorId(),
                dto.title(),
                dto.status() != null ? dto.status() : ThreadStatus.OPEN,
                dto.createdAt(),
                dto.metadata() != null ? dto.metadata() : java.util.Map.of(),
                posts);
    }

    private ImportedPostData toImportedPostData(ImportPostDto dto) {
        return new ImportedPostData(
                dto.id(),
                dto.authorId(),
                dto.content(),
                dto.replyToPostId(),
                dto.metadata() != null ? dto.metadata() : java.util.Map.of(),
                dto.isBot(),
                dto.createdAt());
    }
}
