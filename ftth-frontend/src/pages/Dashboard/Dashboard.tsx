import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";

// ── Types ──
interface CsrStats {
  totalCustomers: number;
  newCustomersToday: number;
  pendingRequests: number;
}

interface RecentActivity {
  id: number;
  type: string;
  description: string;
  timestamp: string;
}

interface MaintStats {
  pendingTasks: number;
  resolvedToday: number;
}

interface MaintIssue {
  id: number;
  area: string;
  description: string;
  severity: "HIGH" | "MEDIUM" | "LOW";
  status: string;
}

interface ConnectionsChart {
  newConnections: number;
  changePlan: number;
  move: number;
  disconnect: number;
  total: number;
}

// ── Styles ──
const container: React.CSSProperties = {
  fontFamily: "'Source Sans 3', 'Segoe UI', sans-serif",
  padding: "24px",
  maxWidth: "1200px",
};

const sectionTitle: React.CSSProperties = {
  fontSize: "14px",
  fontWeight: 600,
  color: "#6b7280",
  textTransform: "uppercase",
  letterSpacing: "0.5px",
  margin: "28px 0 12px 0",
};

const statsGrid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
  gap: "14px",
};

const statCard: React.CSSProperties = {
  background: "#fff",
  border: "1px solid #e5e7eb",
  borderRadius: "8px",
  padding: "18px 16px",
};

const statValue: React.CSSProperties = {
  fontSize: "26px",
  fontWeight: 700,
  color: "#111827",
  margin: "0 0 2px 0",
};

const statLabel: React.CSSProperties = {
  fontSize: "13px",
  color: "#6b7280",
  margin: 0,
};

const actionGrid: React.CSSProperties = { display: "flex", gap: "12px" };

const actionBtn: React.CSSProperties = {
  background: "#2563eb",
  color: "#fff",
  border: "none",
  borderRadius: "6px",
  padding: "10px 18px",
  fontSize: "13px",
  fontWeight: 600,
  cursor: "pointer",
};

const insightCard: React.CSSProperties = {
  background: "#f9fafb",
  border: "1px solid #e5e7eb",
  borderRadius: "6px",
  padding: "12px 16px",
  marginBottom: "8px",
};

const badge = (color: string): React.CSSProperties => ({
  display: "inline-block",
  background: color,
  color: "#fff",
  fontSize: "11px",
  fontWeight: 600,
  borderRadius: "4px",
  padding: "2px 8px",
  marginLeft: "8px",
});

// ── Main Component ──
export default function Dashboard() {
  const { role, user } = useAuth();
  const navigate = useNavigate();

  return (
    <div style={container}>
      <p style={{ fontSize: "14px", color: "#6b7280", margin: "0 0 8px 0" }}>Welcome back, {user}.</p>
      {role === "ADMIN" && <AdminDashboard navigate={navigate} />}
      {role === "CSR" && <CsrDashboard navigate={navigate} />}
      {role === "MAINT" && <MaintDashboard navigate={navigate} />}
    </div>
  );
}

// ══════════════════════════════════════════════
// ADMIN DASHBOARD
// ══════════════════════════════════════════════
const PIE_COLORS = ["#2563eb", "#7c3aed", "#059669", "#dc2626", "#6b7280"];
const PIE_LABELS = ["New", "Change", "Move", "Disconnect", "Total Active"];

