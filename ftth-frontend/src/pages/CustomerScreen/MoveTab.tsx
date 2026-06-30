import { useEffect, useState } from "react";
import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";
import { Field, inputStyle, primaryBtn, cancelBtn, errText, focusBorder, blurBorder } from "../Users/UsersShared";

interface CheckResult {
  available: boolean;
  oltType: string;
  availablePorts: number;
  message: string;
}

export default function MoveTab({ connectionId, currentPincode, onDone }: { connectionId: number; currentPincode: string; onDone: () => void }) {
  const [pincodes, setPincodes] = useState<string[]>([]);
  const [pincode, setPincode] = useState("");
  const [check, setCheck] = useState<CheckResult | null>(null);
  const [checkLoading, setCheckLoading] = useState(false);
  const [moving, setMoving] = useState(false);
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    api.get<string[]>(ENDPOINTS.INVENTORY_PINCODES).then(setPincodes).catch(() => {});
  }, []);

  const handleCheck = async () => {
    if (!pincode.trim()) return;
    setCheck(null);
    setError("");
    setCheckLoading(true);
    try {
      const res = await api.get<CheckResult>(ENDPOINTS.CONNECTION_CHECK_MOVE(connectionId, pincode.trim()));
      setCheck(res);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to check.");
    } finally {
      setCheckLoading(false);
    }
  };

  const handleMove = async () => {
    if (!check?.available) return;
    setMoving(true);
    setMsg("");
    setError("");
    try {
      await api.post(ENDPOINTS.CONNECTION_MOVE(connectionId), { connectionId, newPincode: Number(pincode.trim()) });
      setMsg("Customer moved successfully.");
      setPincode("");
      setCheck(null);
      setTimeout(onDone, 1000);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to move.");
    } finally {
      setMoving(false);
    }
  };

  return (
    <div>
      <p style={{ fontSize: "13px", color: "#6b7280", margin: "0 0 12px 0" }}>
        Current pincode: <strong>{currentPincode}</strong>
      </p>

      <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", padding: "20px", maxWidth: "420px", marginBottom: "16px" }}>
        <div style={{ display: "flex", gap: "12px", alignItems: "flex-end" }}>
          <Field label="New Pincode">
            <input
              value={pincode}
              onChange={(e) => { setPincode(e.target.value); setCheck(null); }}
              placeholder="Type or pick..."
              list="move-tab-pincode-list"
              style={inputStyle}
              onFocus={focusBorder}
              onBlur={blurBorder}
            />
            <datalist id="move-tab-pincode-list">
              {pincodes.filter((p) => p !== currentPincode).map((p) => (
                <option key={p} value={p} />
              ))}
            </datalist>
          </Field>
          <button onClick={handleCheck} disabled={!pincode.trim() || checkLoading} style={{ ...primaryBtn, opacity: !pincode.trim() ? 0.5 : 1 }}>
            {checkLoading ? "Checking..." : "Check"}
          </button>
        </div>
      </div>

      {check && (
        <div style={{
          background: check.available ? "#eff6ff" : "#fef2f2",
          border: `1px solid ${check.available ? "#bfdbfe" : "#fecaca"}`,
          borderRadius: "4px", padding: "12px 16px", marginBottom: "12px",
        }}>
          <p style={{ fontSize: "13px", color: check.available ? "#1e40af" : "#991b1b", margin: 0 }}>
            {check.message}
          </p>
        </div>
      )}

      {msg && <p style={{ fontSize: "13px", color: "#16a34a", marginBottom: "12px" }}>{msg}</p>}
      {error && <p style={{ ...errText, marginBottom: "12px" }}>{error}</p>}

      <div style={{ display: "flex", gap: "8px" }}>
        <button onClick={() => { setPincode(""); setCheck(null); }} style={cancelBtn}>Clear</button>
        <button onClick={handleMove} disabled={!check?.available || moving} style={{ ...primaryBtn, opacity: !check?.available ? 0.5 : 1 }}>
          {moving ? "Moving..." : "Confirm Move"}
        </button>
      </div>
    </div>
  );
}
