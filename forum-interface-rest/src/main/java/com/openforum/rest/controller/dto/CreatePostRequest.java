package com.openforum.rest.controller.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreatePostRequest(
                String content,
                UUID replyToPostId,
                Map<String, Object> metadata,
                List<UUID> mentionedUserIds) {
}
