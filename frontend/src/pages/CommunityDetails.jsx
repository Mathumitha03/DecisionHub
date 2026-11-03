import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import api from "../services/api";
import { reportCommunity } from "../services/communityService";
import "../styles/CommunityDetail.css";
import Sidebar from "../components/Sidebar";
import Navbar from "../components/Navbar";
import ModeratorPanel from "../components/ModeratorPanel";
import { FaThumbtack, FaLock, FaArrowRight, FaExclamationTriangle, FaTimes } from "react-icons/fa";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../components/Toast";
import AdminCommunityWorkspace from "./AdminCommunityWorkspace";

export default function CommunityDetail() {
  const { user } = useAuth();
  const { addToast } = useToast();
  const { id: communityId } = useParams();

  if (user?.role === "ADMIN") {
    return <AdminCommunityWorkspace />;
  }

  const navigate = useNavigate();
  const [community, setCommunity] = useState(null);
  const [decisions, setDecisions] = useState([]);
  const [decisionsLoading, setDecisionsLoading] = useState(true);
  const [loading, setLoading] = useState(true);

  // Community Report Modal States
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportReason, setReportReason] = useState("");
  const [submittingReport, setSubmittingReport] = useState(false);

  useEffect(() => {
    fetchCommunity();
    fetchCommunityDecisions();
  }, [communityId]);

  const fetchCommunity = async () => {
    try {
      const res = await api.get(`/communities/${communityId}`);
      setCommunity(res.data);
    } catch (err) {
      console.error("Failed to fetch community:", err);
    } finally {
      setLoading(false);
    }
  };

  const fetchCommunityDecisions = async () => {
    try {
      setDecisionsLoading(true);
      const res = await api.get(`/decisions?communityId=${communityId}`);
      setDecisions(res.data || []);
    } catch (err) {
      console.error("Failed to fetch community decisions:", err);
    } finally {
      setDecisionsLoading(false);
    }
  };

  const handleReportCommunity = async (e) => {
    e.preventDefault();
    if (!reportReason.trim()) {
      addToast("Please provide a reason for reporting", "error");
      return;
    }

    try {
      setSubmittingReport(true);
      await reportCommunity(communityId, reportReason.trim());
      addToast("Community reported successfully. Platform administrators have been notified.", "success");
      setShowReportModal(false);
      setReportReason("");
    } catch (err) {
      console.error("Failed to report community:", err);
      addToast(
        err.response?.data?.message ||
        err.response?.data?.error ||
        "Failed to report community",
        "error"
      );
    } finally {
      setSubmittingReport(false);
    }
  };

  const isModerator = community && String(community.ownerId) === String(user?.id);

  return (
    <div className="dashboard">
      <Sidebar />
      <div className="dashboard-main">
        <Navbar />
        <div className="dashboard-content animate-fade-in">
          {loading && <p>Loading community...</p>}
          {!loading && !community && <p>Community not found.</p>}

          {!loading && community && (
            <div className="community-detail-page">
              {/* Header Section */}
              <div className="community-detail-header">
                <div className="community-detail-header-info">
                  {community.categoryName && (
                    <span className="category-tag" style={{ marginBottom: "0.5rem", display: "inline-block" }}>
                      {community.categoryName}
                    </span>
                  )}
                  <h2>{community.name}</h2>
                  <p>{community.description || "No description available."}</p>
                </div>

                <div className="community-detail-header-actions">
                  {community.isMember && (
                    <button
                      className="btn-report-community"
                      onClick={() => setShowReportModal(true)}
                      title="Report inappropriate actions in this community"
                    >
                      <FaExclamationTriangle /> Report Community
                    </button>
                  )}
                </div>
              </div>

              {/* Decisions Section */}
              <section className="community-decisions-section">
                <h3>Decisions in this Community</h3>
                {decisionsLoading ? (
                  <p>Loading decisions...</p>
                ) : decisions.length === 0 ? (
                  <p className="empty-community-decisions">
                    No decisions have been created in this community yet.
                  </p>
                ) : (
                  <ul className="community-decisions-list">
                    {[...decisions]
                      .sort((a, b) => (a.pinned && !b.pinned ? -1 : !a.pinned && b.pinned ? 1 : 0))
                      .map((d) => (
                        <li key={d.id} className="community-decision-row">
                          <div className="community-decision-info">
                            <span className="community-decision-title">
                              {d.pinned && <FaThumbtack title="Pinned" style={{ marginRight: 6 }} />}
                              {d.locked && <FaLock title="Locked" style={{ marginRight: 6 }} />}
                              {d.title}
                            </span>
                            <span className={`status-badge ${(d.status || "").toLowerCase()}`}>
                              {d.status}
                            </span>
                          </div>
                          <button
                            className="community-decision-view-btn"
                            onClick={() => navigate(`/decisions/${d.id}`)}
                          >
                            View <FaArrowRight />
                          </button>
                        </li>
                      ))}
                  </ul>
                )}
              </section>

              {isModerator && <ModeratorPanel communityId={community.id} />}

              {/* Report Community Modal */}
              {showReportModal && (
                <div className="report-modal-overlay">
                  <div className="report-modal-content glass-panel animate-pop-in">
                    <div className="report-modal-header">
                      <h3>
                        <FaExclamationTriangle style={{ color: "var(--danger, #ef4444)" }} />
                        Report Community
                      </h3>
                      <button
                        type="button"
                        className="report-modal-close-btn"
                        onClick={() => {
                          setShowReportModal(false);
                          setReportReason("");
                        }}
                      >
                        <FaTimes />
                      </button>
                    </div>

                    <form onSubmit={handleReportCommunity} className="report-modal-body">
                      <p>
                        Are you experiencing or observing inappropriate actions in <strong>{community.name}</strong>?
                        Please specify the reason below. Platform administrators will review the report and take appropriate action.
                      </p>

                      <textarea
                        className="report-textarea"
                        placeholder="Describe the inappropriate actions or reasons for reporting..."
                        value={reportReason}
                        onChange={(e) => setReportReason(e.target.value)}
                        required
                        rows={4}
                      />

                      <div className="report-modal-actions">
                        <button
                          type="button"
                          className="btn-secondary"
                          onClick={() => {
                            setShowReportModal(false);
                            setReportReason("");
                          }}
                          disabled={submittingReport}
                        >
                          Cancel
                        </button>
                        <button
                          type="submit"
                          className="btn-primary"
                          style={{ background: "#EF4444" }}
                          disabled={submittingReport}
                        >
                          {submittingReport ? "Submitting..." : "Submit Report"}
                        </button>
                      </div>
                    </form>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
