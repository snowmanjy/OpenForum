package com.openforum.admin.service;

import com.openforum.admin.dto.BulkImportRequest;
import com.openforum.admin.dto.BulkImportResponse;
import com.openforum.admin.dto.ImportPostDto;
import com.openforum.admin.dto.ImportThreadDto;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.factory.ThreadFactory;
import com.openforum.domain.factory.ThreadFactory.ImportedPostData;
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
    private final com.openforum.domain.repository.MemberRepository memberRepository;

    public BulkImportService(ThreadRepository threadRepository,
            com.openforum.domain.repository.MemberRepository memberRepository) {
        this.threadRepository = threadRepository;
        this.memberRepository = memberRepository;
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
        // Validate that all authors exist to prevent FK violations
        List<java.util.UUID> authorIds = request.threads().stream()
                .map(ImportThreadDto::authorId)
                .distinct()
                .toList();

        if (!authorIds.isEmpty() && !memberRepository.existsAllById(authorIds)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "One or more authors do not exist in the system. Please ensure all users are migrated before importing threads.");
        }

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
                dto.categoryId(), // Nullable - categories are optional
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
