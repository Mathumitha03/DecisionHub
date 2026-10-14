import api from "./api";

// Object Export (for components like AdminAnalyticsView, DecisionAnalyticsView, etc.)
export const analyticsService = {
  // 1. DECISION ANALYTICS (USER/MODERATOR)
  getDecisionOverview: async (decisionId) => {
    const res = await api.get(`/analytics/decisions/${decisionId}/overview`);
    return res.data;
  },

  getVoteStatistics: async (decisionId) => {
    const res = await api.get(`/analytics/decisions/${decisionId}/votes`);
    return res.data;
  },

  getVoteDistribution: async (decisionId) => {
    const res = await api.get(`/analytics/decisions/${decisionId}/distribution`);
    return res.data;
  },

  getParticipation: async (decisionId) => {
    const res = await api.get(`/analytics/decisions/${decisionId}/participation`);
    return res.data;
  },

  getDiscussion: async (decisionId) => {
    const res = await api.get(`/analytics/decisions/${decisionId}/discussion`);
    return res.data;
  },

  getRanking: async (decisionId) => {
    const res = await api.get(`/analytics/decisions/${decisionId}/ranking`);
    return res.data;
  },

  getFeedback: async (decisionId) => {
    const res = await api.get(`/analytics/decisions/${decisionId}/feedback`);
    return res.data;
  },

  fetchAllDecisionAnalytics: async (decisionId) => {
    const [overview, voteStats, distribution, participation, discussion, ranking, feedback] = 
      await Promise.allSettled([
        analyticsService.getDecisionOverview(decisionId),
        analyticsService.getVoteStatistics(decisionId),
        analyticsService.getVoteDistribution(decisionId),
        analyticsService.getParticipation(decisionId),
        analyticsService.getDiscussion(decisionId),
        analyticsService.getRanking(decisionId),
        analyticsService.getFeedback(decisionId),
      ]);

    return {
      overview: overview.status === "fulfilled" ? overview.value : null,
      voteStats: voteStats.status === "fulfilled" ? voteStats.value : null,
      distribution: distribution.status === "fulfilled" ? distribution.value : [],
      participation: participation.status === "fulfilled" ? participation.value : null,
      discussion: discussion.status === "fulfilled" ? discussion.value : null,
      ranking: ranking.status === "fulfilled" ? ranking.value : [],
      feedback: feedback.status === "fulfilled" ? feedback.value : null,
    };
  },

  // 2. COMMUNITY ANALYTICS (MODERATOR)
  getCommunityOverview: async (communityId) => {
    const res = await api.get(`/analytics/communities/${communityId}/overview`);
    return res.data;
  },

  getCommunityDecisions: async (communityId) => {
    const res = await api.get(`/analytics/communities/${communityId}/decisions`);
    return res.data;
  },

  getCommunityVoting: async (communityId) => {
    const res = await api.get(`/analytics/communities/${communityId}/voting`);
    return res.data;
  },

  getCommunityDiscussion: async (communityId) => {
    const res = await api.get(`/analytics/communities/${communityId}/discussion`);
    return res.data;
  },

  getCommunityActivity: async (communityId) => {
    const res = await api.get(`/analytics/communities/${communityId}/activity`);
    return res.data;
  },

  getCommunityModeration: async (communityId) => {
    const res = await api.get(`/analytics/communities/${communityId}/moderation`);
    return res.data;
  },

  fetchAllCommunityAnalytics: async (communityId) => {
    const [overview, decisions, voting, discussion, activity, moderation] = 
      await Promise.allSettled([
        analyticsService.getCommunityOverview(communityId),
        analyticsService.getCommunityDecisions(communityId),
        analyticsService.getCommunityVoting(communityId),
        analyticsService.getCommunityDiscussion(communityId),
        analyticsService.getCommunityActivity(communityId),
        analyticsService.getCommunityModeration(communityId),
      ]);

    return {
      overview: overview.status === "fulfilled" ? overview.value : null,
      decisions: decisions.status === "fulfilled" ? decisions.value : null,
      voting: voting.status === "fulfilled" ? voting.value : null,
      discussion: discussion.status === "fulfilled" ? discussion.value : null,
      activity: activity.status === "fulfilled" ? activity.value : null,
      moderation: moderation.status === "fulfilled" ? moderation.value : null,
    };
  },

  // 3. ADMIN ANALYTICS (PLATFORM ADMIN)
  getAdminDashboard: async () => {
    const res = await api.get("/analytics/admin/dashboard");
    return res.data;
  },

  getAdminUsers: async () => {
    const res = await api.get("/analytics/admin/users");
    return res.data;
  },

  getAdminCommunities: async () => {
    const res = await api.get("/analytics/admin/communities");
    return res.data;
  },

  getAdminDecisions: async () => {
    const res = await api.get("/analytics/admin/decisions");
    return res.data;
  },

  getAdminDiscussion: async () => {
    const res = await api.get("/analytics/admin/discussion");
    return res.data;
  },

  getAdminFeedback: async () => {
    const res = await api.get("/analytics/admin/feedback");
    return res.data;
  },

  getAdminUsersList: async (page = 0, size = 10) => {
    const res = await api.get(`/analytics/admin/users-list?page=${page}&size=${size}`);
    return res.data;
  },

  fetchAllAdminAnalytics: async () => {
    const [dashboard, users, communities, decisions, discussion, feedback, usersList] = 
      await Promise.allSettled([
        analyticsService.getAdminDashboard(),
        analyticsService.getAdminUsers(),
        analyticsService.getAdminCommunities(),
        analyticsService.getAdminDecisions(),
        analyticsService.getAdminDiscussion(),
        analyticsService.getAdminFeedback(),
        analyticsService.getAdminUsersList(0, 5),
      ]);

    return {
      dashboard: dashboard.status === "fulfilled" ? dashboard.value : null,
      users: users.status === "fulfilled" ? users.value : null,
      communities: communities.status === "fulfilled" ? communities.value : null,
      decisions: decisions.status === "fulfilled" ? decisions.value : null,
      discussion: discussion.status === "fulfilled" ? discussion.value : null,
      feedback: feedback.status === "fulfilled" ? feedback.value : null,
      usersList: usersList.status === "fulfilled" ? usersList.value : { content: [] },
    };
  },

  // 4. REPORT EXPORT (PDF, EXCEL, CSV)
  exportDecision: async (decisionId, format) => {
    return await api.get(`/analytics/decisions/${decisionId}/export`, {
      params: { format },
      responseType: "blob"
    });
  },

  exportCommunity: async (communityId, format) => {
    return await api.get(`/analytics/communities/${communityId}/export`, {
      params: { format },
      responseType: "blob"
    });
  },

  exportAdmin: async (format) => {
    return await api.get(`/analytics/admin/export`, {
      params: { format },
      responseType: "blob"
    });
  },

  exportAnalytics: async (type, id, format) => {
    return await api.get(`/analytics/export`, {
      params: { type, id, format },
      responseType: "blob"
    });
  }
};


