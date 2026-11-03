import { useState, useEffect, useMemo } from "react";
import Sidebar from "../components/Sidebar";
import Navbar from "../components/Navbar";
import { useToast } from "../components/Toast";
import api from "../services/api";
import {
  FaUserSlash,
  FaUnlock,
  FaSearch,
  FaUser,
  FaExclamationTriangle,
  FaUserShield,
  FaBan,
} from "react-icons/fa";

export default function BlockedUsers() {
  const { addToast } = useToast();

  const [blockedUsers, setBlockedUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");

  // Unblock modal state
  const [userToUnblock, setUserToUnblock] = useState(null);
  const [unblocking, setUnblocking] = useState(false);

  // Manual block modal state (allows Admin to block a user by ID)
  const [showBlockModal, setShowBlockModal] = useState(false);
  const [blockUserId, setBlockUserId] = useState("");
  const [blockReason, setBlockReason] = useState("");
  const [blocking, setBlocking] = useState(false);

  const fetchBlockedUsers = async () => {
    try {
      setLoading(true);
      const res = await api.get("/admin/users/blocked");
      setBlockedUsers(res.data || []);
    } catch (err) {
      console.error("Failed to fetch blocked users:", err);
      addToast(
        err.response?.data?.error || "Failed to load blocked users.",
        "error"
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBlockedUsers();
  }, []);

  const handleConfirmUnblock = async () => {
    if (!userToUnblock) return;
    setUnblocking(true);

    try {
      await api.put(`/admin/users/${userToUnblock.id}/unblock`);
      addToast("User has been unblocked successfully.", "success");
      setBlockedUsers((prev) => prev.filter((u) => u.id !== userToUnblock.id));
      setUserToUnblock(null);
    } catch (err) {
      console.error("Failed to unblock user:", err);
      addToast(
        err.response?.data?.error || "Failed to unblock user. Please try again.",
        "error"
      );
    } finally {
      setUnblocking(false);
    }
  };

  const handleBlockUser = async (e) => {
    e.preventDefault();
    if (!blockUserId.trim()) {
      addToast("Please enter a valid User ID.", "error");
      return;
    }

    setBlocking(true);
    try {
      await api.put(`/admin/users/${blockUserId.trim()}/block`, {
        reason: blockReason.trim() || "Account blocked by administrator",
      });
      addToast("User blocked successfully.", "success");
      setShowBlockModal(false);
      setBlockUserId("");
      setBlockReason("");
      fetchBlockedUsers();
    } catch (err) {
      console.error("Failed to block user:", err);
      addToast(
        err.response?.data?.error || "Failed to block user. Check the user ID.",
        "error"
      );
    } finally {
      setBlocking(false);
    }
  };

  const filteredUsers = useMemo(() => {
    const q = searchTerm.toLowerCase().trim();
    if (!q) return blockedUsers;
    return blockedUsers.filter(
      (u) =>
        (u.username && u.username.toLowerCase().includes(q)) ||
        (u.email && u.email.toLowerCase().includes(q)) ||
        (u.id && String(u.id).includes(q))
    );
  }, [blockedUsers, searchTerm]);

  return (
    <div className="dashboard">
      <Sidebar />
      <div className="dashboard-main">
        <Navbar />
        <div className="dashboard-content animate-fade-in">
          <div style={{ maxWidth: "1200px", margin: "0 auto", width: "100%" }}>
            {/* Header */}
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                flexWrap: "wrap",
                gap: "1rem",
                marginBottom: "2rem",
              }}
            >
              <div>
                <h1
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "10px",
                    fontSize: "1.8rem",
                    fontWeight: 700,
                    margin: 0,
                  }}
                >
                  <FaUserSlash style={{ color: "#ef4444" }} /> Blocked Users
                </h1>
                <p
                  style={{
                    color: "var(--text-secondary)",
                    marginTop: "0.25rem",
                    marginBottom: 0,
                  }}
                >
                  Manage blocked accounts and restore platform access.
                </p>
              </div>

              <div style={{ display: "flex", gap: "10px" }}>
                <button
                  type="button"
                  className="btn-primary"
                  onClick={() => setShowBlockModal(true)}
                  style={{
                    backgroundColor: "#ef4444",
                    borderColor: "#ef4444",
                    display: "inline-flex",
                    alignItems: "center",
                    gap: "8px",
                    padding: "8px 16px",
                  }}
                >
                  <FaBan /> Block a User
                </button>
              </div>
            </div>

            {/* Search and Filter Bar */}
            <div
              className="glass-panel"
              style={{
                padding: "1rem 1.25rem",
                marginBottom: "1.5rem",
                display: "flex",
                alignItems: "center",
                gap: "12px",
              }}
            >
              <FaSearch style={{ color: "var(--text-secondary)", fontSize: "1rem" }} />
              <input
                type="text"
                placeholder="Search by name, email, or user ID..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{
                  background: "transparent",
                  border: "none",
                  outline: "none",
                  color: "var(--text-primary)",
                  width: "100%",
                  fontSize: "0.95rem",
                }}
              />
              {searchTerm && (
                <button
                  type="button"
                  onClick={() => setSearchTerm("")}
                  style={{
                    background: "none",
                    border: "none",
                    color: "var(--text-secondary)",
                    cursor: "pointer",
                    fontSize: "0.85rem",
                  }}
                >
                  Clear
                </button>
              )}
            </div>

            {/* Blocked Users Table */}
            <div
              className="decision-table-wrapper glass-panel"
              style={{ overflowX: "auto", borderRadius: "12px" }}
            >
              {loading ? (
                <div style={{ padding: "40px", textAlign: "center", color: "var(--text-secondary)" }}>
                  <div
                    style={{
                      width: "36px",
                      height: "36px",
                      border: "3px solid rgba(255, 255, 255, 0.15)",
                      borderTopColor: "var(--accent-purple)",
                      borderRadius: "50%",
                      animation: "spin 1s linear infinite",
                      margin: "0 auto 12px",
                    }}
                  />
                  Loading blocked users...
                </div>
              ) : filteredUsers.length === 0 ? (
                <div style={{ padding: "48px 24px", textAlign: "center" }}>
                  <FaUserSlash
                    style={{
                      fontSize: "3rem",
                      color: "rgba(239, 68, 68, 0.4)",
                      marginBottom: "12px",
                    }}
                  />
                  <h3 style={{ margin: "0 0 6px", fontSize: "1.2rem" }}>
                    No blocked users found
                  </h3>
                  <p style={{ color: "var(--text-secondary)", margin: 0, fontSize: "0.9rem" }}>
                    {searchTerm
                      ? "No blocked users matched your search criteria."
                      : "There are currently no blocked users on DecisionHub."}
                  </p>
                </div>
              ) : (
                <table className="decision-table-element" style={{ width: "100%", minWidth: "800px" }}>
                  <thead>
                    <tr>
                      <th style={{ padding: "14px 18px", textAlign: "left" }}>User ID</th>
                      <th style={{ padding: "14px 18px", textAlign: "left" }}>Profile</th>
                      <th style={{ padding: "14px 18px", textAlign: "left" }}>Name</th>
                      <th style={{ padding: "14px 18px", textAlign: "left" }}>Email</th>
                      <th style={{ padding: "14px 18px", textAlign: "left" }}>Blocked Date</th>
                      <th style={{ padding: "14px 18px", textAlign: "center" }}>Status</th>
                      <th style={{ padding: "14px 18px", textAlign: "center" }}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredUsers.map((u) => {
                      const formattedDate = u.blockedAt
                        ? new Date(u.blockedAt).toLocaleString()
                        : "—";

                      return (
                        <tr key={u.id} style={{ borderBottom: "1px solid rgba(255, 255, 255, 0.05)" }}>
                          <td style={{ padding: "14px 18px", fontWeight: 600 }}>
                            #{u.id}
                          </td>
                          <td style={{ padding: "14px 18px" }}>
                            {u.avatarUrl ? (
                              <img
                                src={u.avatarUrl}
                                alt={u.username || "User"}
                                style={{
                                  width: "36px",
                                  height: "36px",
                                  borderRadius: "50%",
                                  objectFit: "cover",
                                  border: "1px solid rgba(255, 255, 255, 0.2)",
                                }}
                              />
                            ) : (
                              <div
                                style={{
                                  width: "36px",
                                  height: "36px",
                                  borderRadius: "50%",
                                  background: "rgba(239, 68, 68, 0.2)",
                                  display: "flex",
                                  alignItems: "center",
                                  justifyContent: "center",
                                  color: "#ef4444",
                                  fontWeight: 600,
                                  fontSize: "0.9rem",
                                }}
                              >
                                {u.username ? u.username.charAt(0).toUpperCase() : <FaUser />}
                              </div>
                            )}
                          </td>
                          <td style={{ padding: "14px 18px", fontWeight: 500 }}>
                            {u.username || "—"}
                          </td>
                          <td style={{ padding: "14px 18px", color: "var(--text-secondary)" }}>
                            {u.email}
                          </td>
                          <td style={{ padding: "14px 18px", fontSize: "0.85rem", color: "var(--text-secondary)" }}>
                            {formattedDate}
                          </td>
                          <td style={{ padding: "14px 18px", textAlign: "center" }}>
                            <span
                              style={{
                                display: "inline-block",
                                backgroundColor: "rgba(239, 68, 68, 0.2)",
                                color: "#ef4444",
                                border: "1px solid rgba(239, 68, 68, 0.4)",
                                padding: "4px 10px",
                                borderRadius: "12px",
                                fontSize: "0.75rem",
                                fontWeight: 700,
                                letterSpacing: "0.5px",
                              }}
                            >
                              {u.status || "BLOCKED"}
                            </span>
                          </td>
                          <td style={{ padding: "14px 18px", textAlign: "center" }}>
                            <button
                              type="button"
                              className="btn-secondary"
                              onClick={() => setUserToUnblock(u)}
                              style={{
                                padding: "6px 14px",
                                fontSize: "0.85rem",
                                display: "inline-flex",
                                alignItems: "center",
                                gap: "6px",
                              }}
                            >
                              <FaUnlock style={{ color: "var(--accent-purple)" }} /> Unblock
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Unblock Confirmation Modal */}
      {userToUnblock && (
        <div className="delete-overlay" onClick={() => !unblocking && setUserToUnblock(null)}>
          <div
            className="delete-modal glass-panel animate-pop-in"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: "440px", textAlign: "center" }}
          >
            <div
              className="delete-warning-icon"
              style={{
                background: "rgba(16, 185, 129, 0.2)",
                color: "#10b981",
                borderColor: "rgba(16, 185, 129, 0.3)",
              }}
            >
              <FaUnlock />
            </div>
            <h2>Unblock User</h2>
            <p>
              Are you sure you want to unblock this user?
              <br />
              <strong>{userToUnblock.username}</strong> ({userToUnblock.email})
            </p>
            <p style={{ fontSize: "0.8rem", color: "var(--text-secondary)", marginTop: "-0.75rem" }}>
              The user will regain login access and will be permitted to vote, create decisions, and use all DecisionHub features again.
            </p>

            <div className="delete-buttons" style={{ display: "flex", gap: "10px", width: "100%", justifyContent: "center" }}>
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setUserToUnblock(null)}
                disabled={unblocking}
                style={{ flex: 1 }}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn-primary"
                onClick={handleConfirmUnblock}
                disabled={unblocking}
                style={{
                  flex: 1,
                  background: "linear-gradient(135deg, #10b981, #059669)",
                  borderColor: "#10b981",
                }}
              >
                {unblocking ? "Unblocking..." : "Confirm Unblock"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Block User Modal */}
      {showBlockModal && (
        <div className="delete-overlay" onClick={() => !blocking && setShowBlockModal(false)}>
          <div
            className="delete-modal glass-panel animate-pop-in"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: "440px", textAlign: "left", alignItems: "stretch" }}
          >
            <div
              className="delete-warning-icon"
              style={{ alignSelf: "center", marginBottom: "1rem" }}
            >
              <FaExclamationTriangle />
            </div>
            <h2 style={{ textAlign: "center", margin: "0 0 0.5rem" }}>Block User Account</h2>
            <p style={{ textAlign: "center", fontSize: "0.85rem", color: "var(--text-secondary)", margin: "0 0 1.25rem" }}>
              Blocking immediately revokes access and invalidates all active sessions for this user without deleting their decisions, votes, or content.
            </p>

            <form onSubmit={handleBlockUser}>
              <div style={{ marginBottom: "1rem" }}>
                <label style={{ display: "block", fontSize: "0.85rem", marginBottom: "6px", fontWeight: 600 }}>
                  User ID *
                </label>
                <input
                  type="number"
                  placeholder="e.g. 5"
                  value={blockUserId}
                  onChange={(e) => setBlockUserId(e.target.value)}
                  required
                  disabled={blocking}
                  style={{
                    width: "100%",
                    padding: "10px 12px",
                    borderRadius: "8px",
                    background: "rgba(255, 255, 255, 0.05)",
                    border: "1px solid rgba(255, 255, 255, 0.15)",
                    color: "var(--text-primary)",
                    outline: "none",
                  }}
                />
              </div>

              <div style={{ marginBottom: "1.5rem" }}>
                <label style={{ display: "block", fontSize: "0.85rem", marginBottom: "6px", fontWeight: 600 }}>
                  Reason (Optional)
                </label>
                <textarea
                  placeholder="Reason for blocking..."
                  value={blockReason}
                  onChange={(e) => setBlockReason(e.target.value)}
                  rows={3}
                  disabled={blocking}
                  style={{
                    width: "100%",
                    padding: "10px 12px",
                    borderRadius: "8px",
                    background: "rgba(255, 255, 255, 0.05)",
                    border: "1px solid rgba(255, 255, 255, 0.15)",
                    color: "var(--text-primary)",
                    outline: "none",
                    resize: "vertical",
                  }}
                />
              </div>

              <div style={{ display: "flex", gap: "10px" }}>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setShowBlockModal(false)}
                  disabled={blocking}
                  style={{ flex: 1 }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={blocking}
                  style={{
                    flex: 1,
                    backgroundColor: "#ef4444",
                    borderColor: "#ef4444",
                  }}
                >
                  {blocking ? "Blocking..." : "Block User"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