function PieChart({ data }: { data: ConnectionsChart }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const values = [data.newConnections, data.changePlan, data.move, data.disconnect, data.total];
  const sum = values.reduce((a, b) => a + b, 0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const size = 200;
    canvas.width = size;
    canvas.height = size;
    const cx = size / 2, cy = size / 2, r = size / 2 - 10;

    ctx.clearRect(0, 0, size, size);

    if (sum === 0) {
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, 2 * Math.PI);
      ctx.fillStyle = "#e5e7eb";
      ctx.fill();
      return;
    }

    let startAngle = -Math.PI / 2;
    values.forEach((v, i) => {
      const slice = (v / sum) * 2 * Math.PI;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.arc(cx, cy, r, startAngle, startAngle + slice);
      ctx.closePath();
      ctx.fillStyle = PIE_COLORS[i];
      ctx.fill();
      ctx.strokeStyle = "#fff";
      ctx.lineWidth = 2;
      ctx.stroke();
      startAngle += slice;
    });

    // center hole
    ctx.beginPath();
    ctx.arc(cx, cy, r * 0.45, 0, 2 * Math.PI);
    ctx.fillStyle = "#fff";
    ctx.fill();
  }, [data]);

  return (
    <div style={{ display: "flex", alignItems: "center", gap: "32px", flexWrap: "wrap" }}>
      <canvas ref={canvasRef} style={{ width: "200px", height: "200px" }} />
      <div>
        {PIE_LABELS.map((label, i) => (
          <div key={label} style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "8px" }}>
            <div style={{ width: "12px", height: "12px", borderRadius: "3px", background: PIE_COLORS[i], flexShrink: 0 }} />
            <span style={{ fontSize: "13px", color: "#374151", minWidth: "90px" }}>{label}</span>
            <span style={{ fontSize: "14px", fontWeight: 700, color: "#111827" }}>{values[i]}</span>
          </div>
        ))}
        {sum > 0 && (
          <div style={{ marginTop: "8px", fontSize: "12px", color: "#9ca3af" }}>
            Last 30 days activity
          </div>
        )}
      </div>
    </div>
  );
}

function AdminDashboard({ navigate }: { navigate: ReturnType<typeof useNavigate> }) {
  const [chart, setChart] = useState<ConnectionsChart | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<ConnectionsChart>(ENDPOINTS.DASHBOARD_CONNECTIONS_CHART)
      .then(setChart)
      .catch(() => setChart({ newConnections: 0, changePlan: 0, move: 0, disconnect: 0, total: 0 }))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p style={{ color: "#6b7280" }}>Loading dashboard...</p>;

  return (
    <>
      <p style={sectionTitle}>📊 Connections Overview — Last 30 Days</p>
      <div style={{ background: "#fff", border: "1px solid #e5e7eb", borderRadius: "8px", padding: "24px", display: "inline-block" }}>
        {chart && <PieChart data={chart} />}
      </div>

      <p style={sectionTitle}>⚙️ Quick Actions</p>
      <div style={actionGrid}>
        <button style={{ ...actionBtn, flex: 1 }} onClick={() => navigate("/plans")}>Add Plan</button>
        <button style={{ ...actionBtn, flex: 1 }} onClick={() => navigate("/users")}>Add User</button>
        <button style={{ ...actionBtn, flex: 1 }} onClick={() => navigate("/inventory")}>Manage Inventory</button>
        <button style={{ ...actionBtn, flex: 1 }} onClick={() => navigate("/connections")}>New Connection</button>
        <button style={{ ...actionBtn, flex: 1, background: "#7c3aed" }} onClick={() => navigate("/maintenance")}>Maintenance</button>
      </div>
    </>
  );
}

// ══════════════════════════════════════════════
// CSR DASHBOARD
// ══════════════════════════════════════════════
function CsrDashboard({ navigate }: { navigate: ReturnType<typeof useNavigate> }) {
  const [stats, setStats] = useState<CsrStats | null>(null);
  const [recentActivity, setRecentActivity] = useState<RecentActivity[]>([]);
  const [searchCode, setSearchCode] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    api.get<CsrStats>(ENDPOINTS.DASHBOARD_CSR)
      .then((data) => {
        setStats(data);
        if ((data as any).recentActivity) setRecentActivity((data as any).recentActivity);
      })
      .catch(() => setStats({ totalCustomers: 0, newCustomersToday: 0, pendingRequests: 0 }))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p style={{ color: "#6b7280" }}>Loading dashboard...</p>;

  return (
    <>
      <p style={sectionTitle}>👥 Customer Summary</p>
      {stats && (
        <div style={statsGrid}>
          <StatCard value={stats.totalCustomers} label="Total Customers" />
          <StatCard value={stats.newCustomersToday} label="New Today" color="#059669" />
          <StatCard value={stats.pendingRequests} label="Pending Requests" color="#f59e0b" />
        </div>
      )}

      <p style={sectionTitle}>🔍 Quick Customer Lookup</p>
      <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
        <input
          type="text"
          placeholder="Enter customer code..."
          value={searchCode}
          onChange={(e) => setSearchCode(e.target.value)}
          onKeyDown={(e) => { if (e.key === "Enter" && searchCode.trim()) navigate(`/customers?search=${searchCode.trim()}`); }}
          style={{ border: "1px solid #d1d5db", borderRadius: "6px", padding: "8px 12px", fontSize: "13px", width: "240px" }}
        />
        <button style={{ ...actionBtn, background: "#4b5563" }} onClick={() => { if (searchCode.trim()) navigate(`/customers?search=${searchCode.trim()}`); }}>Search</button>
      </div>

      {recentActivity.length > 0 && (
        <>
          <p style={sectionTitle}>📋 Recent Activity</p>
          {recentActivity.slice(0, 5).map((a) => (
            <div key={a.id} style={insightCard}>
              <span style={{ fontWeight: 500, fontSize: "13px" }}>{a.type}</span>
              <span style={{ fontSize: "13px", color: "#6b7280", marginLeft: "8px" }}>{a.description}</span>
              <span style={{ fontSize: "11px", color: "#9ca3af", float: "right" }}>{a.timestamp}</span>
            </div>
          ))}
        </>
      )}

      <p style={sectionTitle}>📌 Quick Actions</p>
      <div style={actionGrid}>
        <button style={{ ...actionBtn, flex: 1 }} onClick={() => navigate("/customers")}>Add Customer</button>
        <button style={{ ...actionBtn, flex: 1 }} onClick={() => navigate("/connections")}>New Connection</button>
        <button style={{ ...actionBtn, flex: 1, background: "#7c3aed" }} onClick={() => navigate("/connections")}>Change Plan</button>
        <button style={{ ...actionBtn, flex: 1, background: "#dc2626" }} onClick={() => navigate("/connections")}>Disconnect</button>
      </div>
    </>
  );
}

