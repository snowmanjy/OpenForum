package com.openforum.domain.factory;

import com.openforum.domain.aggregate.Thread;
import java.util.Map;
import java.util.UUID;

public class ThreadFactory {

    public static Thread create(String tenantId, UUID authorId, String title, Map<String, Object> metadata) {
        return new Thread(UUID.randomUUID(), tenantId, authorId, title, metadata);
    }

    public static Thread createLostPetThread(String tenantId, UUID authorId, String title,
            Map<String, Object> metadata) {
        if (!metadata.containsKey("breed")) {
            throw new IllegalArgumentException("Lost Pet thread must have a 'breed'");
        }
        if (!metadata.containsKey("location")) {
            throw new IllegalArgumentException("Lost Pet thread must have a 'location'");
        }
        return create(tenantId, authorId, title, metadata);
    }
}
