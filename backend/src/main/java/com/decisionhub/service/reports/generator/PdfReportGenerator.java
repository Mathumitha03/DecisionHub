package com.decisionhub.service.reports.generator;

import com.decisionhub.dto.response.analytics.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PdfReportGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Color PRIMARY_COLOR = new Color(79, 70, 229); // Indigo #4F46E5
    private static final Color DARK_TITLE = new Color(15, 23, 42); // Slate 900
    private static final Color MUTED_GRAY = new Color(100, 116, 139); // Slate 500
    private static final Color TABLE_HEADER_BG = new Color(79, 70, 229);
    private static final Color ROW_ALT_BG = new Color(248, 250, 252);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);

    private final Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
    private final Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED_GRAY);
    private final Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, DARK_TITLE);
    private final Font headerCellFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private final Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, DARK_TITLE);
    private final Font bodyFontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, DARK_TITLE);
    private final Font kpiLabelFont = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED_GRAY);
    private final Font kpiValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY_COLOR);

    public byte[] generateDecisionPdf(Long decisionId,
                                      DecisionOverviewResponse overview,
                                      VoteStatisticsResponse voteStats,
                                      ParticipationResponse participation,
                                      DiscussionStatisticsResponse discussion,
                                      List<RankingResponse> ranking,
                                      DecisionFeedbackAnalyticsResponse feedback) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();

        // 1. Header Banner
        addHeader(document, "Decision Analytics Report", "Decision #" + decisionId + " Overview, Voting Statistics & Engagement");

        // 2. Decision Overview Section
        addSectionTitle(document, "Decision Overview");
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setSpacingAfter(12);

        addKeyValueRow(metaTable, "Decision Title", overview != null ? overview.getDecisionTitle() : "Decision #" + decisionId);
        addKeyValueRow(metaTable, "Decision Status", overview != null ? overview.getDecisionStatus() : "N/A");
        addKeyValueRow(metaTable, "Poll Status", overview != null ? overview.getPollStatus() : "N/A");
        addKeyValueRow(metaTable, "Voting Deadline", overview != null && overview.getDecisionDeadline() != null ? overview.getDecisionDeadline().format(FORMATTER) : "N/A");
        document.add(metaTable);

        // 3. KPI Cards Grid
        addSectionTitle(document, "Key Metrics & Insights");
        PdfPTable kpiTable = new PdfPTable(4);
        kpiTable.setWidthPercentage(100);
        kpiTable.setSpacingAfter(15);

        addKpiCell(kpiTable, "TOTAL VOTES", String.valueOf(voteStats != null && voteStats.getTotalVotes() != null ? voteStats.getTotalVotes() : 0));
        addKpiCell(kpiTable, "PARTICIPANTS", String.valueOf(voteStats != null && voteStats.getTotalParticipants() != null ? voteStats.getTotalParticipants() : 0));
        addKpiCell(kpiTable, "TURNOUT", voteStats != null && voteStats.getVotePercentage() != null ? String.format("%.1f%%", voteStats.getVotePercentage()) : "N/A");
        addKpiCell(kpiTable, "OPTIONS", String.valueOf(voteStats != null && voteStats.getNumberOfOptions() != null ? voteStats.getNumberOfOptions() : 0));

        addKpiCell(kpiTable, "COMMENTS", String.valueOf(discussion != null && discussion.getTotalComments() != null ? discussion.getTotalComments() : 0));
        addKpiCell(kpiTable, "REPLIES", String.valueOf(discussion != null && discussion.getTotalReplies() != null ? discussion.getTotalReplies() : 0));
        addKpiCell(kpiTable, "FEEDBACKS", String.valueOf(feedback != null && feedback.getFeedbackCount() != null ? feedback.getFeedbackCount() : 0));
        addKpiCell(kpiTable, "AVG RATING", feedback != null && feedback.getAverageRating() != null ? String.format("%.2f/5", feedback.getAverageRating()) : "N/A");
        document.add(kpiTable);

        // 4. Voting Ranking & Distribution Table
        addSectionTitle(document, "Voting Results & Ranking");
        PdfPTable rankTable = new PdfPTable(new float[]{1.2f, 1.2f, 4.5f, 2.0f, 2.0f});
        rankTable.setWidthPercentage(100);
        rankTable.setSpacingAfter(15);

        addTableHeaderCell(rankTable, "Rank");
        addTableHeaderCell(rankTable, "ID");
        addTableHeaderCell(rankTable, "Option Name");
        addTableHeaderCell(rankTable, "Votes");
        addTableHeaderCell(rankTable, "Percentage");

        if (ranking != null && !ranking.isEmpty()) {
            boolean alt = false;
            for (RankingResponse r : ranking) {
                Color bg = alt ? ROW_ALT_BG : Color.WHITE;
                addTableCell(rankTable, String.valueOf(r.getRank() != null ? r.getRank() : "-"), bg, Element.ALIGN_CENTER);
                addTableCell(rankTable, String.valueOf(r.getOptionId() != null ? r.getOptionId() : "-"), bg, Element.ALIGN_CENTER);
                addTableCell(rankTable, r.getOptionName() != null ? r.getOptionName() : "", bg, Element.ALIGN_LEFT);
                addTableCell(rankTable, String.valueOf(r.getVoteCount() != null ? r.getVoteCount() : 0), bg, Element.ALIGN_RIGHT);
                addTableCell(rankTable, r.getPercentage() != null ? String.format("%.2f%%", r.getPercentage()) : "0.00%", bg, Element.ALIGN_RIGHT);
                alt = !alt;
            }
        } else {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No voting records available for this decision.", bodyFont));
            emptyCell.setColspan(5);
            emptyCell.setPadding(10);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            rankTable.addCell(emptyCell);
        }
        document.add(rankTable);

        addFooter(document);
        document.close();
        return out.toByteArray();
    }

    public byte[] generateCommunityPdf(Long communityId,
                                       CommunityOverviewResponse overview,
                                       CommunityDecisionStatisticsResponse decisions,
                                       CommunityVotingStatisticsResponse voting,
                                       CommunityDiscussionStatisticsResponse discussion,
                                       CommunityActivityResponse activity,
                                       CommunityModerationAnalyticsResponse moderation) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();

        String commName = overview != null && overview.getCommunityName() != null ? overview.getCommunityName() : "Community #" + communityId;
        addHeader(document, "Community Moderator Analytics", commName + " • Member Engagement, Voting & Moderation");

        // Community KPI Overview
        addSectionTitle(document, "Community Overview");
        PdfPTable kpiTable = new PdfPTable(4);
        kpiTable.setWidthPercentage(100);
        kpiTable.setSpacingAfter(15);

        addKpiCell(kpiTable, "REGISTERED MEMBERS", String.valueOf(overview != null ? overview.getTotalMembers() : 0));
        addKpiCell(kpiTable, "TOTAL DECISIONS", String.valueOf(overview != null ? overview.getTotalDecisions() : (decisions != null ? decisions.getTotalDecisions() : 0)));
        addKpiCell(kpiTable, "ACTIVE DECISIONS", String.valueOf(overview != null ? overview.getActiveDecisions() : (decisions != null ? decisions.getActiveDecisions() : 0)));
        addKpiCell(kpiTable, "CLOSED DECISIONS", String.valueOf(overview != null ? overview.getClosedDecisions() : (decisions != null ? decisions.getClosedDecisions() : 0)));

        addKpiCell(kpiTable, "COMMUNITY VOTES", String.valueOf(voting != null ? voting.getTotalVotes() : 0));
        addKpiCell(kpiTable, "TOTAL DISCUSSIONS", String.valueOf(discussion != null ? discussion.getTotalComments() : 0));
        addKpiCell(kpiTable, "TOTAL REPLIES", String.valueOf(discussion != null ? discussion.getTotalReplies() : 0));
        addKpiCell(kpiTable, "REMOVED COMMENTS", String.valueOf(moderation != null ? moderation.getRemovedComments() : 0));
        document.add(kpiTable);

        // Moderation & Governance Table
        addSectionTitle(document, "Moderation & Governance Control");
        PdfPTable modTable = new PdfPTable(new float[]{4f, 2f});
        modTable.setWidthPercentage(100);
        modTable.setSpacingAfter(15);

        addTableHeaderCell(modTable, "Moderation Metric");
        addTableHeaderCell(modTable, "Count");

        Color bg1 = ROW_ALT_BG;
        addTableCell(modTable, "Locked Discussions", bg1, Element.ALIGN_LEFT);
        addTableCell(modTable, String.valueOf(moderation != null ? moderation.getLockedDiscussions() : 0), bg1, Element.ALIGN_RIGHT);

        Color bg2 = Color.WHITE;
        addTableCell(modTable, "Unlocked Discussions", bg2, Element.ALIGN_LEFT);
        addTableCell(modTable, String.valueOf(moderation != null ? moderation.getUnlockedDiscussions() : 0), bg2, Element.ALIGN_RIGHT);

        addTableCell(modTable, "Pinned Decisions", bg1, Element.ALIGN_LEFT);
        addTableCell(modTable, String.valueOf(moderation != null ? moderation.getPinnedDecisions() : 0), bg1, Element.ALIGN_RIGHT);

        addTableCell(modTable, "Reported Comments", bg2, Element.ALIGN_LEFT);
        addTableCell(modTable, String.valueOf(moderation != null ? moderation.getReportedComments() : 0), bg2, Element.ALIGN_RIGHT);

        addTableCell(modTable, "Removed Comments (Mod Deleted)", bg1, Element.ALIGN_LEFT);
        addTableCell(modTable, String.valueOf(moderation != null ? moderation.getRemovedComments() : 0), bg1, Element.ALIGN_RIGHT);

        document.add(modTable);

        addFooter(document);
        document.close();
        return out.toByteArray();
    }

    public byte[] generateAdminPdf(PlatformOverviewResponse dashboard,
                                   UserAnalyticsResponse users,
                                   CommunityAnalyticsResponse communities,
                                   AdminDecisionStatisticsResponse decisions,
                                   PlatformDiscussionAnalyticsResponse discussion,
                                   AdminFeedbackAnalyticsResponse feedback) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();

        addHeader(document, "Platform-Wide Admin Analytics", "Global Platform Overview, Users, Communities & Engagement Statistics");

        // Platform KPIs
        addSectionTitle(document, "Platform Key Metrics");
        PdfPTable kpiTable = new PdfPTable(4);
        kpiTable.setWidthPercentage(100);
        kpiTable.setSpacingAfter(15);

        addKpiCell(kpiTable, "TOTAL USERS", String.valueOf(dashboard != null ? dashboard.getTotalUsers() : (users != null ? users.getTotalUsers() : 0)));
        addKpiCell(kpiTable, "COMMUNITIES", String.valueOf(dashboard != null ? dashboard.getTotalCommunities() : (communities != null ? communities.getTotalCommunities() : 0)));
        addKpiCell(kpiTable, "DECISIONS", String.valueOf(dashboard != null ? dashboard.getTotalDecisions() : (decisions != null ? decisions.getTotalDecisions() : 0)));
        addKpiCell(kpiTable, "TOTAL VOTES", String.valueOf(dashboard != null ? dashboard.getTotalVotes() : 0));

        addKpiCell(kpiTable, "ACTIVE USERS", String.valueOf(users != null ? users.getActiveUsers() : 0));
        addKpiCell(kpiTable, "INACTIVE USERS", String.valueOf(users != null ? users.getInactiveUsers() : 0));
        addKpiCell(kpiTable, "TOTAL COMMENTS", String.valueOf(dashboard != null ? dashboard.getTotalComments() : (discussion != null ? discussion.getTotalComments() : 0)));
        addKpiCell(kpiTable, "TOTAL REPLIES", String.valueOf(dashboard != null ? dashboard.getTotalReplies() : (discussion != null ? discussion.getTotalReplies() : 0)));
        document.add(kpiTable);

        // Breakdown Table
        addSectionTitle(document, "Decisions & Governance Breakdown");
        PdfPTable detailTable = new PdfPTable(new float[]{4f, 2f});
        detailTable.setWidthPercentage(100);
        detailTable.setSpacingAfter(15);

        addTableHeaderCell(detailTable, "Category / Status");
        addTableHeaderCell(detailTable, "Value");

        Color bg1 = ROW_ALT_BG;
        Color bg2 = Color.WHITE;

        addTableCell(detailTable, "Active Decisions", bg1, Element.ALIGN_LEFT);
        addTableCell(detailTable, String.valueOf(decisions != null ? decisions.getActiveDecisions() : 0), bg1, Element.ALIGN_RIGHT);

        addTableCell(detailTable, "Closed Decisions", bg2, Element.ALIGN_LEFT);
        addTableCell(detailTable, String.valueOf(decisions != null ? decisions.getClosedDecisions() : 0), bg2, Element.ALIGN_RIGHT);

        addTableCell(detailTable, "Draft Decisions", bg1, Element.ALIGN_LEFT);
        addTableCell(detailTable, String.valueOf(decisions != null ? decisions.getDraftDecisions() : 0), bg1, Element.ALIGN_RIGHT);

        addTableCell(detailTable, "Public Decisions", bg2, Element.ALIGN_LEFT);
        addTableCell(detailTable, String.valueOf(decisions != null ? decisions.getPublicDecisions() : 0), bg2, Element.ALIGN_RIGHT);

        addTableCell(detailTable, "Community Decisions", bg1, Element.ALIGN_LEFT);
        addTableCell(detailTable, String.valueOf(decisions != null ? decisions.getCommunityDecisions() : 0), bg1, Element.ALIGN_RIGHT);

        addTableCell(detailTable, "Avg Comments / Decision", bg2, Element.ALIGN_LEFT);
        addTableCell(detailTable, String.valueOf(discussion != null && discussion.getAverageCommentsPerDecision() != null ? String.format("%.2f", discussion.getAverageCommentsPerDecision()) : "0"), bg2, Element.ALIGN_RIGHT);

        addTableCell(detailTable, "Total Feedback Submissions", bg1, Element.ALIGN_LEFT);
        addTableCell(detailTable, String.valueOf(feedback != null && feedback.getTotalDecisionFeedback() != null ? feedback.getTotalDecisionFeedback() : 0), bg1, Element.ALIGN_RIGHT);

        addTableCell(detailTable, "Average Decision Rating", bg2, Element.ALIGN_LEFT);
        addTableCell(detailTable, String.valueOf(feedback != null && feedback.getAverageDecisionRating() != null ? String.format("%.2f / 5.0", feedback.getAverageDecisionRating()) : "N/A"), bg2, Element.ALIGN_RIGHT);

        document.add(detailTable);

        addFooter(document);
        document.close();
        return out.toByteArray();
    }

    private void addHeader(Document document, String title, String subtitle) {
        Paragraph pTitle = new Paragraph("DecisionHub - " + title, titleFont);
        pTitle.setSpacingAfter(2);
        document.add(pTitle);

        Paragraph pSub = new Paragraph(subtitle + " • Generated on: " + LocalDateTime.now().format(FORMATTER), subtitleFont);
        pSub.setSpacingAfter(15);
        document.add(pSub);
    }

    private void addSectionTitle(Document document, String title) {
        Paragraph sec = new Paragraph(title, sectionFont);
        sec.setSpacingBefore(8);
        sec.setSpacingAfter(6);
        document.add(sec);
    }

    private void addKpiCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBackgroundColor(ROW_ALT_BG);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBorderWidth(1);

        Paragraph pVal = new Paragraph(value, kpiValueFont);
        pVal.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pVal);

        Paragraph pLbl = new Paragraph(label, kpiLabelFont);
        pLbl.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pLbl);

        table.addCell(cell);
    }

    private void addKeyValueRow(PdfPTable table, String key, String value) {
        PdfPCell c1 = new PdfPCell(new Phrase(key, bodyFontBold));
        c1.setPadding(5);
        c1.setBackgroundColor(ROW_ALT_BG);
        c1.setBorderColor(BORDER_COLOR);

        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "", bodyFont));
        c2.setPadding(5);
        c2.setBorderColor(BORDER_COLOR);

        table.addCell(c1);
        table.addCell(c2);
    }

    private void addTableHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, headerCellFont));
        cell.setBackgroundColor(TABLE_HEADER_BG);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(TABLE_HEADER_BG);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Color bg, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bodyFont));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addFooter(Document document) {
        Paragraph footer = new Paragraph("DecisionHub Collaborative Polling & Decision-Making Platform • Confidential Report", subtitleFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);
    }
}
