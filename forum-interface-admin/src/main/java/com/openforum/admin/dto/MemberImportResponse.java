package com.openforum.admin.dto;

import java.util.Map;
import java.util.UUID;

public record MemberImportResponse(
        int importedCount,
        Map<String, UUID> correlationIdMap) {
}
