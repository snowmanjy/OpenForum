package com.openforum.admin.controller;

import com.openforum.admin.dto.BulkImportRequest;
import com.openforum.admin.dto.BulkImportResponse;
import com.openforum.admin.service.BulkImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin REST controller for bulk operations.
 * Handles high-throughput import endpoints for migrations.
 * 
 * Security: This controller should be restricted to admin users only.
 * (Security configuration to be added in future phase)
 */
@RestController
@RequestMapping("/admin/v1/bulk")
@Tag(name = "Bulk Import", description = "APIs for bulk data ingestion and migration")
public class BulkImportController {

    private final BulkImportService bulkImportService;

    public BulkImportController(BulkImportService bulkImportService) {
        this.bulkImportService = bulkImportService;
    }

    /**
     * Bulk imports threads and posts from legacy systems.
     * Does NOT generate domain events to prevent notification storms.
     * 
     * @param request Bulk import request with threads and posts
     * @return Import statistics
     */
    @Operation(summary = "Bulk Import Threads", description = "Imports threads and posts from legacy systems without generating notification events. "
            +
            "This endpoint is optimized for high-throughput migration.", responses = {
                    @ApiResponse(responseCode = "201", description = "Import successful", content = @Content(schema = @Schema(implementation = BulkImportResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request payload"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
            })
    @PostMapping("/import")
    public ResponseEntity<BulkImportResponse> importThreads(
            @Valid @RequestBody BulkImportRequest request) {

        BulkImportResponse response = bulkImportService.importThreads(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
