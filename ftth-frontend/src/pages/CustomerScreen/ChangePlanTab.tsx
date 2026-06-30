import { useEffect, useState } from "react";
import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";
import { primaryBtn, cancelBtn, thStyle, tdStyle, errText } from "../Users/UsersShared";

interface Plan {
  planId: number;
  planName: string;
  speedLabel: string;
  dataLimitLabel: string;
  ottCount: number;
  monthlyPrice: number;
  oltType: string;
}

export default function ChangePlanTab({ connectionId, onDone }: { connectionId: number; onDone: () => void }) {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Plan | null>(null);
  const [changing, setChanging] = useState(false);
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    api.get<Plan[]>(ENDPOINTS.CONNECTION_AVAILABLE_PLANS(connectionId))
      .then(setPlans)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [connectionId]);

  const handleChange = async () => {
    if (!selected) return;
    setChanging(true);
    setMsg("");
    setError("");
    try {
      await api.post(ENDPOINTS.CONNECTION_CHANGE_PLAN(connectionId), { connectionId, planId: selected.planId });
      setMsg("Plan changed successfully.");
      setSelected(null);
      setTimeout(onDone, 1000);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to change plan.");
    } finally {
      setChanging(false);
    }
  };

  return (
    <div>
      <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden", marginBottom: "16px" }}>
        {loading ? (
          <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>Loading available plans...</p>
        ) : plans.length === 0 ? (
          <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>No other plans available for this service area.</p>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ background: "#f1f5f9" }}>
                {["Plan Name", "Speed", "Data", "OTTs", "OLT", "Price"].map((h) => (
                  <th key={h} style={thStyle}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {plans.map((p, i) => {
                const sel = selected?.planId === p.planId;
                return (
                  <tr key={p.planId} onClick={() => setSelected(p)}
                    style={{ background: sel ? "#e0f2fe" : i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb", cursor: "pointer" }}>
                    <td style={{ ...tdStyle, fontWeight: sel ? 600 : 400 }}>{p.planName}</td>
                    <td style={tdStyle}>{p.speedLabel}</td>
                    <td style={tdStyle}>{p.dataLimitLabel}</td>
                    <td style={tdStyle}>{p.ottCount}</td>
                    <td style={tdStyle}>{p.oltType}</td>
                    <td style={{ ...tdStyle, color: "#256D85", fontWeight: 500 }}>Rs. {p.monthlyPrice}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {selected && (
        <div style={{ background: "#eff6ff", border: "1px solid #bfdbfe", borderRadius: "4px", padding: "12px 16px", marginBottom: "12px" }}>
          <p style={{ fontSize: "13px", color: "#1e40af", margin: 0 }}>
            Switch to <strong>{selected.planName}</strong> @ Rs.{selected.monthlyPrice} ({selected.oltType})
          </p>
        </div>
      )}

      {msg && <p style={{ fontSize: "13px", color: "#16a34a", marginBottom: "12px" }}>{msg}</p>}
      {error && <p style={{ ...errText, marginBottom: "12px" }}>{error}</p>}

      <div style={{ display: "flex", gap: "8px" }}>
        <button onClick={() => setSelected(null)} style={cancelBtn} disabled={!selected}>Cancel</button>
        <button onClick={handleChange} disabled={!selected || changing} style={{ ...primaryBtn, opacity: !selected ? 0.5 : 1 }}>
          {changing ? "Changing..." : "Confirm Change"}
        </button>
      </div>
    </div>
  );
}
