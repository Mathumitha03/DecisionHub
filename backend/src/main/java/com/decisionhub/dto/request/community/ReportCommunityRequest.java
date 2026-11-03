package com.decisionhub.dto.request.community;

import jakarta.validation.constraints.NotBlank;

public record ReportCommunityRequest(
        Long communityId,
        @NotBlank(message = "Report reason is required.")
        String reason
) {}

