package com.MessLoc.controller;

import com.MessLoc.dto.mess.CreateMessRequest;
import com.MessLoc.dto.mess.MessResponse;
import com.MessLoc.dto.mess.PublicMessResponse;
import com.MessLoc.dto.mess.UpdateMessRequest;
import com.MessLoc.dto.response.ApiResponse;
import com.MessLoc.security.CustomUserDetails;
import com.MessLoc.service.MessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessController {

    private final MessService messService;

    // ==========================================
    // OWNER APIs
    // ==========================================

    @PostMapping("/owner/messes")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MessResponse>> createMess(
            @Valid @RequestBody CreateMessRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        MessResponse response = messService.createMess(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mess created successfully", response));
    }

    @GetMapping("/owner/messes")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<List<MessResponse>>> getOwnerMesses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        List<MessResponse> response = messService.getOwnerMesses(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Messes fetched successfully", response));
    }

    @GetMapping("/owner/messes/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MessResponse>> getOwnerMess(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        MessResponse response = messService.getOwnerMess(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Mess fetched successfully", response));
    }

    @PutMapping("/owner/messes/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MessResponse>> updateMess(
            @PathVariable String id,
            @Valid @RequestBody UpdateMessRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        MessResponse response = messService.updateMess(id, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Mess updated successfully", response));
    }

    @DeleteMapping("/owner/messes/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteMess(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        messService.deleteMess(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/owner/messes/{id}/activate")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> activateMess(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        messService.activateMess(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Mess activated successfully", null));
    }

    @PatchMapping("/owner/messes/{id}/deactivate")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> deactivateMess(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        messService.deactivateMess(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Mess deactivated successfully", null));
    }

    // ==========================================
    // PUBLIC APIs
    // ==========================================

    @GetMapping("/messes")
    public ResponseEntity<ApiResponse<List<PublicMessResponse>>> getPublicMesses() {
        List<PublicMessResponse> response = messService.getPublicMesses();
        return ResponseEntity.ok(ApiResponse.success("Messes fetched successfully", response));
    }

    @GetMapping("/messes/{id}")
    public ResponseEntity<ApiResponse<PublicMessResponse>> getPublicMess(
            @PathVariable String id) {
        PublicMessResponse response = messService.getPublicMess(id);
        return ResponseEntity.ok(ApiResponse.success("Mess fetched successfully", response));
    }

    // ==========================================
    // ADMIN APIs
    // ==========================================

    @GetMapping("/admin/messes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MessResponse>>> getAllMesses() {
        List<MessResponse> response = messService.getAllMesses();
        return ResponseEntity.ok(ApiResponse.success("All messes fetched successfully", response));
    }

    @GetMapping("/admin/messes/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MessResponse>>> getPendingMesses() {
        List<MessResponse> response = messService.getPendingMesses();
        return ResponseEntity.ok(ApiResponse.success("Pending messes fetched successfully", response));
    }

    @PatchMapping("/admin/messes/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveMess(
            @PathVariable String id) {
        messService.approveMess(id);
        return ResponseEntity.ok(ApiResponse.success("Mess approved successfully", null));
    }

    @PatchMapping("/admin/messes/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectMess(
            @PathVariable String id) {
        messService.rejectMess(id);
        return ResponseEntity.ok(ApiResponse.success("Mess rejected successfully", null));
    }
}