// Named Exports (for Sriram's pages)
export const getUserAnalyticsOverview = async () => {
  const response = await api.get("/analytics/overview");
  return response.data;
};

export const getUserDecisionStats = async () => {
  const response = await api.get("/analytics/decisions/statistics");
  return response.data;
};

export const getAdminPlatformOverview = async () => {
  const response = await api.get("/analytics/admin/dashboard");
  return response.data;
};

export const getAdminDecisionStats = async () => {
  const response = await api.get("/analytics/admin/decisions");
  return response.data;
};

export const getAdminUserStats = async () => {
  const response = await api.get("/analytics/admin/users");
  return response.data;
};

export const getAdminCommunityStats = async () => {
  const response = await api.get("/analytics/admin/communities");
  return response.data;
};

export const getAdminUsersList = async (page = 0, size = 10) => {
  const response = await api.get(`/analytics/admin/users-list?page=${page}&size=${size}`);
  return response.data;
};

// Helper function to trigger browser download from a binary Axios response
export const triggerBlobDownload = (response, defaultFilename = "analytics_report.pdf") => {
  let filename = defaultFilename;
  const disposition = response.headers?.["content-disposition"] || response.headers?.get?.("content-disposition");
  if (disposition && disposition.indexOf("filename=") !== -1) {
    const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(disposition);
    if (matches != null && matches[1]) {
      filename = matches[1].replace(/['"]/g, "").trim();
    }
  }

  const contentType = response.headers?.["content-type"] || "application/octet-stream";
  const blob = new Blob([response.data], { type: contentType });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => window.URL.revokeObjectURL(url), 1000);
};

export const exportDecisionAnalytics = async (decisionId, format) => {
  try {
    const res = await analyticsService.exportDecision(decisionId, format);
    triggerBlobDownload(res, `DecisionHub_Decision_Analytics_${decisionId}.${format.toLowerCase() === "excel" ? "xlsx" : format.toLowerCase()}`);
    return res;
  } catch (backendErr) {
    console.warn("Backend decision export failed, generating rich export on client:", backendErr);
    const data = await analyticsService.fetchAllDecisionAnalytics(decisionId);
    generateClientExport("decision", decisionId, data, format);
  }
};

export const exportCommunityAnalytics = async (communityId, format) => {
  try {
    const res = await analyticsService.exportCommunity(communityId, format);
    triggerBlobDownload(res, `DecisionHub_Community_Analytics_${communityId}.${format.toLowerCase() === "excel" ? "xlsx" : format.toLowerCase()}`);
    return res;
  } catch (backendErr) {
    console.warn("Backend community export failed, generating rich export on client:", backendErr);
    const data = await analyticsService.fetchAllCommunityAnalytics(communityId);
    generateClientExport("community", communityId, data, format);
  }
};

export const exportAdminAnalytics = async (format) => {
  try {
    const res = await analyticsService.exportAdmin(format);
    triggerBlobDownload(res, `DecisionHub_Platform_Analytics.${format.toLowerCase() === "excel" ? "xlsx" : format.toLowerCase()}`);
    return res;
  } catch (backendErr) {
    console.warn("Backend admin export failed, generating rich export on client:", backendErr);
    const data = await analyticsService.fetchAllAdminAnalytics();
    generateClientExport("admin", "platform", data, format);
  }
};

const downloadClientBlob = (content, filename, mimeType) => {
  const blob = new Blob([content], { type: mimeType });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => window.URL.revokeObjectURL(url), 1000);
};

