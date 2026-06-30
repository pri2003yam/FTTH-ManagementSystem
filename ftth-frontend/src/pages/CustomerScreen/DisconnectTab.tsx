import { useState } from "react";
import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";
import { primaryBtn, cancelBtn, errText } from "../Users/UsersShared";

export default function DisconnectTab({ connectionId, customerName, onDone }: { connectionId: number; customerName: string; onDone: () => void }) {
  const [confirming, setConfirming] = useState(false);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");

  const handleDisconnect = async () => {
    setLoading(true);
    setMsg("");
    setError("");
    try {
      await api.post(ENDPOINTS.CONNECTION_DISCONNECT(connectionId), { connectionId });
      setMsg("Connection disconnected successfully.");
      setConfirming(false);
      setTimeout(onDone, 1000);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to disconnect.");
    } finally {
      setLoading(false);
    }
  };

  if (msg) {
    return <p style={{ fontSize: "13px", color: "#16a34a" }}>{msg}</p>;
  }

  if (!confirming) {
    return (
      <div>
        <div style={{ background: "#fff7ed", border: "1px solid #fed7aa", borderRadius: "4px", padding: "16px", marginBottom: "16px", maxWidth: "480px" }}>
          <p style={{ fontSize: "13px", color: "#9a3412", margin: "0 0 4px 0" }}>
            This will permanently disconnect <strong>{customerName}</strong>'s connection and release the allocated port.
          </p>
          <p style={{ fontSize: "12px", color: "#6b7280", margin: 0 }}>This action cannot be undone.</p>
        </div>
        <button onClick={() => setConfirming(true)} style={{ ...primaryBtn, background: "#dc2626" }}>
          Disconnect
        </button>
      </div>
    );
  }

  return (
    <div style={{ background: "#fef2f2", border: "1px solid #fecaca", borderRadius: "4px", padding: "20px", maxWidth: "480px" }}>
      <p style={{ fontSize: "14px", fontWeight: 600, color: "#991b1b", margin: "0 0 8px 0" }}>Confirm Disconnection</p>
      <p style={{ fontSize: "13px", color: "#374151", margin: "0 0 4px 0" }}>Customer: <strong>{customerName}</strong></p>
      <p style={{ fontSize: "13px", color: "#374151", margin: "0 0 16px 0" }}>Connection ID: <strong>{connectionId}</strong></p>
      <p style={{ fontSize: "13px", color: "#dc2626", margin: "0 0 16px 0" }}>Are you sure? The port will be released and the connection terminated.</p>

      {error && <p style={{ ...errText, marginBottom: "12px" }}>{error}</p>}

      <div style={{ display: "flex", gap: "8px" }}>
        <button onClick={() => setConfirming(false)} style={cancelBtn} disabled={loading}>Cancel</button>
        <button onClick={handleDisconnect} disabled={loading} style={{ ...primaryBtn, background: "#dc2626" }}>
          {loading ? "Disconnecting..." : "Yes, Disconnect"}
        </button>
      </div>
    </div>
  );
}
