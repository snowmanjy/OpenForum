package com.openforum.application.service;

import com.openforum.application.dto.CreatePrivatePostRequest;
import com.openforum.application.dto.CreatePrivateThreadRequest;
import com.openforum.application.dto.PrivatePostDto;
import com.openforum.application.dto.PrivateThreadDto;
import com.openforum.domain.aggregate.PrivatePost;
import com.openforum.domain.aggregate.PrivateThread;
import com.openforum.domain.factory.PrivateThreadFactory;
import com.openforum.domain.repository.PrivateThreadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PrivateThreadService {

    private final PrivateThreadRepository privateThreadRepository;
    private final PrivateThreadFactory privateThreadFactory;

    public PrivateThreadService(PrivateThreadRepository privateThreadRepository,
            PrivateThreadFactory privateThreadFactory) {
        this.privateThreadRepository = privateThreadRepository;
        this.privateThreadFactory = privateThreadFactory;
    }

    @Transactional
    public UUID createThread(String tenantId, UUID authorId, CreatePrivateThreadRequest request) {
        // Ensure author is included in participants
        List<UUID> participants = new java.util.ArrayList<>(request.participantIds());
        if (!participants.contains(authorId)) {
            participants.add(authorId);
        }

        PrivateThread thread = privateThreadFactory.create(tenantId, participants, request.subject());

        // Add initial message if provided
        if (request.initialMessage() != null && !request.initialMessage().isBlank()) {
            thread.addPost(request.initialMessage(), authorId);
        }

        privateThreadRepository.save(thread);
        return thread.getId();
    }

    @Transactional
    public void addPost(String tenantId, UUID threadId, UUID authorId, CreatePrivatePostRequest request) {
        PrivateThread thread = privateThreadRepository.findByIdAndParticipantId(threadId, authorId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found or user is not a participant"));

        thread.addPost(request.content(), authorId);
        privateThreadRepository.save(thread);
    }

    @Transactional(readOnly = true)
    public List<PrivateThreadDto> getMyThreads(String tenantId, UUID userId, int page, int size) {
        return privateThreadRepository.findByParticipantId(tenantId, userId, page, size).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private PrivateThreadDto toDto(PrivateThread thread) {
        List<PrivatePostDto> postDtos = thread.getPosts().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new PrivateThreadDto(
                thread.getId(),
                thread.getSubject(),
                thread.getParticipantIds(),
                thread.getCreatedAt(),
                thread.getLastActivityAt(),
                postDtos);
    }

    private PrivatePostDto toDto(PrivatePost post) {
        return new PrivatePostDto(
                post.getId(),
                post.getAuthorId(),
                post.getContent(),
                post.getCreatedAt());
    }
}
