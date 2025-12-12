package com.openforum.rest.controller;

import com.openforum.rest.service.BookmarkService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.context.TenantContext;
import com.openforum.rest.controller.dto.PageResponse;
import com.openforum.rest.controller.dto.PostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Bookmarks", description = "Bookmark management APIs")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @Operation(summary = "Bookmark Post", description = "Saves a post to user's private collection")
    @PostMapping("/posts/{postId}/bookmark")
    public ResponseEntity<Void> bookmarkPost(
            @PathVariable UUID postId,
            @com.openforum.shared.api.TenantId String tenantId,
            @AuthenticationPrincipal Member member) {

        bookmarkService.bookmarkPost(member.getId(), postId, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Unbookmark Post", description = "Removes a post from user's private collection")
    @DeleteMapping("/posts/{postId}/bookmark")
    public ResponseEntity<Void> unbookmarkPost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Member member) {

        bookmarkService.unbookmarkPost(member.getId(), postId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get Member Bookmarks", description = "Retrieves paginated list of user's bookmarked posts")
    @GetMapping("/members/{memberId}/bookmarks")
    public ResponseEntity<PageResponse<PostResponse>> getMemberBookmarks(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Member member) {

        // Security: Only allow viewing own bookmarks
        if (!member.getId().equals(memberId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<Post> bookmarks = bookmarkService.getMemberBookmarks(memberId, page, size);

        List<PostResponse> content = bookmarks.getContent().stream()
                .filter(post -> post != null)
                .map(post -> PostResponse.from(post, null)) // Author name not needed for bookmark list
                .toList();

        return ResponseEntity.ok(new PageResponse<>(
                content,
                bookmarks.getNumber(),
                bookmarks.getSize(),
                bookmarks.getTotalElements(),
                bookmarks.getTotalPages(),
                bookmarks.isFirst(),
                bookmarks.isLast()));
    }

    @Operation(summary = "Check Bookmark Status", description = "Checks if user has bookmarked a specific post")
    @GetMapping("/posts/{postId}/bookmark")
    public ResponseEntity<BookmarkStatusResponse> getBookmarkStatus(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Member member) {

        boolean isBookmarked = bookmarkService.isBookmarked(member.getId(), postId);
        return ResponseEntity.ok(new BookmarkStatusResponse(isBookmarked));
    }

    public record BookmarkStatusResponse(boolean isBookmarked) {
    }
}
