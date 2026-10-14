package com.decisionhub.service.interfaces.report;

import com.decisionhub.enums.reports.ReportType;
import com.decisionhub.service.reports.dto.ExportResult;

public interface ReportExportService {

    ExportResult exportDecisionAnalytics(Long decisionId, ReportType format, String username);

    ExportResult exportCommunityAnalytics(Long communityId, ReportType format, String username);

    ExportResult exportAdminAnalytics(ReportType format, String username);
}
