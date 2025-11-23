package com.openforum.application.service;

import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.aggregate.ThreadFactory;
import com.openforum.domain.repository.ThreadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ThreadService {

    private final ThreadRepository threadRepository;

    public ThreadService(ThreadRepository threadRepository) {
        this.threadRepository = threadRepository;
    }

    @Transactional
    public Thread createThread(String tenantId, UUID authorId, String title) {
        Thread thread = ThreadFactory.create(tenantId, authorId, title, java.util.Map.of());
        threadRepository.save(thread);
        return thread;
    }

    @Transactional(readOnly = true)
    public Optional<Thread> getThread(UUID id) {
        return threadRepository.findById(id);
    }
}
