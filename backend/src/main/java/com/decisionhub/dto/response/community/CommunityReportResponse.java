package com.decisionhub.dto.response.community;

import java.time.LocalDateTime;

public record CommunityReportResponse(
        Long id,
        Long communityId,
        String communityName,
        Long reporterId,
        String reporterUsername,
        String reason,
        LocalDateTime createdAt
) {}

