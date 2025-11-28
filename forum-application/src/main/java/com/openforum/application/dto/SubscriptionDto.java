package com.openforum.application.dto;

import com.openforum.domain.valueobject.TargetType;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionDto(
        UUID targetId,
        TargetType targetType,
        String title,
        LocalDateTime subscribedAt) {
}
