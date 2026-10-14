package com.decisionhub.service.reports.generator;

import com.decisionhub.dto.response.analytics.*;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class CsvReportGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateDecisionCsv(Long decisionId,
                                      DecisionOverviewResponse overview,
                                      VoteStatisticsResponse voteStats,
                                      ParticipationResponse participation,
                                      DiscussionStatisticsResponse discussion,
                                      List<RankingResponse> ranking,
                                      DecisionFeedbackAnalyticsResponse feedback) {
        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM for Excel compatibility
        sb.append("\uFEFF");

        sb.append("=== DECISIONHUB ANALYTICS REPORT ===\n");
        sb.append("Report Type,Decision Analytics\n");
        sb.append("Generated At,").append(LocalDateTime.now().format(FORMATTER)).append("\n");
        sb.append("Decision ID,").append(decisionId).append("\n");
        sb.append("Decision Title,\"").append(escape(overview != null ? overview.getDecisionTitle() : "Decision #" + decisionId)).append("\"\n");
        sb.append("Decision Status,").append(overview != null ? overview.getDecisionStatus() : "N/A").append("\n");
        sb.append("Poll Status,").append(overview != null ? overview.getPollStatus() : "N/A").append("\n");
        sb.append("Deadline,").append(overview != null && overview.getDecisionDeadline() != null ? overview.getDecisionDeadline().format(FORMATTER) : "N/A").append("\n\n");

        sb.append("--- KEY METRICS ---\n");
        sb.append("Metric,Value\n");
        sb.append("Total Votes,").append(voteStats != null ? voteStats.getTotalVotes() : 0).append("\n");
        sb.append("Total Participants,").append(voteStats != null ? voteStats.getTotalParticipants() : 0).append("\n");
        sb.append("Turnout Percentage,").append(voteStats != null && voteStats.getVotePercentage() != null ? String.format("%.2f%%", voteStats.getVotePercentage()) : "N/A").append("\n");
        sb.append("Number of Options,").append(voteStats != null ? voteStats.getNumberOfOptions() : 0).append("\n");
        sb.append("Total Discussion Comments,").append(discussion != null ? discussion.getTotalComments() : 0).append("\n");
        sb.append("Total Replies,").append(discussion != null ? discussion.getTotalReplies() : 0).append("\n");
        sb.append("Feedback Submissions,").append(feedback != null && feedback.getFeedbackCount() != null ? feedback.getFeedbackCount() : 0).append("\n");
        sb.append("Average Rating,").append(feedback != null && feedback.getAverageRating() != null ? String.format("%.2f / 5.0", feedback.getAverageRating()) : "N/A").append("\n\n");

        sb.append("--- VOTING RANKING & DISTRIBUTION ---\n");
        sb.append("Rank,Option ID,Option Name,Votes,Percentage\n");
        if (ranking != null && !ranking.isEmpty()) {
            for (RankingResponse r : ranking) {
                sb.append(r.getRank() != null ? r.getRank() : "").append(",");
                sb.append(r.getOptionId() != null ? r.getOptionId() : "").append(",");
                sb.append("\"").append(escape(r.getOptionName())).append("\",");
                sb.append(r.getVoteCount() != null ? r.getVoteCount() : 0).append(",");
                sb.append(r.getPercentage() != null ? String.format("%.2f%%", r.getPercentage()) : "0.00%").append("\n");
            }
        } else {
            sb.append("No voting ranking records available.\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generateCommunityCsv(Long communityId,
                                       CommunityOverviewResponse overview,
                                       CommunityDecisionStatisticsResponse decisions,
                                       CommunityVotingStatisticsResponse voting,
                                       CommunityDiscussionStatisticsResponse discussion,
                                       CommunityActivityResponse activity,
                                       CommunityModerationAnalyticsResponse moderation) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");

        sb.append("=== DECISIONHUB ANALYTICS REPORT ===\n");
        sb.append("Report Type,Moderator Community Analytics\n");
        sb.append("Generated At,").append(LocalDateTime.now().format(FORMATTER)).append("\n");
        sb.append("Community ID,").append(communityId).append("\n");
        sb.append("Community Name,\"").append(escape(overview != null ? overview.getCommunityName() : "Community #" + communityId)).append("\"\n\n");

        sb.append("--- COMMUNITY OVERVIEW & ACTIVITY ---\n");
        sb.append("Metric,Value\n");
        sb.append("Registered Community Members,").append(overview != null ? overview.getTotalMembers() : 0).append("\n");
        sb.append("Total Decisions,").append(overview != null ? overview.getTotalDecisions() : (decisions != null ? decisions.getTotalDecisions() : 0)).append("\n");
        sb.append("Active Decisions,").append(overview != null ? overview.getActiveDecisions() : (decisions != null ? decisions.getActiveDecisions() : 0)).append("\n");
        sb.append("Closed Decisions,").append(overview != null ? overview.getClosedDecisions() : (decisions != null ? decisions.getClosedDecisions() : 0)).append("\n");
        sb.append("Total Community Votes,").append(voting != null ? voting.getTotalVotes() : 0).append("\n");
        sb.append("Total Community Discussions,").append(discussion != null ? discussion.getTotalComments() : 0).append("\n");
        sb.append("Total Discussion Replies,").append(discussion != null ? discussion.getTotalReplies() : 0).append("\n\n");

        sb.append("--- MODERATION & CONTROL METRICS ---\n");
        sb.append("Metric,Count\n");
        sb.append("Locked Discussions,").append(moderation != null ? moderation.getLockedDiscussions() : 0).append("\n");
        sb.append("Unlocked Discussions,").append(moderation != null ? moderation.getUnlockedDiscussions() : 0).append("\n");
        sb.append("Pinned Decisions,").append(moderation != null ? moderation.getPinnedDecisions() : 0).append("\n");
        sb.append("Reported Comments,").append(moderation != null ? moderation.getReportedComments() : 0).append("\n");
        sb.append("Removed Comments,").append(moderation != null ? moderation.getRemovedComments() : 0).append("\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generateAdminCsv(PlatformOverviewResponse dashboard,
                                   UserAnalyticsResponse users,
                                   CommunityAnalyticsResponse communities,
                                   AdminDecisionStatisticsResponse decisions,
                                   PlatformDiscussionAnalyticsResponse discussion,
                                   AdminFeedbackAnalyticsResponse feedback) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");

        sb.append("=== DECISIONHUB ANALYTICS REPORT ===\n");
        sb.append("Report Type,Platform-Wide Admin Analytics\n");
        sb.append("Generated At,").append(LocalDateTime.now().format(FORMATTER)).append("\n\n");

        sb.append("--- PLATFORM OVERVIEW ---\n");
        sb.append("Metric,Value\n");
        sb.append("Total Registered Users,").append(dashboard != null ? dashboard.getTotalUsers() : (users != null ? users.getTotalUsers() : 0)).append("\n");
        sb.append("Active Users,").append(users != null ? users.getActiveUsers() : 0).append("\n");
        sb.append("Inactive Users,").append(users != null ? users.getInactiveUsers() : 0).append("\n");
        sb.append("Suspended Users,").append(users != null ? users.getSuspendedUsers() : 0).append("\n");
        sb.append("Total Communities,").append(dashboard != null ? dashboard.getTotalCommunities() : (communities != null ? communities.getTotalCommunities() : 0)).append("\n");
        sb.append("Total Decisions,").append(dashboard != null ? dashboard.getTotalDecisions() : (decisions != null ? decisions.getTotalDecisions() : 0)).append("\n");
        sb.append("Active Decisions,").append(decisions != null ? decisions.getActiveDecisions() : 0).append("\n");
        sb.append("Closed Decisions,").append(decisions != null ? decisions.getClosedDecisions() : 0).append("\n");
        sb.append("Draft Decisions,").append(decisions != null ? decisions.getDraftDecisions() : 0).append("\n");
        sb.append("Public Decisions,").append(decisions != null ? decisions.getPublicDecisions() : 0).append("\n");
        sb.append("Community Decisions,").append(decisions != null ? decisions.getCommunityDecisions() : 0).append("\n");
        sb.append("Total Platform Votes,").append(dashboard != null ? dashboard.getTotalVotes() : 0).append("\n");
        sb.append("Total Platform Comments,").append(dashboard != null ? dashboard.getTotalComments() : (discussion != null ? discussion.getTotalComments() : 0)).append("\n");
        sb.append("Total Platform Replies,").append(dashboard != null ? dashboard.getTotalReplies() : (discussion != null ? discussion.getTotalReplies() : 0)).append("\n");
        sb.append("Average Comments Per Decision,").append(discussion != null && discussion.getAverageCommentsPerDecision() != null ? String.format("%.2f", discussion.getAverageCommentsPerDecision()) : "0").append("\n");
        sb.append("Total Feedback Submissions,").append(feedback != null && feedback.getTotalDecisionFeedback() != null ? feedback.getTotalDecisionFeedback() : 0).append("\n");
        sb.append("Average Platform Rating,").append(feedback != null && feedback.getAverageDecisionRating() != null ? String.format("%.2f / 5.0", feedback.getAverageDecisionRating()) : "N/A").append("\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("\"", "\"\"");
    }
}
