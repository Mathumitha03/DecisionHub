package com.decisionhub.service.impl.report;

import com.decisionhub.dto.response.analytics.*;
import com.decisionhub.entity.authentication.User;
import com.decisionhub.entity.reports.ReportExport;
import com.decisionhub.enums.administration.AuditActionType;
import com.decisionhub.enums.reports.ReportType;
import com.decisionhub.exception.BadRequestException;
import com.decisionhub.repository.authentication.UserRepository;
import com.decisionhub.repository.reports.ReportExportRepository;
import com.decisionhub.service.analytics.AnalyticsService;
import com.decisionhub.service.interfaces.audit.AuditService;
import com.decisionhub.service.interfaces.report.ReportExportService;
import com.decisionhub.service.reports.dto.ExportResult;
import com.decisionhub.service.reports.generator.CsvReportGenerator;
import com.decisionhub.service.reports.generator.ExcelReportGenerator;
import com.decisionhub.service.reports.generator.PdfReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportServiceImpl implements ReportExportService {

    private final AnalyticsService analyticsService;
    private final PdfReportGenerator pdfReportGenerator;
    private final ExcelReportGenerator excelReportGenerator;
    private final CsvReportGenerator csvReportGenerator;
    private final ReportExportRepository reportExportRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    public ExportResult exportDecisionAnalytics(Long decisionId, ReportType format, String username) {
        DecisionOverviewResponse overview = null;
        try { overview = analyticsService.getDecisionOverview(decisionId); } catch (Exception e) { log.warn("Failed to get decision overview: {}", e.getMessage()); }

        VoteStatisticsResponse voteStats = null;
        try { voteStats = analyticsService.getVoteStatistics(decisionId); } catch (Exception e) { log.warn("Failed to get vote stats: {}", e.getMessage()); }

        ParticipationResponse participation = null;
        try { participation = analyticsService.getParticipation(decisionId); } catch (Exception e) { log.warn("Failed to get participation: {}", e.getMessage()); }

        DiscussionStatisticsResponse discussion = null;
        try { discussion = analyticsService.getDiscussionStatistics(decisionId); } catch (Exception e) { log.warn("Failed to get discussion stats: {}", e.getMessage()); }

        List<RankingResponse> ranking = null;
        try { ranking = analyticsService.getFinalRanking(decisionId); } catch (Exception e) { log.warn("Failed to get ranking: {}", e.getMessage()); }

        DecisionFeedbackAnalyticsResponse feedback = null;
        try { feedback = analyticsService.getDecisionFeedback(decisionId); } catch (Exception e) { log.warn("Failed to get feedback: {}", e.getMessage()); }

        byte[] content;
        String ext;
        String contentType;

        switch (format) {
            case PDF -> {
                content = pdfReportGenerator.generateDecisionPdf(decisionId, overview, voteStats, participation, discussion, ranking, feedback);
                ext = "pdf";
                contentType = "application/pdf";
            }
            case EXCEL -> {
                try {
                    content = excelReportGenerator.generateDecisionExcel(decisionId, overview, voteStats, participation, discussion, ranking, feedback);
                } catch (IOException e) {
                    log.error("Failed to generate Excel report for decision {}", decisionId, e);
                    throw new BadRequestException("Failed to generate Excel report: " + e.getMessage());
                }
                ext = "xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }
            case CSV -> {
                content = csvReportGenerator.generateDecisionCsv(decisionId, overview, voteStats, participation, discussion, ranking, feedback);
                ext = "csv";
                contentType = "text/csv; charset=UTF-8";
            }
            default -> throw new BadRequestException("Unsupported export format: " + format);
        }

        String filename = String.format("DecisionHub_Decision_Analytics_%d.%s", decisionId, ext);
        recordExportAudit(username, format, filename);

        return ExportResult.builder()
                .data(content)
                .filename(filename)
                .contentType(contentType)
                .build();
    }

    @Override
    public ExportResult exportCommunityAnalytics(Long communityId, ReportType format, String username) {
        CommunityOverviewResponse overview = null;
        try { overview = analyticsService.getCommunityOverview(communityId); } catch (Exception e) { log.warn("Failed to get community overview: {}", e.getMessage()); }

        CommunityDecisionStatisticsResponse decisions = null;
        try { decisions = analyticsService.getCommunityDecisionStatistics(communityId); } catch (Exception e) { log.warn("Failed to get community decisions: {}", e.getMessage()); }

        CommunityVotingStatisticsResponse voting = null;
        try { voting = analyticsService.getCommunityVotingStatistics(communityId); } catch (Exception e) { log.warn("Failed to get community voting: {}", e.getMessage()); }

        CommunityDiscussionStatisticsResponse discussion = null;
        try { discussion = analyticsService.getCommunityDiscussionStatistics(communityId); } catch (Exception e) { log.warn("Failed to get community discussion: {}", e.getMessage()); }

        CommunityActivityResponse activity = null;
        try { activity = analyticsService.getCommunityActivity(communityId); } catch (Exception e) { log.warn("Failed to get community activity: {}", e.getMessage()); }

        CommunityModerationAnalyticsResponse moderation = null;
        try { moderation = analyticsService.getCommunityModerationAnalytics(communityId); } catch (Exception e) { log.warn("Failed to get community moderation: {}", e.getMessage()); }

        byte[] content;
        String ext;
        String contentType;

        switch (format) {
            case PDF -> {
                content = pdfReportGenerator.generateCommunityPdf(communityId, overview, decisions, voting, discussion, activity, moderation);
                ext = "pdf";
                contentType = "application/pdf";
            }
            case EXCEL -> {
                try {
                    content = excelReportGenerator.generateCommunityExcel(communityId, overview, decisions, voting, discussion, activity, moderation);
                } catch (IOException e) {
                    log.error("Failed to generate Excel report for community {}", communityId, e);
                    throw new BadRequestException("Failed to generate Excel report: " + e.getMessage());
                }
                ext = "xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }
            case CSV -> {
                content = csvReportGenerator.generateCommunityCsv(communityId, overview, decisions, voting, discussion, activity, moderation);
                ext = "csv";
                contentType = "text/csv; charset=UTF-8";
            }
            default -> throw new BadRequestException("Unsupported export format: " + format);
        }

        String filename = String.format("DecisionHub_Community_Analytics_%d.%s", communityId, ext);
        recordExportAudit(username, format, filename);

        return ExportResult.builder()
                .data(content)
                .filename(filename)
                .contentType(contentType)
                .build();
    }

    @Override
    public ExportResult exportAdminAnalytics(ReportType format, String username) {
        PlatformOverviewResponse dashboard = analyticsService.getPlatformOverview();
        UserAnalyticsResponse users = analyticsService.getUserAnalytics();
        CommunityAnalyticsResponse communities = analyticsService.getCommunityAnalytics();
        AdminDecisionStatisticsResponse decisions = analyticsService.getAdminDecisionStatistics();
        PlatformDiscussionAnalyticsResponse discussion = analyticsService.getPlatformDiscussionAnalytics();
        AdminFeedbackAnalyticsResponse feedback = analyticsService.getAdminFeedbackAnalytics();

        byte[] content;
        String ext;
        String contentType;

        switch (format) {
            case PDF -> {
                content = pdfReportGenerator.generateAdminPdf(dashboard, users, communities, decisions, discussion, feedback);
                ext = "pdf";
                contentType = "application/pdf";
            }
            case EXCEL -> {
                try {
                    content = excelReportGenerator.generateAdminExcel(dashboard, users, communities, decisions, discussion, feedback);
                } catch (IOException e) {
                    log.error("Failed to generate Excel report for admin platform", e);
                    throw new BadRequestException("Failed to generate Excel report: " + e.getMessage());
                }
                ext = "xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }
            case CSV -> {
                content = csvReportGenerator.generateAdminCsv(dashboard, users, communities, decisions, discussion, feedback);
                ext = "csv";
                contentType = "text/csv; charset=UTF-8";
            }
            default -> throw new BadRequestException("Unsupported export format: " + format);
        }

        String filename = String.format("DecisionHub_Platform_Analytics.%s", ext);
        recordExportAudit(username, format, filename);

        return ExportResult.builder()
                .data(content)
                .filename(filename)
                .contentType(contentType)
                .build();
    }

    private void recordExportAudit(String username, ReportType format, String filename) {
        if (username == null || username.isBlank()) return;
        try {
            userRepository.findByEmail(username).ifPresent(user -> {
                ReportExport reportExport = new ReportExport();
                reportExport.setReportType(format);
                reportExport.setFileUrl(filename);
                reportExport.setGeneratedAt(LocalDateTime.now());
                reportExport.setRequestedBy(user);
                ReportExport saved = reportExportRepository.save(reportExport);

                auditService.log(user, AuditActionType.EXPORT_REPORT.name(), "report_exports",
                        saved.getId(), null, filename, null, null);
            });
        } catch (Exception e) {
            log.warn("Could not save report export audit log for user {}: {}", username, e.getMessage());
        }
    }
}