// ══════════════════════════════════════════════
// MAINTENANCE DASHBOARD
// ══════════════════════════════════════════════
function MaintDashboard({ navigate }: { navigate: ReturnType<typeof useNavigate> }) {
  const [stats, setStats] = useState<MaintStats | null>(null);
  const [issues, setIssues] = useState<MaintIssue[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    api.get<MaintStats>(ENDPOINTS.DASHBOARD_MAINT)
      .then((data) => {
        setStats(data);
        if ((data as any).issues) setIssues((data as any).issues);
      })
      .catch(() => setStats({ pendingTasks: 0, resolvedToday: 0 }))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p style={{ color: "#6b7280" }}>Loading dashboard...</p>;

  const severityColor = { HIGH: "#dc2626", MEDIUM: "#f59e0b", LOW: "#6b7280" };

  return (
    <>
      <p style={sectionTitle}>🚨 Overview</p>
      {stats && (
        <div style={statsGrid}>
          <StatCard value={stats.pendingTasks} label="Pending Tasks" color="#f59e0b" />
          <StatCard value={stats.resolvedToday} label="Resolved Today" color="#059669" />
        </div>
      )}

      {issues.length > 0 && (
        <>
          <p style={sectionTitle}>📋 Active Issues</p>
          {issues.slice(0, 8).map((issue) => (
            <div key={issue.id} style={insightCard}>
              <span style={badge(severityColor[issue.severity])}>{issue.severity}</span>
              <span style={{ fontWeight: 500, marginLeft: "8px" }}>{issue.area}</span>
              <span style={{ fontSize: "13px", color: "#6b7280", marginLeft: "12px" }}>{issue.description}</span>
              <span style={{ fontSize: "12px", color: "#9ca3af", float: "right" }}>{issue.status}</span>
            </div>
          ))}
        </>
      )}

      <p style={sectionTitle}>⚡ Quick Actions</p>
      <div style={actionGrid}>
        <button style={{ ...actionBtn, flex: 1 }} onClick={() => navigate("/maintenance")}>View All Tasks</button>
        <button style={{ ...actionBtn, flex: 1, background: "#059669" }} onClick={() => navigate("/maintenance")}>Mark Resolved</button>
        <button style={{ ...actionBtn, flex: 1, background: "#4b5563" }} onClick={() => navigate("/capacity")}>Check Capacity</button>
        <button style={{ ...actionBtn, flex: 1, background: "#7c3aed" }} onClick={() => navigate("/inventory")}>Inventory Status</button>
      </div>
    </>
  );
}

// ── Shared Components ──
function StatCard({ value, label, color }: { value: string | number; label: string; color?: string }) {
  return (
    <div style={statCard}>
      <p style={{ ...statValue, color: color || "#111827" }}>{value}</p>
      <p style={statLabel}>{label}</p>
    </div>
  );
}
