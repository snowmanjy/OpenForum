package com.openforum.domain.aggregate;

import com.openforum.domain.events.ThreadCreatedEvent;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ThreadTest {

    @Test
    void should_accumulate_event_on_creation() {
        UUID authorId = UUID.randomUUID();
        String tenantId = "tenant-1";
        String title = "Test Thread";
        Map<String, Object> metadata = Map.of("key", "value");

        Thread thread = ThreadFactory.create(tenantId, authorId, title, metadata);

        List<Object> events = thread.pollEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof ThreadCreatedEvent);

        ThreadCreatedEvent event = (ThreadCreatedEvent) events.get(0);
        assertEquals(thread.getId(), event.threadId());
        assertEquals(title, event.title());
    }

    @Test
    void should_clear_events_after_polling() {
        Thread thread = ThreadFactory.create("t1", UUID.randomUUID(), "Title", Map.of());

        assertFalse(thread.pollEvents().isEmpty());
        assertTrue(thread.pollEvents().isEmpty());
    }

    @Test
    void should_return_new_list_on_poll() {
        Thread thread = ThreadFactory.create("t1", UUID.randomUUID(), "Title", Map.of());

        List<Object> events1 = thread.pollEvents();
        List<Object> events2 = thread.pollEvents();

        assertNotSame(events1, events2);
    }
}
