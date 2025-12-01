package com.openforum.admin.dto;

import java.time.Instant;
import java.util.List;

public record MemberImportRequest(List<MemberImportItem> members) {
    public record MemberImportItem(
            String correlationId,
            String externalId,
            String email,
            String name,
            Instant joinedAt) {
    }
}
