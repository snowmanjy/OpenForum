package com.openforum.rest.controller;

import com.openforum.domain.aggregate.Tag;
import com.openforum.domain.repository.TagRepository;
import com.openforum.rest.context.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagRepository tagRepository;

    public TagController(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @GetMapping("/search")
    public ResponseEntity<List<TagDto>> searchTags(@RequestParam String q) {
        String tenantId = TenantContext.getTenantId();
        List<Tag> tags = tagRepository.findByNameStartingWith(tenantId, q.toLowerCase(), 10);
        List<TagDto> response = tags.stream()
                .map(tag -> new TagDto(tag.getId(), tag.getName(), tag.getUsageCount()))
                .toList();
        return ResponseEntity.ok(response);
    }

    public record TagDto(UUID id, String name, long usageCount) {
    }
}
