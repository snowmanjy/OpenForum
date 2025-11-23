package com.openforum.domain.aggregate;

import java.util.Map;
import java.util.UUID;

public class ThreadFactory {

    public static Thread create(String tenantId, UUID authorId, String title, Map<String, Object> metadata) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .authorId(authorId)
                .title(title)
                .metadata(metadata)
                .isNew(true)
                .build();
    }
}
