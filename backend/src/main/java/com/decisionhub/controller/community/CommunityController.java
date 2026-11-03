package com.decisionhub.controller.community;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.decisionhub.dto.request.community.CreateCommunityRequest;
import com.decisionhub.dto.request.community.ReportCommunityRequest;
import com.decisionhub.dto.request.community.UpdateCommunityRequest;
import com.decisionhub.dto.request.community.UpdateMemberRoleRequest;
import com.decisionhub.dto.response.community.CommunityJoinRequestResponse;
import com.decisionhub.dto.response.community.CommunityMemberResponse;
import com.decisionhub.dto.response.community.CommunityReportResponse;
import com.decisionhub.dto.response.community.CommunityResponse;
import com.decisionhub.dto.response.community.JoinCommunityResponse;
import com.decisionhub.service.interfaces.community.CommunityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/communities")
@Validated
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping
    public ResponseEntity<CommunityResponse> createCommunity(
            @Valid @RequestBody CreateCommunityRequest request) {

        return ResponseEntity.ok(
                communityService.createCommunity(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<CommunityResponse>> getAllCommunities() {

        return ResponseEntity.ok(
                communityService.getAllCommunities()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityResponse> getCommunityById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                communityService.getCommunityById(id)
        );
    }

    @GetMapping("/my")
    public ResponseEntity<List<CommunityResponse>> getMyCommunities() {
        return ResponseEntity.ok(communityService.getMyCommunities());
    }

    @GetMapping("/moderating")
    public ResponseEntity<List<CommunityResponse>> getModeratingCommunities() {
        return ResponseEntity.ok(
                communityService.getModeratingCommunities()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommunityResponse> updateCommunity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommunityRequest request) {

        return ResponseEntity.ok(
                communityService.updateCommunity(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCommunity(
            @PathVariable Long id) {

        communityService.deleteCommunity(id);

        return ResponseEntity.ok(
                "Community deleted successfully"
        );
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<JoinCommunityResponse> joinCommunity(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                communityService.joinCommunity(id)
        );
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<String> leaveCommunity(
            @PathVariable Long id) {

        communityService.leaveCommunity(id);

        return ResponseEntity.ok(
                "Left community successfully"
        );
    }

    // Moderator Request Endpoints

    @GetMapping("/{id}/requests")
    public ResponseEntity<List<CommunityJoinRequestResponse>> getPendingRequests(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                communityService.getPendingRequests(id)
        );
    }

    @PutMapping("/{id}/requests/{memberId}/approve")
    public ResponseEntity<String> approveJoinRequest(
            @PathVariable Long id,
            @PathVariable Long memberId) {

        communityService.approveJoinRequest(id, memberId);

        return ResponseEntity.ok("Join request approved");
    }

    @PutMapping("/{id}/requests/{memberId}/reject")
    public ResponseEntity<String> rejectJoinRequest(
            @PathVariable Long id,
            @PathVariable Long memberId) {

        communityService.rejectJoinRequest(id, memberId);

        return ResponseEntity.ok("Join request rejected");
    }

    // Final Phase 2 Features (View Members & Remove Member)

    @GetMapping("/{id}/members")
    public ResponseEntity<List<CommunityMemberResponse>> getCommunityMembers(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                communityService.getCommunityMembers(id)
        );
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<String> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId) {

        communityService.removeMember(id, memberId);

        return ResponseEntity.ok("Member removed successfully");
    }

    @PutMapping("/{id}/members/{memberId}/role")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Promote or demote a community member role",
            description = "Modifies the role of a community member. Only the Community Owner or a Platform Admin can perform this action.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<CommunityMemberResponse> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {

        return ResponseEntity.ok(
                communityService.updateMemberRole(id, memberId, request)
        );
    }

    // Report Community & Admin Notification APIs

    @PostMapping("/reportCommunity")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Report a community",
            description = "Allows an approved member of a community to report it for inappropriate actions.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<CommunityReportResponse> reportCommunity(
            @RequestParam(required = false) Long communityId,
            @Valid @RequestBody ReportCommunityRequest request) {
        Long targetId = request.communityId() != null ? request.communityId() : communityId;
        if (targetId == null) {
            throw new com.decisionhub.exception.BadRequestException("Community ID is required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(
                communityService.reportCommunity(targetId, request.reason())
        );
    }

    @PostMapping("/{id}/reportCommunity")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Report a community by path ID",
            description = "Allows an approved member of a community to report it by community ID in URL.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<CommunityReportResponse> reportCommunityById(
            @PathVariable Long id,
            @Valid @RequestBody ReportCommunityRequest request) {
        Long targetId = id != null ? id : request.communityId();
        if (targetId == null) {
            throw new com.decisionhub.exception.BadRequestException("Community ID is required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(
                communityService.reportCommunity(targetId, request.reason())
        );
    }

    @PostMapping("/sendAdminNotification")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Send notification to admin regarding community report action",
            description = "Sends an admin notification regarding a community report action.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<String> sendAdminNotification(
            @RequestParam(required = false) Long communityId,
            @Valid @RequestBody ReportCommunityRequest request) {
        Long targetId = request.communityId() != null ? request.communityId() : communityId;
        if (targetId == null) {
            throw new com.decisionhub.exception.BadRequestException("Community ID is required");
        }
        communityService.sendAdminReportNotification(targetId, request.reason());
        return ResponseEntity.ok("Notification sent to admin");
    }

    @PostMapping("/{id}/notifyAdmin")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Send notification to admin regarding community report action by ID",
            description = "Sends an admin notification regarding a community report action for community ID.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<String> notifyAdmin(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReportCommunityRequest request) {
        String reason = (request != null && request.reason() != null) ? request.reason() : "Inappropriate actions reported";
        communityService.sendAdminReportNotification(id, reason);
        return ResponseEntity.ok("Notification sent to admin");
    }

    @GetMapping("/{id}/reports")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get reports for a community",
            description = "Retrieves reports submitted for a given community.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<CommunityReportResponse>> getCommunityReports(
            @PathVariable Long id) {
        return ResponseEntity.ok(communityService.getCommunityReports(id));
    }

    @GetMapping("/reports")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get all community reports",
            description = "Retrieves all reports submitted across communities.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<CommunityReportResponse>> getAllCommunityReports() {
        return ResponseEntity.ok(communityService.getAllCommunityReports());
    }
}
