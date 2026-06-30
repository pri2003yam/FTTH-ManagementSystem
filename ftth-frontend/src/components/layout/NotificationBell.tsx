import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { workRequestService } from "../../services/workRequestService";
import type { WRNotification } from "../../types/models";

export default function NotificationBell() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<WRNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const fetchNotifications = async () => {
    if (!user) return;
    try {
      const res = await workRequestService.getNotifications(user);
      setNotifications(res.notifications);
      setUnreadCount(res.unreadCount);
    } catch { /* ignore */ }
  };

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 30000);
    return () => clearInterval(interval);
  }, [user]);

  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  const handleNotificationClick = async (n: WRNotification) => {
    if (!n.isRead) {
      try {
        await workRequestService.markNotificationRead(n.notificationId);
        setNotifications((prev) => prev.map((x) => x.notificationId === n.notificationId ? { ...x, isRead: true } : x));
        setUnreadCount((c) => Math.max(0, c - 1));
      } catch { /* ignore */ }
    }
    setOpen(false);
    navigate("/work-requests");
  };

  return (
    <div ref={ref} style={{ position: "relative" }}>
      <button
        onClick={() => setOpen(!open)}
        style={{
          background: "none", border: "none", cursor: "pointer", position: "relative",
          padding: "6px", borderRadius: "50%", display: "flex", alignItems: "center",
        }}
        aria-label="Notifications"
      >
        {/* Bell SVG */}
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#4b5563" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
        {unreadCount > 0 && (
          <span style={{
            position: "absolute", top: "2px", right: "2px",
            background: "#ef4444", color: "#fff", fontSize: "10px", fontWeight: 700,
            borderRadius: "50%", minWidth: "16px", height: "16px",
            display: "flex", alignItems: "center", justifyContent: "center",
            padding: "0 4px",
          }}>
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div style={{
          position: "absolute", top: "40px", right: 0, width: "340px",
          background: "#fff", borderRadius: "10px", boxShadow: "0 4px 24px rgba(0,0,0,0.15)",
          zIndex: 1000, overflow: "hidden", border: "1px solid #e5e7eb",
        }}>
          <div style={{ padding: "12px 16px", borderBottom: "1px solid #f3f4f6", fontWeight: 700, fontSize: "15px" }}>
            Notifications
          </div>
          <div style={{ maxHeight: "320px", overflowY: "auto" }}>
            {notifications.length === 0 ? (
              <div style={{ padding: "24px 16px", textAlign: "center", color: "#9ca3af", fontSize: "13px" }}>
                No notifications
              </div>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.notificationId}
                  onClick={() => handleNotificationClick(n)}
                  style={{
                    padding: "10px 16px", cursor: "pointer",
                    background: n.isRead ? "#fff" : "#eff6ff",
                    borderBottom: "1px solid #f3f4f6",
                    transition: "background 0.15s",
                  }}
                >
                  <div style={{ fontSize: "13px", color: "#1f2937", lineHeight: 1.4 }}>{n.message}</div>
                  <div style={{ fontSize: "11px", color: "#9ca3af", marginTop: "4px" }}>
                    {new Date(n.createdAt).toLocaleString()}
                    {!n.isRead && <span style={{ marginLeft: "8px", color: "#3b82f6", fontWeight: 600 }}>● New</span>}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
