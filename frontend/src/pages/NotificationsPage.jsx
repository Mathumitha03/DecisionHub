import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import Navbar from "../components/Navbar";
import LoadingState from "../components/LoadingState";
import { useToast } from "../components/Toast";
import { useNotifications } from "../context/NotificationsContext";
import { useAuth } from "../context/AuthContext";
import {
  FaBell,
  FaCheckDouble,
  FaTrash,
  FaCheck,
  FaComment,
  FaVoteYea,
  FaUsers,
  FaClock,
  FaCog,
  FaBullhorn,
  FaExclamationTriangle,
  FaLock,
  FaUnlock,
  FaThumbtack,
  FaTimes
} from "react-icons/fa";
import "../styles/NotificationsPage.css";
import { getNotifications, broadcastNotification } from "../services/notificationService";

function NotificationsPage() {
  const { user } = useAuth();
  const { addToast } = useToast();
  const navigate = useNavigate();
  const [filter, setFilter] = useState("all"); // "all", "unread"

  const {
    unreadCount,
    markAsRead,
    markAllAsRead,
    deleteNotif,
    clearAllNotifs
  } = useNotifications();

  const [notifications, setNotifications] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalAllCount, setTotalAllCount] = useState(0);
  const [localLoading, setLocalLoading] = useState(false);

  // Broadcast Announcement State for Admin
  const [showBroadcastModal, setShowBroadcastModal] = useState(false);
  const [broadcastTitle, setBroadcastTitle] = useState("");
  const [broadcastMessage, setBroadcastMessage] = useState("");
  const [broadcastActionUrl, setBroadcastActionUrl] = useState("");
  const [broadcasting, setBroadcasting] = useState(false);

  const fetchTotalAllCount = async () => {
    try {
      const data = await getNotifications(0, 1, false);
      if (data) {
        setTotalAllCount(data.totalElements || 0);
      }
    } catch (err) {
      console.error("Failed to fetch total all notifications count", err);
    }
  };

  const loadNotifications = async (pageNum = 0, isUnreadOnly = filter === "unread", append = false) => {
    setLocalLoading(true);
    try {
      const data = await getNotifications(pageNum, 10, isUnreadOnly);
      if (data) {
        const content = data.content || [];
        if (append) {
          setNotifications((prev) => {
            const existingIds = new Set(prev.map(item => item.id));
            const newItems = content.filter(item => !existingIds.has(item.id));
            return [...prev, ...newItems];
          });
        } else {
          setNotifications(content);
        }
        setTotalPages(data.totalPages || 0);
        setTotalElements(data.totalElements || 0);
        setPage(pageNum);
      }
    } catch (err) {
      console.error("Failed to load notifications page", err);
    } finally {
      setLocalLoading(false);
    }
  };

  // Reload list and counts when filter tab changes
  useEffect(() => {
    loadNotifications(0, filter === "unread", false);
    fetchTotalAllCount();
  }, [filter]);

  // Poll list and counts every 30 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      loadNotifications(0, filter === "unread", false);
      fetchTotalAllCount();
    }, 30000);
    return () => clearInterval(interval);
  }, [filter]);

  const handleMarkAllRead = async () => {
    try {
      await markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      fetchTotalAllCount();
      addToast("All notifications marked as read.", "success");
    } catch (err) {
      console.error(err);
      addToast("Failed to mark all notifications as read.", "error");
    }
  };

  const handleMarkAsRead = async (id) => {
    try {
      await markAsRead(id);
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)));
      fetchTotalAllCount();
    } catch (err) {
      console.error(err);
      addToast("Failed to mark notification as read.", "error");
    }
  };

  const handleDeleteNotif = async (id) => {
    try {
      await deleteNotif(id);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      fetchTotalAllCount();
      addToast("Notification deleted.", "success");
    } catch (err) {
      console.error(err);
      addToast("Failed to delete notification.", "error");
    }
  };

  const handleClearAll = async () => {
    try {
      await clearAllNotifs();
      setNotifications([]);
      setTotalAllCount(0);
      addToast("All notifications cleared.", "info");
    } catch (err) {
      console.error(err);
      addToast("Failed to delete all notifications.", "error");
    }
  };

  const handleNotifClick = async (notif) => {
    try {
      if (!notif.isRead) {
        await markAsRead(notif.id);
        setNotifications((prev) => prev.map((n) => (n.id === notif.id ? { ...n, isRead: true } : n)));
        fetchTotalAllCount();
      }
      if (notif.actionUrl) {
        navigate(notif.actionUrl);
      }
    } catch (err) {
      console.error(err);
      addToast("Failed to process notification click.", "error");
    }
  };

  const handleSendBroadcast = async (e) => {
    e.preventDefault();
    if (!broadcastTitle.trim() || !broadcastMessage.trim()) {
      addToast("Title and message are required.", "warning");
      return;
    }
    setBroadcasting(true);
    try {
      await broadcastNotification({
        title: broadcastTitle.trim(),
        message: broadcastMessage.trim(),
        actionUrl: broadcastActionUrl.trim() || null
      });
      addToast("Broadcast announcement sent successfully to all users!", "success");
      setShowBroadcastModal(false);
      setBroadcastTitle("");
      setBroadcastMessage("");
      setBroadcastActionUrl("");
      loadNotifications(0, filter === "unread", false);
      fetchTotalAllCount();
    } catch (err) {
      console.error(err);
      addToast(err?.response?.data?.message || "Failed to send broadcast announcement.", "error");
    } finally {
      setBroadcasting(false);
    }
  };

  const getNotificationIcon = (type) => {
    switch (type) {
      case "COMMENT":
        return <FaComment />;
      case "COMMENT_REMOVED":
        return <FaTrash style={{ color: "var(--danger, #ef4444)" }} />;
      case "VOTE":
        return <FaVoteYea />;
      case "COMMUNITY":
        return <FaUsers />;
      case "DECISION":
        return <FaBell />;
      case "DECISION_LOCKED":
        return <FaLock style={{ color: "var(--warning, #eab308)" }} />;
      case "DECISION_UNLOCKED":
        return <FaUnlock style={{ color: "var(--accent-blue)" }} />;
      case "DECISION_PINNED":
      case "DECISION_UNPINNED":
        return <FaThumbtack style={{ color: "var(--accent-purple)" }} />;
      case "REPORT":
        return <FaExclamationTriangle style={{ color: "var(--danger, #ef4444)" }} />;
      case "REMINDER":
        return <FaClock />;
      case "SYSTEM":
        return <FaBullhorn style={{ color: "var(--accent-purple)" }} />;
      default:
        return <FaBell />;
    }
  };

  const formatDateTime = (dateStr) => {
    if (!dateStr) return "";
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      return d.toLocaleString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="dashboard">
      <Sidebar />
      <div className="dashboard-main">
        <Navbar />
        <div className="dashboard-content animate-fade-in">
          <div className="notifications-page-header">
            <div>
              <h1>Notifications Center</h1>
              <p>Keep track of activities, comments, and voting milestones.</p>
            </div>

            <div className="notifications-page-actions">
              {user?.role === "ADMIN" && (
                <button
                  className="btn-primary"
                  onClick={() => setShowBroadcastModal(true)}
                  style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
                >
                  <FaBullhorn /> Broadcast Announcement
                </button>
              )}
              {unreadCount > 0 && (
                <button className="btn-secondary" onClick={handleMarkAllRead}>
                  <FaCheckDouble /> Mark All Read
                </button>
              )}
              {totalAllCount > 0 && (
                <button className="btn-secondary clear-btn-all" onClick={handleClearAll}>
                  <FaTrash /> Delete All
                </button>
              )}
            </div>
          </div>

          <div className="notifications-card glass-panel">
            {/* Filter Tabs */}
            <div className="notifications-tabs">
              <button
                className={`tab-btn ${filter === "all" ? "active-tab" : ""}`}
                onClick={() => setFilter("all")}
              >
                All Notifications <span className="tab-count">{totalAllCount}</span>
              </button>
              <button
                className={`tab-btn ${filter === "unread" ? "active-tab" : ""}`}
                onClick={() => setFilter("unread")}
              >
                Unread <span className="tab-count">{unreadCount}</span>
              </button>
            </div>

            {/* List */}
            <div className="notifications-page-list">
              {localLoading && notifications.length === 0 ? (
                <LoadingState />
              ) : notifications.length === 0 ? (
                <div className="empty-state">
                  <FaBell className="empty-icon" />
                  <h3>No notifications found</h3>
                  <p>
                    You're all caught up! When new activities occur, they'll
                    show up here.
                  </p>
                </div>
              ) : (
                <>
                  {notifications.map((n) => (
                    <div
                      key={n.id}
                      className={`notif-page-item ${!n.isRead ? "unread" : ""}`}
                    >
                      <div className="notif-indicator-dot"></div>
                      <div className="notif-page-icon">
                        {getNotificationIcon(n.type)}
                      </div>
                      <div
                        className="notif-page-content"
                        onClick={() => handleNotifClick(n)}
                        style={{ cursor: "pointer" }}
                      >
                        {n.title && <h4>{n.title}</h4>}
                        <p>{n.message}</p>
                        <span className="notif-page-time">
                          {formatDateTime(n.createdAt)}
                        </span>
                      </div>
                      <div className="notif-page-buttons">
                        {!n.isRead && (
                          <button
                            className="action-btn-p read-btn-p"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleMarkAsRead(n.id);
                            }}
                            title="Mark as Read"
                          >
                            <FaCheck /> Mark read
                          </button>
                        )}
                        <button
                          className="action-btn-p delete-btn-p"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDeleteNotif(n.id);
                          }}
                          title="Delete"
                        >
                          <FaTrash />
                        </button>
                      </div>
                    </div>
                  ))}

                  {page + 1 < totalPages && (
                    <div style={{ display: "flex", justifyContent: "center", padding: "15px 0" }}>
                      <button
                        className="btn-secondary"
                        onClick={() => loadNotifications(page + 1, filter === "unread", true)}
                        disabled={localLoading}
                      >
                        {localLoading ? "Loading..." : "Load More"}
                      </button>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Broadcast Announcement Modal for Admin */}
      {showBroadcastModal && (
        <div className="broadcast-modal-overlay" onClick={() => setShowBroadcastModal(false)}>
          <div className="broadcast-modal" onClick={(e) => e.stopPropagation()}>
            <div className="broadcast-modal-header">
              <h2><FaBullhorn /> Broadcast Announcement</h2>
              <button
                className="broadcast-modal-close"
                onClick={() => setShowBroadcastModal(false)}
                title="Close"
              >
                <FaTimes />
              </button>
            </div>
            <form onSubmit={handleSendBroadcast}>
              <div className="broadcast-form-group">
                <label>Announcement Title *</label>
                <input
                  type="text"
                  placeholder="e.g. Platform Notice / Maintenance"
                  value={broadcastTitle}
                  onChange={(e) => setBroadcastTitle(e.target.value)}
                  required
                  maxLength={100}
                />
              </div>
              <div className="broadcast-form-group">
                <label>Message *</label>
                <textarea
                  rows="4"
                  placeholder="Enter the notification message to broadcast to all active users..."
                  value={broadcastMessage}
                  onChange={(e) => setBroadcastMessage(e.target.value)}
                  required
                  maxLength={500}
                />
              </div>
              <div className="broadcast-form-group">
                <label>Action URL (Optional)</label>
                <input
                  type="text"
                  placeholder="e.g. /decisions or /governance"
                  value={broadcastActionUrl}
                  onChange={(e) => setBroadcastActionUrl(e.target.value)}
                />
              </div>
              <div className="broadcast-modal-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setShowBroadcastModal(false)}
                  disabled={broadcasting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={broadcasting}
                  style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
                >
                  <FaBullhorn /> {broadcasting ? "Broadcasting..." : "Send Announcement"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default NotificationsPage;