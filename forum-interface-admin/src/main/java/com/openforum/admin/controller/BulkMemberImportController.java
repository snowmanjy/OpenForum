package com.openforum.admin.controller;

import com.openforum.admin.dto.MemberImportRequest;
import com.openforum.admin.dto.MemberImportResponse;
import com.openforum.admin.service.BulkMemberImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/bulk/members")
@Tag(name = "Bulk Import", description = "APIs for bulk data import")
public class BulkMemberImportController {

    private final BulkMemberImportService bulkMemberImportService;

    public BulkMemberImportController(BulkMemberImportService bulkMemberImportService) {
        this.bulkMemberImportService = bulkMemberImportService;
    }

    @PostMapping
    @Operation(summary = "Import members in bulk", description = "Imports members from an external source. Returns a mapping of correlation IDs to internal UUIDs.")
    public ResponseEntity<MemberImportResponse> importMembers(@RequestBody MemberImportRequest request) {
        MemberImportResponse response = bulkMemberImportService.importMembers(request);
        return ResponseEntity.ok(response);
    }
}
