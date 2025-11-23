package com.openforum.admin.dto;

import java.util.List;

/**
 * Response DTO for bulk import endpoint.
 * Reports statistics about the import operation.
 */
public record BulkImportResponse(
        int threadsImported,
        int postsImported,
        List<String> errors) {
    
    public static BulkImportResponse success(int threads, int posts) {
        return new BulkImportResponse(threads, posts, List.of());
    }
    
    public static BulkImportResponse withErrors(int threads, int posts, List<String> errors) {
        return new BulkImportResponse(threads, posts, errors);
    }
}
