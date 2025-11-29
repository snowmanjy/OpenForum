package com.openforum.application.dto;

import java.util.List;
import java.util.UUID;

public record CreatePrivateThreadRequest(
        List<UUID> participantIds,
        String subject,
        String initialMessage) {
}
