import { useEffect, useState } from "react";
import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";
import {
  Field, inputStyle, primaryBtn, cancelBtn, errText,
  focusBorder, blurBorder, thStyle, tdStyle,
} from "../Users/UsersShared";

interface Plan {
  planId: number;
  planName: string;
  speedLabel: string;
  dataLimitLabel: string;
  ottCount: number;
  monthlyPrice: number;
  oltType: string;
}

interface ActiveConnection {
  connectionId: number;
  customerCode: string;
  fullName: string;
  pincode: string;
  planName: string;
  monthlyPrice: number;
  oltType: string;
  oltCode: string;
  splitterNumber: number;
  portNumber: number;
  planId: number;
  serviceAreaId: number;
}

type View = "menu" | "new-install" | "change" | "move" | "disconnect";

const cardStyle: React.CSSProperties = {
  background: "#ffffff",
  border: "1px solid #d1d5db",
  borderRadius: "4px",
  padding: "24px 20px",
  width: "200px",
  textAlign: "center",
};

const sectionLabel: React.CSSProperties = {
  fontSize: "13px",
  fontWeight: 600,
  color: "#6b7280",
  margin: "0 0 8px 0",
};

export default function Connections() {
  const [view, setView] = useState<View>("menu");

  // ── New Install state ──
  const [plans, setPlans] = useState<Plan[]>([]);
  const [pincodes, setPincodes] = useState<string[]>([]);
  const [selectedPlanId, setSelectedPlanId] = useState<number | null>(null);
  const [niPincodeInput, setNiPincodeInput] = useState("");
  const [niForm, setNiForm] = useState({ customerName: "", email: "", salary: "", pincode: "" });
  const [niLoading, setNiLoading] = useState(false);
  const [niDataLoading, setNiDataLoading] = useState(false);
  const [niDataError, setNiDataError] = useState("");
  const [niError, setNiError] = useState("");
  const [niOltTypes, setNiOltTypes] = useState<string[]>([]);
  const [niOltLoading, setNiOltLoading] = useState(false);
  const [niCreatedConn, setNiCreatedConn] = useState<any>(null);

  // ── Change Plan state ──
  const [activeConns, setActiveConns] = useState<ActiveConnection[]>([]);
  const [connsLoading, setConnsLoading] = useState(false);
  const [connsError, setConnsError] = useState("");
  const [cpSearchName, setCpSearchName] = useState("");
  const [selectedConn, setSelectedConn] = useState<ActiveConnection | null>(null);
  const [availPlans, setAvailPlans] = useState<Plan[]>([]);
  const [availPlansLoading, setAvailPlansLoading] = useState(false);
  const [selectedNewPlanId, setSelectedNewPlanId] = useState<number | null>(null);
  const [cpLoading, setCpLoading] = useState(false);
  const [cpError, setCpError] = useState("");
  const [cpSuccess, setCpSuccess] = useState("");
  const [cpSuccessData, setCpSuccessData] = useState<any>(null);

  // ── Move state ──
  const [moveConn, setMoveConn] = useState<ActiveConnection | null>(null);
  const [movePincode, setMovePincode] = useState("");
  const [moveSearch, setMoveSearch] = useState("");
  const [moveCheck, setMoveCheck] = useState<{ available: boolean; oltType: string; availablePorts: number; message: string } | null>(null);
  const [moveCheckLoading, setMoveCheckLoading] = useState(false);
  const [moveLoading, setMoveLoading] = useState(false);
  const [moveError, setMoveError] = useState("");
  const [moveSuccess, setMoveSuccess] = useState("");
  const [moveSuccessData, setMoveSuccessData] = useState<any>(null);
  // ── Disconnect state ──
  const [dcConns, setDcConns] = useState<ActiveConnection[]>([]);
  const [dcConnsLoading, setDcConnsLoading] = useState(false);
  const [dcConnsError, setDcConnsError] = useState("");
  const [dcSearchName, setDcSearchName] = useState("");
  const [dcSelected, setDcSelected] = useState<ActiveConnection | null>(null);
  const [dcConfirming, setDcConfirming] = useState(false);
  const [dcLoading, setDcLoading] = useState(false);
  const [dcError, setDcError] = useState("");
  const [dcSuccess, setDcSuccess] = useState("");

  // ── Load data on view change ──
  useEffect(() => {
    if (view === "new-install") {
      setNiDataLoading(true);
      setNiDataError("");
      Promise.all([
        api.get<Plan[]>(ENDPOINTS.PLANS),
        api.get<string[]>(ENDPOINTS.INVENTORY_PINCODES),
      ])
        .then(([p, pins]) => { setPlans(p); setPincodes(pins); })
        .catch((err) => setNiDataError(err instanceof Error ? err.message : "Failed to load data."))
        .finally(() => setNiDataLoading(false));
    }

    if (view === "change") {
      setConnsLoading(true);
      setConnsError("");
      setSelectedConn(null);
      setAvailPlans([]);
      setSelectedNewPlanId(null);
      setCpSearchName("");
      api.get<ActiveConnection[]>(ENDPOINTS.CONNECTION_ACTIVE)
        .then(setActiveConns)
        .catch((err) => setConnsError(err instanceof Error ? err.message : "Failed to load connections."))
        .finally(() => setConnsLoading(false));
    }
    if (view === "move") {
      setConnsLoading(true);
      setConnsError("");
      setMoveConn(null);
      setMovePincode("");
      setMoveCheck(null);
      setMoveError("");
      setMoveSuccess("");
      api.get<ActiveConnection[]>(ENDPOINTS.CONNECTION_ACTIVE)
        .then(setActiveConns)
        .catch((err) => setConnsError(err instanceof Error ? err.message : "Failed to load connections."))
        .finally(() => setConnsLoading(false));
      // Also load pincodes for datalist
      api.get<string[]>(ENDPOINTS.INVENTORY_PINCODES).then(setPincodes).catch(() => {});
    }

    if (view === "disconnect") {
      setDcConnsLoading(true);
      setDcConnsError("");
      setDcSelected(null);
      setDcConfirming(false);
      setDcError("");
      setDcSuccess("");
      setDcSearchName("");
      api.get<ActiveConnection[]>(ENDPOINTS.CONNECTION_ACTIVE)
        .then(setDcConns)
        .catch((err) => setDcConnsError(err instanceof Error ? err.message : "Failed to load connections."))
        .finally(() => setDcConnsLoading(false));
    }
  }, [view]);

  // ── Load available plans when a connection is selected ──
  useEffect(() => {
    if (!selectedConn) return;
    setAvailPlansLoading(true);
    setAvailPlans([]);
    setSelectedNewPlanId(null);
    setCpError("");
    setCpSuccess("");
    api.get<Plan[]>(ENDPOINTS.CONNECTION_AVAILABLE_PLANS(selectedConn.connectionId))
      .then(setAvailPlans)
      .catch((err) => setCpError(err instanceof Error ? err.message : "Failed to load plans."))
      .finally(() => setAvailPlansLoading(false));
  }, [selectedConn]);

  const resetNi = () => {
    setNiForm({ customerName: "", email: "", salary: "", pincode: "" });
    setSelectedPlanId(null);
    setNiPincodeInput("");
    setNiError("");
    setNiOltTypes([]);
    setNiCreatedConn(null);
  };

  // When pincode is selected, fetch OLT types available
  const handlePincodeSelect = async (pin: string) => {
    setNiForm({ ...niForm, pincode: pin });
    setNiPincodeInput(pin);
    setSelectedPlanId(null);
    setNiOltTypes([]);
    if (!pin) return;
    setNiOltLoading(true);
    try {
      const olts = await api.get<{ oltType: string; availablePorts: number }[]>(
        `${ENDPOINTS.INVENTORY_OLTS}?pincode=${pin}`
      );
      const types = [...new Set(olts.filter(o => o.availablePorts > 0).map(o => o.oltType))];
      setNiOltTypes(types);
    } catch { setNiOltTypes([]); }
    finally { setNiOltLoading(false); }
  };

  const handleNewInstall = async (e: React.FormEvent) => {
    e.preventDefault();
    setNiError("");
    const plan = plans.find((p) => p.planId === selectedPlanId);
    if (!plan) { setNiError("Please select a plan from the table."); return; }
    setNiLoading(true);
    try {
      const res = await api.post<any>(ENDPOINTS.CONNECTION_NEW_INSTALL, {
        customerName: niForm.customerName,
        email: niForm.email,
        salary: Number(niForm.salary),
        pincode: Number(niForm.pincode),
        planId: plan.planId,
        oltType: plan.oltType,
      });
      setNiCreatedConn(res);
      resetNi();
      setNiCreatedConn(res);
    } catch (err: unknown) {
      setNiError(err instanceof Error ? err.message : "Failed to create connection.");
    } finally {
      setNiLoading(false);
    }
  };

  const handleChangePlan = async () => {
    if (!selectedConn || !selectedNewPlanId) return;
    setCpError("");
    setCpSuccess("");
    setCpLoading(true);
    try {
      await api.post(ENDPOINTS.CONNECTION_CHANGE_PLAN(selectedConn.connectionId), { planId: selectedNewPlanId });
      const newPlan = availPlans.find(p => p.planId === selectedNewPlanId);
      setCpSuccessData({
        customerCode: selectedConn.customerCode,
        fullName: selectedConn.fullName,
        oldPlan: selectedConn.planName,
        newPlan: newPlan?.planName,
        newPrice: newPlan?.monthlyPrice,
        oltType: newPlan?.oltType,
        pincode: selectedConn.pincode,
      });
      const updated = await api.get<ActiveConnection[]>(ENDPOINTS.CONNECTION_ACTIVE);
      setActiveConns(updated);
      setSelectedConn(null);
      setAvailPlans([]);
      setSelectedNewPlanId(null);
    } catch (err: unknown) {
      setCpError(err instanceof Error ? err.message : "Failed to change plan.");
    } finally {
      setCpLoading(false);
    }
  };

  const handleCheckMove = async () => {
    if (!moveConn || !movePincode.trim()) return;
    setMoveCheck(null);
    setMoveError("");
    setMoveCheckLoading(true);
    try {
      const res = await api.get<{ available: boolean; oltType: string; availablePorts: number; message: string }>(
        ENDPOINTS.CONNECTION_CHECK_MOVE(moveConn.connectionId, movePincode.trim())
      );
      setMoveCheck(res);
    } catch (err: unknown) {
      setMoveError(err instanceof Error ? err.message : "Failed to check move.");
    } finally {
      setMoveCheckLoading(false);
    }
  };

  const handleMove = async () => {
    if (!moveConn || !moveCheck?.available) return;
    setMoveLoading(true);
    setMoveError("");
    setMoveSuccess("");
    try {
      await api.post(ENDPOINTS.CONNECTION_MOVE(moveConn.connectionId), { newPincode: Number(movePincode.trim()) });
      setMoveSuccessData({
        customerCode: moveConn.customerCode,
        fullName: moveConn.fullName,
        oldPincode: moveConn.pincode,
        newPincode: movePincode.trim(),
        planName: moveConn.planName,
        oltType: moveConn.oltType,
      });
      const updated = await api.get<ActiveConnection[]>(ENDPOINTS.CONNECTION_ACTIVE);
      setActiveConns(updated);
      setMoveConn(null);
      setMovePincode("");
      setMoveCheck(null);
    } catch (err: unknown) {
      setMoveError(err instanceof Error ? err.message : "Failed to move connection.");
    } finally {
      setMoveLoading(false);
    }
  };

  const selectedNewPlan = availPlans.find((p) => p.planId === selectedNewPlanId) ?? null;

  const filterConns = (list: ActiveConnection[], query: string) => {
    const q = query.toLowerCase();
    return list.filter((c) =>
      q === "" ||
      c.fullName.toLowerCase().includes(q) ||
      c.customerCode.toLowerCase().includes(q)
    );
  };

  const handleDisconnect = async () => {
    if (!dcSelected) return;
    setDcLoading(true);
    setDcError("");
    setDcSuccess("");
    try {
      await api.post(ENDPOINTS.CONNECTION_DISCONNECT(dcSelected.connectionId), {});
      setDcSuccess(`Connection #${dcSelected.connectionId} (${dcSelected.fullName}) disconnected successfully.`);
      setDcSelected(null);
      setDcConfirming(false);
      const updated = await api.get<ActiveConnection[]>(ENDPOINTS.CONNECTION_ACTIVE);
      setDcConns(updated);
    } catch (err: unknown) {
      setDcError(err instanceof Error ? err.message : "Failed to disconnect.");
    } finally {
      setDcLoading(false);
    }
  };

  const goBack = () => {
    setView("menu");
    resetNi();
    setCpError("");
    setCpSuccess("");
    setCpSuccessData(null);
    setSelectedConn(null);
    setMoveConn(null);
    setMovePincode("");
    setMoveCheck(null);
    setMoveError("");
    setMoveSuccess("");
    setMoveSuccessData(null);
    setDcSelected(null);
    setDcConfirming(false);
    setDcError("");
    setDcSuccess("");
    setNiCreatedConn(null);
  };

  return (
    <div style={{ fontFamily: "'Source Sans 3', 'Segoe UI', sans-serif", padding: "24px" }}>

      {/* 4 Option Cards */}
      <div style={{ display: "flex", gap: "16px" }}>
        {[
          { key: "new-install", label: "New Install",  desc: "Add a new customer connection",    active: true },
          { key: "move",        label: "Move",         desc: "Move customer to new pincode",      active: true },
          { key: "change",      label: "Change Plan",  desc: "Update customer service plan",      active: true },
          { key: "disconnect",  label: "Disconnect",   desc: "Terminate customer connection",     active: true },
        ].map((opt) => (
          <div
            key={opt.key}
            onClick={() => opt.active && setView(opt.key as View)}
            style={{
              ...cardStyle,
              flex: 1,
              width: "auto",
              borderColor: view === opt.key ? "#256D85" : "#d1d5db",
              cursor: opt.active ? "pointer" : "not-allowed",
              opacity: opt.active ? 1 : 0.45,
            }}
          >
            <p style={{ fontSize: "15px", fontWeight: 600, color: "#111827", margin: "0 0 6px 0" }}>{opt.label}</p>
            <p style={{ fontSize: "13px", color: "#6b7280", margin: 0 }}>{opt.desc}</p>
          </div>
        ))}
      </div>

      {/* ── NEW INSTALL ── */}
      {view === "new-install" && (
        <div style={{ marginTop: "32px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "20px" }}>
            <h2 style={{ fontSize: "16px", fontWeight: 600, color: "#111827", margin: 0 }}>New Install</h2>
            <button onClick={goBack} style={cancelBtn}>← Back</button>
          </div>

          {niDataLoading ? (
            <p style={{ fontSize: "13px", color: "#6b7280" }}>Loading...</p>
          ) : niDataError ? (
            <p style={{ fontSize: "13px", color: "#dc2626" }}>{niDataError}</p>
          ) : (
            <>
              {/* TOP ─ Customer Details Form — original box size */}
              <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", padding: "24px", maxWidth: "480px", marginBottom: "24px" }}>
                <p style={{ ...sectionLabel, marginBottom: "16px" }}>CUSTOMER DETAILS</p>
                <form onSubmit={handleNewInstall} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                  <Field label="Customer Name">
                    <input value={niForm.customerName} onChange={(e) => setNiForm({ ...niForm, customerName: e.target.value })}
                      required placeholder="Enter full name" style={inputStyle} onFocus={focusBorder} onBlur={blurBorder} />
                  </Field>
                  <Field label="Email">
                    <input type="email" value={niForm.email} onChange={(e) => setNiForm({ ...niForm, email: e.target.value })}
                      required placeholder="Enter email address" style={inputStyle} onFocus={focusBorder} onBlur={blurBorder} />
                  </Field>
                  <Field label="Salary (Rs.)">
                    <input type="number" value={niForm.salary} onChange={(e) => setNiForm({ ...niForm, salary: e.target.value })}
                      required placeholder="Minimum Rs. 30,000" style={inputStyle} onFocus={focusBorder} onBlur={blurBorder} />
                  </Field>
                  <Field label="Pincode">
                    <input
                      value={niPincodeInput}
                      onChange={(e) => { 
                        const val = e.target.value;
                        setNiPincodeInput(val); 
                        setNiForm({ ...niForm, pincode: "" }); 
                        setNiOltTypes([]); 
                        setSelectedPlanId(null);
                        if (pincodes.includes(val)) handlePincodeSelect(val);
                      }}
                      placeholder="Enter pincode"
                      list="ni-pincode-list"
                      style={inputStyle} onFocus={focusBorder} onBlur={(e) => { blurBorder(e); if (pincodes.includes(niPincodeInput)) handlePincodeSelect(niPincodeInput); }}
                    />
                    <datalist id="ni-pincode-list">
                      {pincodes.map((p) => <option key={p} value={p} />)}
                    </datalist>
                  </Field>
                  {niOltLoading && <p style={{ fontSize: "12px", color: "#6b7280", margin: 0 }}>Checking OLT types...</p>}
                  {niOltTypes.length > 0 && (
                    <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
                      {niOltTypes.map(t => (
                        <span key={t} style={{ fontSize: "12px", background: "#e0f2fe", color: "#0369a1", padding: "2px 8px", borderRadius: "3px", fontWeight: 500 }}>
                          {t} available
                        </span>
                      ))}
                    </div>
                  )}
                  {niForm.pincode && niOltTypes.length === 0 && !niOltLoading && (
                    <p style={{ fontSize: "12px", color: "#dc2626", margin: 0 }}>No ports available in this pincode.</p>
                  )}
                  {niOltTypes.length > 0 && !plans.find(p => p.planId === selectedPlanId) && (
                    <p style={{ fontSize: "12px", color: "#f59e0b", margin: 0 }}>Select a plan from the table below.</p>
                  )}
                  {plans.find(p => p.planId === selectedPlanId) && (
                    <p style={{ fontSize: "13px", color: "#16a34a", margin: 0, fontWeight: 500 }}>
                      ✓ Plan: {plans.find(p => p.planId === selectedPlanId)!.planName} — Rs.{plans.find(p => p.planId === selectedPlanId)!.monthlyPrice}
                    </p>
                  )}
                  {niError && <p style={errText}>{niError}</p>}
                  <div style={{ display: "flex", gap: "8px", justifyContent: "flex-end", marginTop: "4px" }}>
                    <button type="button" onClick={resetNi} style={cancelBtn}>Clear</button>
                    <button type="submit" disabled={niLoading || !selectedPlanId} style={{ ...primaryBtn, opacity: !selectedPlanId ? 0.5 : 1 }}>
                      {niLoading ? "Creating..." : "Create Connection"}
                    </button>
                  </div>
                </form>
              </div>

              {/* BOTTOM ─ Plans shown only after pincode selected */}
              {niForm.pincode && niOltTypes.length > 0 && (
                <>
                  <p style={sectionLabel}>PLANS FOR PINCODE {niForm.pincode} — click a row to select</p>
                  <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden" }}>
                    <div style={{ maxHeight: "300px", overflowY: "auto" }}>
                      <table style={{ width: "100%", borderCollapse: "collapse" }}>
                        <thead style={{ position: "sticky", top: 0, zIndex: 1 }}>
                          <tr style={{ background: "#f1f5f9" }}>
                            {["Plan Name", "Speed", "Data", "OTTs", "OLT Type", "Price / Month"].map(h => (
                              <th key={h} style={thStyle}>{h}</th>
                            ))}
                          </tr>
                        </thead>
                        <tbody>
                          {plans.filter(p => niOltTypes.includes(p.oltType)).map((p, i) => {
                            const sel = selectedPlanId === p.planId;
                            return (
                              <tr key={p.planId} onClick={() => setSelectedPlanId(p.planId)}
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
                    </div>
                  </div>
                </>
              )}

              {/* ✅ Green success box — same width as form, compact */}
              {niCreatedConn && (
                <div style={{ marginTop: "16px", background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "4px", padding: "12px 16px", maxWidth: "480px" }}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "10px" }}>
                    <p style={{ fontSize: "13px", fontWeight: 600, color: "#166534", margin: 0 }}>✅ Connection Created Successfully</p>
                    <button onClick={() => setNiCreatedConn(null)} style={cancelBtn}>Done</button>
                  </div>
                  <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                    {[
                      ["Connection ID", `#${niCreatedConn.connectionId}`],
                      ["Customer Code", niCreatedConn.customerCode],
                      ["Customer Name", niCreatedConn.fullName],
                      ["Pincode",       niCreatedConn.pincode],
                      ["Plan",          `${niCreatedConn.planName} — Rs.${niCreatedConn.monthlyPrice}/mo`],
                      ["OLT Type",      niCreatedConn.oltType],
                      ["OLT Code",      niCreatedConn.oltCode],
                      ["Port Assigned", `Splitter ${niCreatedConn.splitterNumber} / Port ${niCreatedConn.portNumber}`],
                    ].map(([label, value]) => (
                      <div key={label} style={{ display: "flex", justifyContent: "space-between", fontSize: "13px" }}>
                        <span style={{ color: "#6b7280" }}>{label}</span>
                        <span style={{ color: "#166534", fontWeight: 600 }}>{value}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {/* ── CHANGE PLAN ── */}
      {view === "change" && (
        <div style={{ marginTop: "32px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "20px" }}>
            <h2 style={{ fontSize: "16px", fontWeight: 600, color: "#111827", margin: 0 }}>Change Plan</h2>
            <button onClick={goBack} style={cancelBtn}>← Back</button>
          </div>

          {/* Search bar — Change Plan */}
          <div style={{ display: "flex", gap: "10px", marginBottom: "10px", alignItems: "center" }}>
            <input
              value={cpSearchName}
              onChange={(e) => { setCpSearchName(e.target.value); setSelectedConn(null); }}
              placeholder="Search by name or customer code..."
              style={{ ...inputStyle, width: "280px" }}
              onFocus={focusBorder} onBlur={blurBorder}
            />
            {cpSearchName && (
              <button onClick={() => { setCpSearchName(""); }} style={cancelBtn}>Clear</button>
            )}
          </div>

          {/* Active Connections Table */}
          <p style={sectionLabel}>ACTIVE CONNECTIONS — click a row to select</p>
          <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden", marginBottom: "24px" }}>
            {connsLoading ? (
              <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>Loading connections...</p>
            ) : connsError ? (
              <p style={{ padding: "16px", fontSize: "13px", color: "#dc2626" }}>{connsError}</p>
            ) : activeConns.length === 0 ? (
              <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>No active connections found.</p>
            ) : (() => {
              const filtered = filterConns(activeConns, cpSearchName);
              return filtered.length === 0 ? (
                <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>No connections match your search.</p>
              ) : (
                <div style={{ maxHeight: "294px", overflowY: filtered.length > 7 ? "auto" : "visible" }}>
                  <table style={{ width: "100%", borderCollapse: "collapse" }}>
                    <thead style={{ position: "sticky", top: 0, zIndex: 1 }}>
                      <tr style={{ background: "#f1f5f9" }}>
                        {["Conn ID", "Cust Code", "Customer", "Pincode", "Current Plan", "Price", "OLT", "Port"].map((h) => (
                          <th key={h} style={thStyle}>{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {filtered.map((c, i) => {
                        const sel = selectedConn?.connectionId === c.connectionId;
                        return (
                          <tr key={c.connectionId} onClick={() => setSelectedConn(c)}
                            style={{ background: sel ? "#e0f2fe" : i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb", cursor: "pointer" }}>
                            <td style={tdStyle}>{c.connectionId}</td>
                            <td style={tdStyle}>{c.customerCode}</td>
                            <td style={{ ...tdStyle, fontWeight: sel ? 600 : 400 }}>{c.fullName}</td>
                            <td style={tdStyle}>{c.pincode}</td>
                            <td style={tdStyle}>{c.planName}</td>
                            <td style={{ ...tdStyle, color: "#256D85", fontWeight: 500 }}>Rs. {c.monthlyPrice}</td>
                            <td style={tdStyle}>{c.oltType}</td>
                            <td style={{ ...tdStyle, fontSize: "12px", color: "#6b7280" }}>{c.oltCode}/Spl{c.splitterNumber}/Port{c.portNumber}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              );
            })()}
          </div>

          {/* Available Plans for selected connection */}
          {selectedConn && (
            <>
              <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "4px", padding: "12px 16px", marginBottom: "16px" }}>
                <p style={{ fontSize: "13px", color: "#166534", margin: 0 }}>
                  Selected: <strong>{selectedConn.fullName}</strong> ({selectedConn.customerCode}) — Current plan: <strong>{selectedConn.planName}</strong> @ Rs.{selectedConn.monthlyPrice}
                </p>
              </div>

              <p style={sectionLabel}>AVAILABLE PLANS — click a row to select new plan</p>
              <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden", marginBottom: "20px" }}>
                {availPlansLoading ? (
                  <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>Loading available plans...</p>
                ) : availPlans.length === 0 ? (
                  <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>No other plans available for this service area.</p>
                ) : (
                  <div style={{ maxHeight: "168px", overflowY: availPlans.length > 3 ? "auto" : "visible" }}>
                    <table style={{ width: "100%", borderCollapse: "collapse" }}>
                      <thead style={{ position: "sticky", top: 0, zIndex: 1 }}>
                        <tr style={{ background: "#f1f5f9" }}>
                          {["ID", "Plan Name", "Speed", "Data", "OTTs", "OLT Type", "Price / Month"].map((h) => (
                            <th key={h} style={thStyle}>{h}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {availPlans.map((p, i) => {
                          const sel = selectedNewPlanId === p.planId;
                          return (
                            <tr key={p.planId} onClick={() => setSelectedNewPlanId(p.planId)}
                              style={{ background: sel ? "#e0f2fe" : i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb", cursor: "pointer" }}>
                              <td style={tdStyle}>{p.planId}</td>
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
                  </div>
                )}
              </div>

              {selectedNewPlan && (
                <div style={{ background: "#eff6ff", border: "1px solid #bfdbfe", borderRadius: "4px", padding: "12px 16px", marginBottom: "16px" }}>
                  <p style={{ fontSize: "13px", color: "#1e40af", margin: 0 }}>
                    Change from <strong>{selectedConn.planName}</strong> → <strong>{selectedNewPlan.planName}</strong> @ Rs.{selectedNewPlan.monthlyPrice}
                    {selectedConn.oltType !== selectedNewPlan.oltType && (
                      <span style={{ color: "#b45309", marginLeft: "8px" }}>⚠ OLT type will change, port will be reallocated.</span>
                    )}
                  </p>
                </div>
              )}

              {cpError   && <p style={{ ...errText, marginBottom: "12px" }}>{cpError}</p>}

              <div style={{ display: "flex", gap: "8px" }}>
                <button onClick={() => { setSelectedConn(null); setAvailPlans([]); setSelectedNewPlanId(null); setCpError(""); setCpSuccess(""); }} style={cancelBtn}>
                  Cancel
                </button>
                <button
                  onClick={handleChangePlan}
                  disabled={!selectedNewPlanId || cpLoading}
                  style={{ ...primaryBtn, opacity: !selectedNewPlanId ? 0.5 : 1 }}
                >
                  {cpLoading ? "Changing..." : "Confirm Change"}
                </button>
              </div>
            </>
          )}
        </div>
      )}

      {/* ── CHANGE PLAN SUCCESS BOX ── */}
      {cpSuccessData && view === "change" && (
        <div style={{ marginTop: "16px", background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "4px", padding: "12px 16px", maxWidth: "480px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "10px" }}>
            <p style={{ fontSize: "13px", fontWeight: 600, color: "#166534", margin: 0 }}>✅ Plan Changed Successfully</p>
            <button onClick={() => setCpSuccessData(null)} style={cancelBtn}>Done</button>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            {[
              ["Customer",  `${cpSuccessData.fullName} (${cpSuccessData.customerCode})`],
              ["Pincode",   cpSuccessData.pincode],
              ["Old Plan",  cpSuccessData.oldPlan],
              ["New Plan",  `${cpSuccessData.newPlan} — Rs.${cpSuccessData.newPrice}/mo`],
              ["OLT Type",  cpSuccessData.oltType],
            ].map(([label, value]) => (
              <div key={label} style={{ display: "flex", justifyContent: "space-between", fontSize: "13px" }}>
                <span style={{ color: "#6b7280" }}>{label}</span>
                <span style={{ color: "#166534", fontWeight: 600 }}>{value}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── MOVE ── */}
      {view === "move" && (
        <div style={{ marginTop: "32px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "20px" }}>
            <h2 style={{ fontSize: "16px", fontWeight: 600, color: "#111827", margin: 0 }}>Move Customer</h2>
            <button onClick={goBack} style={cancelBtn}>← Back</button>
          </div>

          {/* Search */}
          <div style={{ marginBottom: "12px", maxWidth: "500px" }}>
            <input
              value={moveSearch}
              onChange={(e) => setMoveSearch(e.target.value)}
              placeholder="Search by customer code, name, or pincode..."
              style={{ ...inputStyle, width: "100%" }}
              onFocus={focusBorder}
              onBlur={blurBorder}
            />
          </div>

          {/* Active Connections Table */}
          <p style={sectionLabel}>ACTIVE CONNECTIONS — click a row to select</p>
          <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden", marginBottom: "24px" }}>
            {connsLoading ? (
              <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>Loading connections...</p>
            ) : connsError ? (
              <p style={{ padding: "16px", fontSize: "13px", color: "#dc2626" }}>{connsError}</p>
            ) : activeConns.length === 0 ? (
              <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>No active connections found.</p>
            ) : (
              <table style={{ width: "100%", borderCollapse: "collapse" }}>
                <thead>
                  <tr style={{ background: "#f1f5f9" }}>
                    {["Conn ID", "Cust Code", "Customer", "Pincode", "Plan", "Price", "OLT", "Port"].map((h) => (
                      <th key={h} style={thStyle}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {activeConns
                    .filter((c) => {
                      if (!moveSearch.trim()) return true;
                      const q = moveSearch.toLowerCase();
                      return c.customerCode.toLowerCase().includes(q)
                        || c.fullName.toLowerCase().includes(q)
                        || c.pincode.includes(q);
                    })
                    .map((c, i) => {
                    const sel = moveConn?.connectionId === c.connectionId;
                    return (
                      <tr key={c.connectionId} onClick={() => { setMoveConn(c); setMovePincode(""); setMoveCheck(null); setMoveError(""); setMoveSuccess(""); }}
                        style={{ background: sel ? "#e0f2fe" : i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb", cursor: "pointer" }}>
                        <td style={tdStyle}>{c.connectionId}</td>
                        <td style={tdStyle}>{c.customerCode}</td>
                        <td style={{ ...tdStyle, fontWeight: sel ? 600 : 400 }}>{c.fullName}</td>
                        <td style={tdStyle}>{c.pincode}</td>
                        <td style={tdStyle}>{c.planName}</td>
                        <td style={{ ...tdStyle, color: "#256D85", fontWeight: 500 }}>Rs. {c.monthlyPrice}</td>
                        <td style={tdStyle}>{c.oltType}</td>
                        <td style={{ ...tdStyle, fontSize: "12px", color: "#6b7280" }}>{c.oltCode}/Spl{c.splitterNumber}/Port{c.portNumber}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>

          {/* Move form */}
          {moveConn && (
            <>
              <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "4px", padding: "12px 16px", marginBottom: "16px" }}>
                <p style={{ fontSize: "13px", color: "#166534", margin: 0 }}>
                  Selected: <strong>{moveConn.fullName}</strong> ({moveConn.customerCode}) — Current pincode: <strong>{moveConn.pincode}</strong> — OLT: <strong>{moveConn.oltType}</strong>
                </p>
              </div>

              <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", padding: "24px", maxWidth: "480px", marginBottom: "16px" }}>
                <div style={{ display: "flex", gap: "12px", alignItems: "flex-end" }}>
                  <Field label="New Pincode">
                    <input
                      value={movePincode}
                      onChange={(e) => { setMovePincode(e.target.value); setMoveCheck(null); }}
                      placeholder="Enter pincode"
                      list="move-pincode-list"
                      style={inputStyle}
                      onFocus={focusBorder}
                      onBlur={blurBorder}
                    />
                    <datalist id="move-pincode-list">
                      {pincodes.filter((p) => p !== moveConn.pincode).map((p) => (
                        <option key={p} value={p} />
                      ))}
                    </datalist>
                  </Field>
                  <button
                    onClick={handleCheckMove}
                    disabled={!movePincode.trim() || moveCheckLoading}
                    style={{ ...primaryBtn, opacity: !movePincode.trim() ? 0.5 : 1 }}
                  >
                    {moveCheckLoading ? "Checking..." : "Check Availability"}
                  </button>
                </div>
              </div>

              {/* Check result */}
              {moveCheck && (
                <div style={{
                  background: moveCheck.available ? "#eff6ff" : "#fef2f2",
                  border: `1px solid ${moveCheck.available ? "#bfdbfe" : "#fecaca"}`,
                  borderRadius: "4px",
                  padding: "12px 16px",
                  marginBottom: "16px",
                }}>
                  <p style={{ fontSize: "13px", color: moveCheck.available ? "#1e40af" : "#991b1b", margin: 0 }}>
                    {moveCheck.message}
                    {moveCheck.available && (
                      <span style={{ marginLeft: "8px" }}>
                        (Move from <strong>{moveConn.pincode}</strong> → <strong>{movePincode}</strong>)
                      </span>
                    )}
                  </p>
                </div>
              )}

              {moveError   && <p style={{ ...errText, marginBottom: "12px" }}>{moveError}</p>}

              <div style={{ display: "flex", gap: "8px" }}>
                <button onClick={() => { setMoveConn(null); setMovePincode(""); setMoveCheck(null); setMoveError(""); setMoveSuccess(""); }} style={cancelBtn}>
                  Cancel
                </button>
                <button
                  onClick={handleMove}
                  disabled={!moveCheck?.available || moveLoading}
                  style={{ ...primaryBtn, opacity: !moveCheck?.available ? 0.5 : 1 }}
                >
                  {moveLoading ? "Moving..." : "Confirm Move"}
                </button>
              </div>
            </>
          )}
        </div>
      )}

      {/* ── MOVE SUCCESS BOX ── */}
      {moveSuccessData && view === "move" && (
        <div style={{ marginTop: "16px", background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "4px", padding: "12px 16px", maxWidth: "480px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "10px" }}>
            <p style={{ fontSize: "13px", fontWeight: 600, color: "#166534", margin: 0 }}>✅ Customer Moved Successfully</p>
            <button onClick={() => setMoveSuccessData(null)} style={cancelBtn}>Done</button>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            {[
              ["Customer",    `${moveSuccessData.fullName} (${moveSuccessData.customerCode})`],
              ["Old Pincode", moveSuccessData.oldPincode],
              ["New Pincode", moveSuccessData.newPincode],
              ["Plan",        moveSuccessData.planName],
              ["OLT Type",    moveSuccessData.oltType],
            ].map(([label, value]) => (
              <div key={label} style={{ display: "flex", justifyContent: "space-between", fontSize: "13px" }}>
                <span style={{ color: "#6b7280" }}>{label}</span>
                <span style={{ color: "#166534", fontWeight: 600 }}>{value}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── DISCONNECT ── */}
      {view === "disconnect" && (
        <div style={{ marginTop: "32px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "20px" }}>
            <h2 style={{ fontSize: "16px", fontWeight: 600, color: "#111827", margin: 0 }}>Disconnect Connection</h2>
            <button onClick={goBack} style={cancelBtn}>← Back</button>
          </div>

          {/* Search bar — Disconnect */}
          <div style={{ display: "flex", gap: "10px", marginBottom: "10px", alignItems: "center" }}>
            <input
              value={dcSearchName}
              onChange={(e) => { setDcSearchName(e.target.value); setDcSelected(null); setDcConfirming(false); }}
              placeholder="Search by name or customer code..."
              style={{ ...inputStyle, width: "280px" }}
              onFocus={focusBorder} onBlur={blurBorder}
            />
            {dcSearchName && (
              <button onClick={() => { setDcSearchName(""); }} style={cancelBtn}>Clear</button>
            )}
          </div>

          <p style={sectionLabel}>ACTIVE CONNECTIONS — click a row to select</p>
          <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden", marginBottom: "24px" }}>
            {dcConnsLoading ? (
              <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>Loading connections...</p>
            ) : dcConnsError ? (
              <p style={{ padding: "16px", fontSize: "13px", color: "#dc2626" }}>{dcConnsError}</p>
            ) : dcConns.length === 0 ? (
              <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>No active connections found.</p>
            ) : (() => {
              const filtered = filterConns(dcConns, dcSearchName);
              return filtered.length === 0 ? (
                <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>No connections match your search.</p>
              ) : (
                <table style={{ width: "100%", borderCollapse: "collapse" }}>
                  <thead>
                    <tr style={{ background: "#f1f5f9" }}>
                      {["Conn ID", "Cust Code", "Customer", "Pincode", "Plan", "Price", "OLT", "Port"].map((h) => (
                        <th key={h} style={thStyle}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.map((c, i) => {
                      const sel = dcSelected?.connectionId === c.connectionId;
                      return (
                        <tr key={c.connectionId}
                          onClick={() => { setDcSelected(c); setDcConfirming(false); setDcError(""); setDcSuccess(""); }}
                          style={{ background: sel ? "#fee2e2" : i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb", cursor: "pointer" }}>
                          <td style={tdStyle}>{c.connectionId}</td>
                          <td style={tdStyle}>{c.customerCode}</td>
                          <td style={{ ...tdStyle, fontWeight: sel ? 600 : 400 }}>{c.fullName}</td>
                          <td style={tdStyle}>{c.pincode}</td>
                          <td style={tdStyle}>{c.planName}</td>
                          <td style={{ ...tdStyle, color: "#256D85", fontWeight: 500 }}>Rs. {c.monthlyPrice}</td>
                          <td style={tdStyle}>{c.oltType}</td>
                          <td style={{ ...tdStyle, fontSize: "12px", color: "#6b7280" }}>{c.oltCode}/Spl{c.splitterNumber}/Port{c.portNumber}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              );
            })()}
          </div>

          {dcSelected && !dcConfirming && (
            <>
              <div style={{ background: "#fff7ed", border: "1px solid #fed7aa", borderRadius: "4px", padding: "12px 16px", marginBottom: "16px" }}>
                <p style={{ fontSize: "13px", color: "#9a3412", margin: 0 }}>
                  Selected: <strong>{dcSelected.fullName}</strong> ({dcSelected.customerCode}) — Plan: <strong>{dcSelected.planName}</strong> @ Rs.{dcSelected.monthlyPrice}
                </p>
              </div>
              {dcSuccess && <p style={{ fontSize: "13px", color: "#16a34a", marginBottom: "12px" }}>{dcSuccess}</p>}
              <div style={{ display: "flex", gap: "8px" }}>
                <button onClick={() => setDcSelected(null)} style={cancelBtn}>Cancel</button>
                <button
                  onClick={() => setDcConfirming(true)}
                  style={{ ...primaryBtn, background: "#dc2626" }}
                >
                  Disconnect
                </button>
              </div>
            </>
          )}

          {dcSelected && dcConfirming && (
            <div style={{ background: "#fef2f2", border: "1px solid #fecaca", borderRadius: "4px", padding: "16px 20px", maxWidth: "480px" }}>
              <p style={{ fontSize: "14px", fontWeight: 600, color: "#991b1b", margin: "0 0 6px 0" }}>Confirm Disconnection</p>
              <p style={{ fontSize: "13px", color: "#374151", margin: "0 0 4px 0" }}>Connection ID : <strong>{dcSelected.connectionId}</strong></p>
              <p style={{ fontSize: "13px", color: "#374151", margin: "0 0 4px 0" }}>Customer : <strong>{dcSelected.fullName}</strong> ({dcSelected.customerCode})</p>
              <p style={{ fontSize: "13px", color: "#374151", margin: "0 0 4px 0" }}>Service Area : <strong>{dcSelected.pincode}</strong></p>
              <p style={{ fontSize: "13px", color: "#374151", margin: "0 0 16px 0" }}>Port : <strong>{dcSelected.oltCode}/Spl{dcSelected.splitterNumber}/Port{dcSelected.portNumber}</strong></p>
              <p style={{ fontSize: "13px", color: "#dc2626", margin: "0 0 16px 0" }}>This will terminate the connection and release the port. This action cannot be undone.</p>
              {dcError && <p style={{ fontSize: "13px", color: "#dc2626", margin: "0 0 12px 0" }}>{dcError}</p>}
              <div style={{ display: "flex", gap: "8px" }}>
                <button onClick={() => setDcConfirming(false)} style={cancelBtn} disabled={dcLoading}>Cancel</button>
                <button
                  onClick={handleDisconnect}
                  disabled={dcLoading}
                  style={{ ...primaryBtn, background: "#dc2626" }}
                >
                  {dcLoading ? "Disconnecting..." : "Yes, Disconnect"}
                </button>
              </div>
            </div>
          )}

          {!dcSelected && dcSuccess && (
            <p style={{ fontSize: "13px", color: "#16a34a" }}>{dcSuccess}</p>
          )}
        </div>
      )}
    </div>
  );
}