export const generateClientExport = (type, id, data, format) => {
  const isExcel = format.toLowerCase() === "excel" || format.toLowerCase() === "xlsx";
  const isPdf = format.toLowerCase() === "pdf";

  let filename = "";
  let content = "";
  let mimeType = "text/plain";

  if (type === "community") {
    const { overview, decisions, voting, discussion, moderation } = data || {};
    filename = `DecisionHub_Community_Analytics_${id}.${isExcel ? "xls" : isPdf ? "txt" : "csv"}`;
    const commName = overview?.communityName || `Community #${id}`;

    if (isExcel) {
      mimeType = "application/vnd.ms-excel;charset=utf-8";
      content = `
        <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">
        <head><meta charset="utf-8"></head>
        <body>
          <h2>DecisionHub Analytics Report - ${commName}</h2>
          <p>Generated: ${new Date().toLocaleString()}</p>
          <table border="1">
            <tr style="background:#4F46E5;color:#ffffff;"><th>Metric</th><th>Value</th></tr>
            <tr><td>Community ID</td><td>${id}</td></tr>
            <tr><td>Community Name</td><td>${commName}</td></tr>
            <tr><td>Registered Members</td><td>${overview?.totalMembers ?? overview?.memberCount ?? 0}</td></tr>
            <tr><td>Total Decisions</td><td>${decisions?.totalDecisions ?? overview?.totalDecisions ?? 0}</td></tr>
            <tr><td>Active Decisions</td><td>${decisions?.activeDecisions ?? overview?.activeDecisions ?? 0}</td></tr>
            <tr><td>Closed Decisions</td><td>${decisions?.closedDecisions ?? overview?.closedDecisions ?? 0}</td></tr>
            <tr><td>Total Community Votes</td><td>${voting?.totalVotes ?? 0}</td></tr>
            <tr><td>Total Discussions</td><td>${discussion?.totalComments ?? 0}</td></tr>
            <tr><td>Discussion Replies</td><td>${discussion?.totalReplies ?? 0}</td></tr>
            <tr><td>Locked Discussions</td><td>${moderation?.lockedDiscussions ?? 0}</td></tr>
            <tr><td>Unlocked Discussions</td><td>${moderation?.unlockedDiscussions ?? 0}</td></tr>
            <tr><td>Pinned Decisions</td><td>${moderation?.pinnedDecisions ?? 0}</td></tr>
            <tr><td>Reported Comments</td><td>${moderation?.reportedComments ?? 0}</td></tr>
            <tr><td>Removed Comments</td><td>${moderation?.removedComments ?? 0}</td></tr>
          </table>
        </body>
        </html>
      `;
    } else if (isPdf) {
      mimeType = "text/plain;charset=utf-8";
      content = `========================================================\n`
        + `        DECISIONHUB COMMUNITY ANALYTICS REPORT        \n`
        + `========================================================\n`
        + `Community: ${commName} (ID: ${id})\n`
        + `Generated: ${new Date().toLocaleString()}\n\n`
        + `[COMMUNITY OVERVIEW]\n`
        + `• Registered Members: ${overview?.totalMembers ?? overview?.memberCount ?? 0}\n`
        + `• Total Decisions: ${decisions?.totalDecisions ?? overview?.totalDecisions ?? 0}\n`
        + `• Active Decisions: ${decisions?.activeDecisions ?? overview?.activeDecisions ?? 0}\n`
        + `• Closed Decisions: ${decisions?.closedDecisions ?? overview?.closedDecisions ?? 0}\n`
        + `• Total Community Votes: ${voting?.totalVotes ?? 0}\n\n`
        + `[DISCUSSIONS & MODERATION]\n`
        + `• Total Discussion Comments: ${discussion?.totalComments ?? 0}\n`
        + `• Total Discussion Replies: ${discussion?.totalReplies ?? 0}\n`
        + `• Locked Discussions: ${moderation?.lockedDiscussions ?? 0}\n`
        + `• Pinned Decisions: ${moderation?.pinnedDecisions ?? 0}\n`
        + `• Reported Comments: ${moderation?.reportedComments ?? 0}\n`
        + `• Removed Comments: ${moderation?.removedComments ?? 0}\n`
        + `========================================================\n`;
    } else {
      mimeType = "text/csv;charset=utf-8;";
      content = "\uFEFF"
        + "Metric,Value\n"
        + `Community ID,${id}\n`
        + `Community Name,"${commName.replace(/"/g, '""')}"\n`
        + `Registered Members,${overview?.totalMembers ?? overview?.memberCount ?? 0}\n`
        + `Total Decisions,${decisions?.totalDecisions ?? overview?.totalDecisions ?? 0}\n`
        + `Active Decisions,${decisions?.activeDecisions ?? overview?.activeDecisions ?? 0}\n`
        + `Closed Decisions,${decisions?.closedDecisions ?? overview?.closedDecisions ?? 0}\n`
        + `Total Community Votes,${voting?.totalVotes ?? 0}\n`
        + `Total Discussions,${discussion?.totalComments ?? 0}\n`
        + `Total Discussion Replies,${discussion?.totalReplies ?? 0}\n`
        + `Locked Discussions,${moderation?.lockedDiscussions ?? 0}\n`
        + `Pinned Decisions,${moderation?.pinnedDecisions ?? 0}\n`
        + `Reported Comments,${moderation?.reportedComments ?? 0}\n`
        + `Removed Comments,${moderation?.removedComments ?? 0}\n`;
    }
  } else if (type === "decision") {
    const { overview, voteStats, discussion, ranking, feedback } = data || {};
    filename = `DecisionHub_Decision_Analytics_${id}.${isExcel ? "xls" : isPdf ? "txt" : "csv"}`;
    const decTitle = overview?.decisionTitle || overview?.title || `Decision #${id}`;

    if (isExcel) {
      mimeType = "application/vnd.ms-excel;charset=utf-8";
      let rankRows = "";
      if (ranking && ranking.length > 0) {
        rankRows = ranking.map(r => `<tr><td>${r.rank ?? "-"}</td><td>${r.optionName ?? ""}</td><td>${r.voteCount ?? 0}</td><td>${r.percentage != null ? r.percentage.toFixed(1) + "%" : "0%"}</td></tr>`).join("");
      }
      content = `
        <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">
        <head><meta charset="utf-8"></head>
        <body>
          <h2>DecisionHub Analytics Report - ${decTitle}</h2>
          <p>Generated: ${new Date().toLocaleString()}</p>
          <table border="1">
            <tr style="background:#4F46E5;color:#ffffff;"><th>Metric</th><th>Value</th></tr>
            <tr><td>Decision ID</td><td>${id}</td></tr>
            <tr><td>Decision Title</td><td>${decTitle}</td></tr>
            <tr><td>Status</td><td>${overview?.decisionStatus ?? overview?.status ?? "CLOSED"}</td></tr>
            <tr><td>Total Votes</td><td>${voteStats?.totalVotes ?? 0}</td></tr>
            <tr><td>Total Participants</td><td>${voteStats?.totalParticipants ?? 0}</td></tr>
            <tr><td>Turnout Percentage</td><td>${voteStats?.votePercentage != null ? voteStats.votePercentage.toFixed(1) + "%" : "N/A"}</td></tr>
            <tr><td>Total Comments</td><td>${discussion?.totalComments ?? 0}</td></tr>
            <tr><td>Total Replies</td><td>${discussion?.totalReplies ?? 0}</td></tr>
            <tr><td>Feedback Submissions</td><td>${feedback?.feedbackCount ?? 0}</td></tr>
            <tr><td>Average Rating</td><td>${feedback?.averageRating != null ? feedback.averageRating.toFixed(2) + " / 5.0" : "N/A"}</td></tr>
          </table>
          <br/>
          <h3>Voting Distribution & Ranking</h3>
          <table border="1">
            <tr style="background:#4F46E5;color:#ffffff;"><th>Rank</th><th>Option Name</th><th>Votes</th><th>Percentage</th></tr>
            ${rankRows}
          </table>
        </body>
        </html>
      `;
    } else if (isPdf) {
      mimeType = "text/plain;charset=utf-8";
      let rankText = "";
      if (ranking && ranking.length > 0) {
        rankText = ranking.map(r => `  #${r.rank ?? "-"}: ${r.optionName} — ${r.voteCount ?? 0} votes (${r.percentage != null ? r.percentage.toFixed(1) : 0}%)\n`).join("");
      }
      content = `========================================================\n`
        + `        DECISIONHUB DECISION ANALYTICS REPORT         \n`
        + `========================================================\n`
        + `Title: ${decTitle} (ID: ${id})\n`
        + `Status: ${overview?.decisionStatus ?? overview?.status ?? "CLOSED"}\n`
        + `Generated: ${new Date().toLocaleString()}\n\n`
        + `[KEY METRICS]\n`
        + `• Total Votes: ${voteStats?.totalVotes ?? 0}\n`
        + `• Total Participants: ${voteStats?.totalParticipants ?? 0}\n`
        + `• Turnout: ${voteStats?.votePercentage != null ? voteStats.votePercentage.toFixed(1) + "%" : "N/A"}\n`
        + `• Discussion Comments: ${discussion?.totalComments ?? 0}\n`
        + `• Replies: ${discussion?.totalReplies ?? 0}\n`
        + `• Feedback Rating: ${feedback?.averageRating != null ? feedback.averageRating.toFixed(2) + " / 5.0" : "N/A"}\n\n`
        + `[VOTING RANKING & DISTRIBUTION]\n`
        + (rankText || "  No voting records available.\n")
        + `========================================================\n`;
    } else {
      mimeType = "text/csv;charset=utf-8;";
      let rankCsv = "";
      if (ranking && ranking.length > 0) {
        rankCsv = "\nRank,Option Name,Votes,Percentage\n" + ranking.map(r => `${r.rank ?? ""},"${(r.optionName || "").replace(/"/g, '""')}",${r.voteCount ?? 0},${r.percentage != null ? r.percentage.toFixed(2) + "%" : "0.00%"}`).join("\n");
      }
      content = "\uFEFF"
        + "Metric,Value\n"
        + `Decision ID,${id}\n`
        + `Decision Title,"${decTitle.replace(/"/g, '""')}"\n`
        + `Status,${overview?.decisionStatus ?? overview?.status ?? "CLOSED"}\n`
        + `Total Votes,${voteStats?.totalVotes ?? 0}\n`
        + `Total Participants,${voteStats?.totalParticipants ?? 0}\n`
        + `Turnout,${voteStats?.votePercentage != null ? voteStats.votePercentage.toFixed(2) + "%" : "N/A"}\n`
        + `Comments,${discussion?.totalComments ?? 0}\n`
        + `Replies,${discussion?.totalReplies ?? 0}\n`
        + `Average Rating,${feedback?.averageRating != null ? feedback.averageRating.toFixed(2) : "N/A"}\n`
        + rankCsv;
    }
  } else {
    // Admin platform
    const { dashboard, users, communities, decisions, discussion } = data || {};
    filename = `DecisionHub_Platform_Analytics.${isExcel ? "xls" : isPdf ? "txt" : "csv"}`;

    if (isExcel) {
      mimeType = "application/vnd.ms-excel;charset=utf-8";
      content = `
        <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">
        <head><meta charset="utf-8"></head>
        <body>
          <h2>DecisionHub Analytics Report - Platform Overview</h2>
          <p>Generated: ${new Date().toLocaleString()}</p>
          <table border="1">
            <tr style="background:#4F46E5;color:#ffffff;"><th>Metric</th><th>Value</th></tr>
            <tr><td>Total Users</td><td>${dashboard?.totalUsers ?? users?.totalUsers ?? 0}</td></tr>
            <tr><td>Total Communities</td><td>${dashboard?.totalCommunities ?? communities?.totalCommunities ?? 0}</td></tr>
            <tr><td>Total Decisions</td><td>${dashboard?.totalDecisions ?? decisions?.totalDecisions ?? 0}</td></tr>
            <tr><td>Total Votes</td><td>${dashboard?.totalVotes ?? 0}</td></tr>
            <tr><td>Total Comments</td><td>${dashboard?.totalComments ?? discussion?.totalComments ?? 0}</td></tr>
            <tr><td>Active Decisions</td><td>${decisions?.activeDecisions ?? 0}</td></tr>
            <tr><td>Closed Decisions</td><td>${decisions?.closedDecisions ?? 0}</td></tr>
          </table>
        </body>
        </html>
      `;
    } else {
      mimeType = isPdf ? "text/plain;charset=utf-8" : "text/csv;charset=utf-8;";
      content = "\uFEFF"
        + "Metric,Value\n"
        + `Total Users,${dashboard?.totalUsers ?? users?.totalUsers ?? 0}\n`
        + `Total Communities,${dashboard?.totalCommunities ?? communities?.totalCommunities ?? 0}\n`
        + `Total Decisions,${dashboard?.totalDecisions ?? decisions?.totalDecisions ?? 0}\n`
        + `Total Votes,${dashboard?.totalVotes ?? 0}\n`
        + `Total Comments,${dashboard?.totalComments ?? discussion?.totalComments ?? 0}\n`
        + `Active Decisions,${decisions?.activeDecisions ?? 0}\n`
        + `Closed Decisions,${decisions?.closedDecisions ?? 0}\n`;
    }
  }

  downloadClientBlob(content, filename, mimeType);
};

