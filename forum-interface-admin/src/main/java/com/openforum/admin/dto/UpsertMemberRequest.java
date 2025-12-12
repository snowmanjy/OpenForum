package com.openforum.admin.dto;

public record UpsertMemberRequest(
                String externalId,
                String email,
                String name,
                String role,
                String tenantId,
                String avatarUrl) {
}
