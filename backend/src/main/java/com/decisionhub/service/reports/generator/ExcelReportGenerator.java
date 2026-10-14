package com.decisionhub.service.reports.generator;

import com.decisionhub.dto.response.analytics.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ExcelReportGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateDecisionExcel(Long decisionId,
                                        DecisionOverviewResponse overview,
                                        VoteStatisticsResponse voteStats,
                                        ParticipationResponse participation,
                                        DiscussionStatisticsResponse discussion,
                                        List<RankingResponse> ranking,
                                        DecisionFeedbackAnalyticsResponse feedback) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);

            // Sheet 1: Overview & Metrics
            Sheet sheet1 = workbook.createSheet("Decision Overview");
            int rowIdx = 0;

            rowIdx = createTitleBlock(sheet1, rowIdx, styles, "DecisionHub Analytics - Decision #" + decisionId,
                    "Generated: " + LocalDateTime.now().format(FORMATTER));

            rowIdx = createSectionHeader(sheet1, rowIdx, styles, "Decision Information");
            rowIdx = addDataRow(sheet1, rowIdx, styles, "Decision Title", overview != null ? overview.getDecisionTitle() : "Decision #" + decisionId);
            rowIdx = addDataRow(sheet1, rowIdx, styles, "Decision Status", overview != null ? overview.getDecisionStatus() : "N/A");
            rowIdx = addDataRow(sheet1, rowIdx, styles, "Poll Status", overview != null ? overview.getPollStatus() : "N/A");
            rowIdx = addDataRow(sheet1, rowIdx, styles, "Voting Deadline", overview != null && overview.getDecisionDeadline() != null ? overview.getDecisionDeadline().format(FORMATTER) : "N/A");
            rowIdx++;

            rowIdx = createSectionHeader(sheet1, rowIdx, styles, "Voting & Participation Metrics");
            rowIdx = addDataRow(sheet1, rowIdx, styles, "Total Votes", voteStats != null && voteStats.getTotalVotes() != null ? voteStats.getTotalVotes() : 0);
            rowIdx = addDataRow(sheet1, rowIdx, styles, "Total Participants", voteStats != null && voteStats.getTotalParticipants() != null ? voteStats.getTotalParticipants() : 0);
            rowIdx = addDataRow(sheet1, rowIdx, styles, "Turnout Percentage", voteStats != null && voteStats.getVotePercentage() != null ? String.format("%.2f%%", voteStats.getVotePercentage()) : "N/A");
            rowIdx = addDataRow(sheet1, rowIdx, styles, "Number of Options", voteStats != null && voteStats.getNumberOfOptions() != null ? voteStats.getNumberOfOptions() : 0);
            rowIdx++;

            rowIdx = createSectionHeader(sheet1, rowIdx, styles, "Discussion & Feedback");
            rowIdx = addDataRow(sheet1, rowIdx, styles, "Total Discussion Comments", discussion != null && discussion.getTotalComments() != null ? discussion.getTotalComments() : 0);
            rowIdx = addDataRow(sheet1, rowIdx, styles, "Total Replies", discussion != null && discussion.getTotalReplies() != null ? discussion.getTotalReplies() : 0);
            rowIdx = addDataRow(sheet1, rowIdx, styles, "Feedback Submissions", feedback != null && feedback.getFeedbackCount() != null ? feedback.getFeedbackCount() : 0);
            rowIdx = addDataRow(sheet1, rowIdx, styles, "Average Rating", feedback != null && feedback.getAverageRating() != null ? String.format("%.2f / 5.0", feedback.getAverageRating()) : "N/A");

            autoSizeColumns(sheet1, 2);

            // Sheet 2: Options Ranking & Distribution
            Sheet sheet2 = workbook.createSheet("Voting Results");
            int r2 = 0;
            r2 = createTitleBlock(sheet2, r2, styles, "Voting Distribution & Final Ranking",
                    "Options and Vote Counts for Decision #" + decisionId);

            Row tableHeader = sheet2.createRow(r2++);
            createHeaderCell(tableHeader, 0, styles, "Rank");
            createHeaderCell(tableHeader, 1, styles, "Option ID");
            createHeaderCell(tableHeader, 2, styles, "Option Name");
            createHeaderCell(tableHeader, 3, styles, "Votes Received");
            createHeaderCell(tableHeader, 4, styles, "Vote Percentage");

            if (ranking != null && !ranking.isEmpty()) {
                for (RankingResponse r : ranking) {
                    Row row = sheet2.createRow(r2++);
                    createDataCell(row, 0, styles, r.getRank() != null ? r.getRank() : "-", false);
                    createDataCell(row, 1, styles, r.getOptionId() != null ? r.getOptionId() : "-", false);
                    createDataCell(row, 2, styles, r.getOptionName() != null ? r.getOptionName() : "", false);
                    createDataCell(row, 3, styles, r.getVoteCount() != null ? r.getVoteCount() : 0, true);
                    createDataCell(row, 4, styles, r.getPercentage() != null ? String.format("%.2f%%", r.getPercentage()) : "0.00%", true);
                }
            }

            autoSizeColumns(sheet2, 5);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateCommunityExcel(Long communityId,
                                         CommunityOverviewResponse overview,
                                         CommunityDecisionStatisticsResponse decisions,
                                         CommunityVotingStatisticsResponse voting,
                                         CommunityDiscussionStatisticsResponse discussion,
                                         CommunityActivityResponse activity,
                                         CommunityModerationAnalyticsResponse moderation) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);

            Sheet sheet = workbook.createSheet("Community Analytics");
            int rowIdx = 0;

            String commName = overview != null && overview.getCommunityName() != null ? overview.getCommunityName() : "Community #" + communityId;
            rowIdx = createTitleBlock(sheet, rowIdx, styles, "DecisionHub Analytics - " + commName,
                    "Moderator Community Report • Generated: " + LocalDateTime.now().format(FORMATTER));

            rowIdx = createSectionHeader(sheet, rowIdx, styles, "Community Engagement Overview");
            rowIdx = addDataRow(sheet, rowIdx, styles, "Community ID", communityId);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Community Name", commName);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Registered Members", overview != null ? overview.getTotalMembers() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Total Decisions Created", overview != null ? overview.getTotalDecisions() : (decisions != null ? decisions.getTotalDecisions() : 0));
            rowIdx = addDataRow(sheet, rowIdx, styles, "Active Decisions", overview != null ? overview.getActiveDecisions() : (decisions != null ? decisions.getActiveDecisions() : 0));
            rowIdx = addDataRow(sheet, rowIdx, styles, "Closed Decisions", overview != null ? overview.getClosedDecisions() : (decisions != null ? decisions.getClosedDecisions() : 0));
            rowIdx++;

            rowIdx = createSectionHeader(sheet, rowIdx, styles, "Voting & Discussion Analytics");
            rowIdx = addDataRow(sheet, rowIdx, styles, "Total Community Votes", voting != null ? voting.getTotalVotes() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Total Discussion Comments", discussion != null ? discussion.getTotalComments() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Discussion Replies", discussion != null ? discussion.getTotalReplies() : 0);
            rowIdx++;

            rowIdx = createSectionHeader(sheet, rowIdx, styles, "Moderation & Control Actions");
            rowIdx = addDataRow(sheet, rowIdx, styles, "Locked Discussions", moderation != null ? moderation.getLockedDiscussions() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Unlocked Discussions", moderation != null ? moderation.getUnlockedDiscussions() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Pinned Decisions", moderation != null ? moderation.getPinnedDecisions() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Reported Comments", moderation != null ? moderation.getReportedComments() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Removed Comments", moderation != null ? moderation.getRemovedComments() : 0);

            autoSizeColumns(sheet, 2);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateAdminExcel(PlatformOverviewResponse dashboard,
                                     UserAnalyticsResponse users,
                                     CommunityAnalyticsResponse communities,
                                     AdminDecisionStatisticsResponse decisions,
                                     PlatformDiscussionAnalyticsResponse discussion,
                                     AdminFeedbackAnalyticsResponse feedback) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);

            Sheet sheet = workbook.createSheet("Platform Overview");
            int rowIdx = 0;

            rowIdx = createTitleBlock(sheet, rowIdx, styles, "DecisionHub Analytics - Platform Overview",
                    "Global Platform Report • Generated: " + LocalDateTime.now().format(FORMATTER));

            rowIdx = createSectionHeader(sheet, rowIdx, styles, "User Statistics");
            rowIdx = addDataRow(sheet, rowIdx, styles, "Total Registered Users", dashboard != null ? dashboard.getTotalUsers() : (users != null ? users.getTotalUsers() : 0));
            rowIdx = addDataRow(sheet, rowIdx, styles, "Active Users", users != null ? users.getActiveUsers() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Inactive Users", users != null ? users.getInactiveUsers() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Suspended Users", users != null ? users.getSuspendedUsers() : 0);
            rowIdx++;

            rowIdx = createSectionHeader(sheet, rowIdx, styles, "Communities & Decisions");
            rowIdx = addDataRow(sheet, rowIdx, styles, "Total Communities", dashboard != null ? dashboard.getTotalCommunities() : (communities != null ? communities.getTotalCommunities() : 0));
            rowIdx = addDataRow(sheet, rowIdx, styles, "Total Decisions", dashboard != null ? dashboard.getTotalDecisions() : (decisions != null ? decisions.getTotalDecisions() : 0));
            rowIdx = addDataRow(sheet, rowIdx, styles, "Active Decisions", decisions != null ? decisions.getActiveDecisions() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Closed Decisions", decisions != null ? decisions.getClosedDecisions() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Draft Decisions", decisions != null ? decisions.getDraftDecisions() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Public Decisions", decisions != null ? decisions.getPublicDecisions() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Community Decisions", decisions != null ? decisions.getCommunityDecisions() : 0);
            rowIdx++;

            rowIdx = createSectionHeader(sheet, rowIdx, styles, "Engagement & Moderation");
            rowIdx = addDataRow(sheet, rowIdx, styles, "Total Platform Votes", dashboard != null ? dashboard.getTotalVotes() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Total Discussion Comments", dashboard != null ? dashboard.getTotalComments() : (discussion != null ? discussion.getTotalComments() : 0));
            rowIdx = addDataRow(sheet, rowIdx, styles, "Total Discussion Replies", dashboard != null ? dashboard.getTotalReplies() : (discussion != null ? discussion.getTotalReplies() : 0));
            rowIdx = addDataRow(sheet, rowIdx, styles, "Average Comments Per Decision", discussion != null && discussion.getAverageCommentsPerDecision() != null ? String.format("%.2f", discussion.getAverageCommentsPerDecision()) : "0");
            rowIdx = addDataRow(sheet, rowIdx, styles, "Total Feedback Submissions", feedback != null && feedback.getTotalDecisionFeedback() != null ? feedback.getTotalDecisionFeedback() : 0);
            rowIdx = addDataRow(sheet, rowIdx, styles, "Average Platform Rating", feedback != null && feedback.getAverageDecisionRating() != null ? String.format("%.2f / 5.0", feedback.getAverageDecisionRating()) : "N/A");

            autoSizeColumns(sheet, 2);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private int createTitleBlock(Sheet sheet, int rowIdx, Styles styles, String title, String subtitle) {
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(styles.titleStyle);

        Row subRow = sheet.createRow(rowIdx++);
        Cell subCell = subRow.createCell(0);
        subCell.setCellValue(subtitle);
        subCell.setCellStyle(styles.subtitleStyle);

        rowIdx++; // blank spacing
        return rowIdx;
    }

    private int createSectionHeader(Sheet sheet, int rowIdx, Styles styles, String headerTitle) {
        Row row = sheet.createRow(rowIdx++);
        Cell cell = row.createCell(0);
        cell.setCellValue(headerTitle);
        cell.setCellStyle(styles.sectionStyle);

        Cell cell2 = row.createCell(1);
        cell2.setCellStyle(styles.sectionStyle);
        return rowIdx;
    }

    private int addDataRow(Sheet sheet, int rowIdx, Styles styles, String metric, Object value) {
        Row row = sheet.createRow(rowIdx++);
        Cell c0 = row.createCell(0);
        c0.setCellValue(metric);
        c0.setCellStyle(styles.dataLabelStyle);

        Cell c1 = row.createCell(1);
        if (value instanceof Number) {
            c1.setCellValue(((Number) value).doubleValue());
            c1.setCellStyle(styles.dataNumberStyle);
        } else {
            c1.setCellValue(value != null ? value.toString() : "");
            c1.setCellStyle(styles.dataValueStyle);
        }
        return rowIdx;
    }

    private void createHeaderCell(Row row, int col, Styles styles, String text) {
        Cell cell = row.createCell(col);
        cell.setCellValue(text);
        cell.setCellStyle(styles.headerStyle);
    }

    private void createDataCell(Row row, int col, Styles styles, Object val, boolean isNumeric) {
        Cell cell = row.createCell(col);
        if (isNumeric && val instanceof Number) {
            cell.setCellValue(((Number) val).doubleValue());
            cell.setCellStyle(styles.dataNumberStyle);
        } else {
            cell.setCellValue(val != null ? val.toString() : "");
            cell.setCellStyle(isNumeric ? styles.dataNumberStyle : styles.dataValueStyle);
        }
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.max(currentWidth + 1000, 5000));
        }
    }

    private Styles createStyles(XSSFWorkbook wb) {
        Styles s = new Styles();

        // Palette
        java.awt.Color primaryColor = new java.awt.Color(79, 70, 229); // Indigo #4F46E5
        java.awt.Color sectionColor = new java.awt.Color(238, 242, 255); // Soft indigo tint #EEF2FF
        java.awt.Color borderColor = new java.awt.Color(203, 213, 225); // Slate 300

        // Title
        XSSFFont titleFont = wb.createFont();
        titleFont.setFontHeightInPoints((short) 15);
        titleFont.setBold(true);
        titleFont.setColor(new XSSFColor(primaryColor, null));
        s.titleStyle = wb.createCellStyle();
        s.titleStyle.setFont(titleFont);

        // Subtitle
        XSSFFont subFont = wb.createFont();
        subFont.setFontHeightInPoints((short) 10);
        subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.subtitleStyle = wb.createCellStyle();
        s.subtitleStyle.setFont(subFont);

        // Section header
        XSSFFont secFont = wb.createFont();
        secFont.setFontHeightInPoints((short) 11);
        secFont.setBold(true);
        secFont.setColor(new XSSFColor(new java.awt.Color(30, 27, 75), null));
        s.sectionStyle = wb.createCellStyle();
        s.sectionStyle.setFont(secFont);
        s.sectionStyle.setFillForegroundColor(new XSSFColor(sectionColor, null));
        s.sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(s.sectionStyle);

        // Table Header
        XSSFFont headerFont = wb.createFont();
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        s.headerStyle = wb.createCellStyle();
        s.headerStyle.setFont(headerFont);
        s.headerStyle.setFillForegroundColor(new XSSFColor(primaryColor, null));
        s.headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.headerStyle.setAlignment(HorizontalAlignment.CENTER);
        setBorders(s.headerStyle);

        // Data labels
        XSSFFont dataFont = wb.createFont();
        dataFont.setFontHeightInPoints((short) 10);
        s.dataLabelStyle = wb.createCellStyle();
        s.dataLabelStyle.setFont(dataFont);
        s.dataLabelStyle.setAlignment(HorizontalAlignment.LEFT);
        setBorders(s.dataLabelStyle);

        // Data value
        s.dataValueStyle = wb.createCellStyle();
        s.dataValueStyle.setFont(dataFont);
        s.dataValueStyle.setAlignment(HorizontalAlignment.LEFT);
        setBorders(s.dataValueStyle);

        // Data number
        s.dataNumberStyle = wb.createCellStyle();
        s.dataNumberStyle.setFont(dataFont);
        s.dataNumberStyle.setAlignment(HorizontalAlignment.RIGHT);
        setBorders(s.dataNumberStyle);

        return s;
    }

    private void setBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static class Styles {
        CellStyle titleStyle;
        CellStyle subtitleStyle;
        CellStyle sectionStyle;
        CellStyle headerStyle;
        CellStyle dataLabelStyle;
        CellStyle dataValueStyle;
        CellStyle dataNumberStyle;
    }
}
