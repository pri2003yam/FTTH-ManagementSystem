import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
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
  const location = useLocation();
  const [view, setView] = useState<View>((location.state as any)?.view ?? "menu");

  // â”€â”€ New Install state â”€â”€
  const [plans, setPlans] = useState<Plan[]>([]);
  const [pincodes, setPincodes] = useState<string[]>([]);
  const [selectedPlanId, setSelectedPlanId] = useState<number | null>(null);
  const [niPincodeInput, setNiPincodeInput] = useState("");
  const [niPincodeSuggestions, setNiPincodeSuggestions] = useState<string[]>([]);
  const [niPincodeDropdownOpen, setNiPincodeDropdownOpen] = useState(false);
  const [niPlanSearch, setNiPlanSearch] = useState("");
  const [niForm, setNiForm] = useState({ customerName: "", email: "", salary: "", pincode: "" });
  const [niLoading, setNiLoading] = useState(false);
  const [niDataLoading, setNiDataLoading] = useState(false);
  const [niDataError, setNiDataError] = useState("");
  const [niError, setNiError] = useState("");
  const [niOltTypes, setNiOltTypes] = useState<string[]>([]);
  const [niOltLoading, setNiOltLoading] = useState(false);
  const [niCreatedConn, setNiCreatedConn] = useState<any>(null);

  // â”€â”€ Change Plan state â”€â”€
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

  // â”€â”€ Move state â”€â”€
  const [moveConn, setMoveConn] = useState<ActiveConnection | null>(null);
  const [movePincode, setMovePincode] = useState("");
  const [movePincodeSuggestions, setMovePincodeSuggestions] = useState<string[]>([]);
  const [movePincodeDropdownOpen, setMovePincodeDropdownOpen] = useState(false);
  const [moveSearch, setMoveSearch] = useState("");
  const [moveCheck, setMoveCheck] = useState<{ available: boolean; oltType: string; availablePorts: number; message: string } | null>(null);
  const [moveCheckLoading, setMoveCheckLoading] = useState(false);
  const [moveLoading, setMoveLoading] = useState(false);
  const [moveError, setMoveError] = useState("");
  const [moveSuccess, setMoveSuccess] = useState("");
  const [moveSuccessData, setMoveSuccessData] = useState<any>(null);
  // â”€â”€ Disconnect state â”€â”€
  const [dcConns, setDcConns] = useState<ActiveConnection[]>([]);
  const [dcDisconnectedConns, setDcDisconnectedConns] = useState<ActiveConnection[]>([]);
  const [dcShowDisconnected, setDcShowDisconnected] = useState(false);
  const [dcConnsLoading, setDcConnsLoading] = useState(false);
  const [dcConnsError, setDcConnsError] = useState("");
  const [dcSearchName, setDcSearchName] = useState("");
  const [dcSelected, setDcSelected] = useState<ActiveConnection | null>(null);
  const [dcConfirming, setDcConfirming] = useState(false);
  const [dcReactivating, setDcReactivating] = useState(false);
  const [dcReactivateSuccess, setDcReactivateSuccess] = useState("");
  const [dcLoading, setDcLoading] = useState(false);
  const [dcError, setDcError] = useState("");

  const [dcSuccess, setDcSuccess] = useState("");
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
      setDcShowDisconnected(false);
      setDcReactivateSuccess("");
      Promise.all([
        api.get<ActiveConnection[]>(ENDPOINTS.CONNECTION_ACTIVE),
        api.get<ActiveConnection[]>(ENDPOINTS.CONNECTION_DISCONNECTED),
      ])
        .then(([active, disconnected]) => { setDcConns(active); setDcDisconnectedConns(disconnected); })
        .catch((err) => setDcConnsError(err instanceof Error ? err.message : "Failed to load connections."))
        .finally(() => setDcConnsLoading(false));
    }
  }, [view]);

  // â”€â”€ Load available plans when a connection is selected â”€â”€
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
    setNiPincodeSuggestions([]);
    setNiPincodeDropdownOpen(false);
    setNiPlanSearch("");
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

  const handleReactivate = async (conn: ActiveConnection) => {
    setDcReactivating(true);
    setDcError("");
    setDcReactivateSuccess("");
    try {
      await api.post(ENDPOINTS.CONNECTION_REACTIVATE(conn.connectionId), {});
      setDcReactivateSuccess(`${conn.fullName} (${conn.customerCode}) reactivated successfully.`);
      const [active, disconnected] = await Promise.all([
        api.get<ActiveConnection[]>(ENDPOINTS.CONNECTION_ACTIVE),
        api.get<ActiveConnection[]>(ENDPOINTS.CONNECTION_DISCONNECTED),
      ]);
      setDcConns(active);
      setDcDisconnectedConns(disconnected);
    } catch (err: unknown) {
      setDcError(err instanceof Error ? err.message : "Failed to reactivate.");
    } finally {
      setDcReactivating(false);
    }
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
    setMovePincodeSuggestions([]);
    setMovePincodeDropdownOpen(false);
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

      {/* â”€â”€ NEW INSTALL â”€â”€ */}
      {view === "new-install" && (
        <div style={{ marginTop: "32px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "20px" }}>
            <h2 style={{ fontSize: "16px", fontWeight: 600, color: "#111827", margin: 0 }}>New Install</h2>
            <button onClick={goBack} style={cancelBtn}>â† Back</button>
          </div>

          {niDataLoading ? (
            <p style={{ fontSize: "13px", color: "#6b7280" }}>Loading...</p>
          ) : niDataError ? (
            <p style={{ fontSize: "13px", color: "#dc2626" }}>{niDataError}</p>
          ) : (
            <>
              {/* TOP â”€ Customer Details Form â€” original box size */}
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
                  <Field label="Monthly Salary (Rs.)">
                    <input type="number" value={niForm.salary} onChange={(e) => setNiForm({ ...niForm, salary: e.target.value })}
                      required placeholder="Minimum Rs. 30,000" style={inputStyle} onFocus={focusBorder} onBlur={blurBorder} />
                  </Field>
                  <Field label="Pincode">
                    <div style={{ position: "relative" }}>
                      <input
                        value={niPincodeInput}
                        onChange={(e) => {
                          const val = e.target.value.replace(/\D/g, "").slice(0, 6);
                          setNiPincodeInput(val);
                          setNiForm({ ...niForm, pincode: "" });
                          setNiOltTypes([]);
                          setSelectedPlanId(null);
                          if (val.length >= 4) {
                            const matches = pincodes.filter(p => p.startsWith(val)).slice(0, 8);
                            setNiPincodeSuggestions(matches);
                            setNiPincodeDropdownOpen(matches.length > 0);
                          } else {
                            setNiPincodeSuggestions([]);
                            setNiPincodeDropdownOpen(false);
                          }
                          if (pincodes.includes(val)) handlePincodeSelect(val);
                        }}
                        onBlur={() => setTimeout(() => setNiPincodeDropdownOpen(false), 150)}
                        placeholder="Enter pincode"
                        maxLength={6}
                        style={inputStyle}
                        onFocus={focusBorder}
                        autoComplete="off"
                      />
                      {niPincodeDropdownOpen && (
                        <div style={{
                          position: "absolute", top: "100%", left: 0, right: 0,
                          background: "#fff", border: "1px solid #d1d5db", borderRadius: "4px",
                          boxShadow: "0 4px 12px rgba(0,0,0,0.1)", zIndex: 100,
                          maxHeight: "180px", overflowY: "auto", marginTop: "2px",
                        }}>
                          {niPincodeSuggestions.map((p) => (
                            <div
                              key={p}
                              onMouseDown={() => {
                                setNiPincodeDropdownOpen(false);
                                handlePincodeSelect(p);
                              }}
                              style={{
                                padding: "8px 12px", fontSize: "13px", cursor: "pointer",
                                borderBottom: "1px solid #f3f4f6",
                              }}
                              onMouseEnter={e => (e.currentTarget.style.background = "#f0f9ff")}
                              onMouseLeave={e => (e.currentTarget.style.background = "#fff")}
                            >
                              {p}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
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
                </form>
              </div>

              {/* BOTTOM â”€ Plans shown only after pincode selected */}
              {niForm.pincode && niOltTypes.length > 0 && (
                <>
                  <p style={sectionLabel}>SELECT A PLAN FOR PINCODE {niForm.pincode}</p>
                  
                  {/* Search bar */}
                  <div style={{ marginBottom: "12px" }}>
                    <input
                      type="text"
                      placeholder="Search plans by name, speed, or price..."
                      value={niPlanSearch}
                      onChange={(e) => setNiPlanSearch(e.target.value)}
                      style={{ ...inputStyle, width: "400px" }}
                      onFocus={focusBorder}
                      onBlur={blurBorder}
                    />
                  </div>

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
                          {(() => {
                            const feasiblePlans = plans.filter(p => niOltTypes.includes(p.oltType));
                            const searchQuery = niPlanSearch.toLowerCase();
                            const filteredPlans = searchQuery
                              ? feasiblePlans.filter(p =>
                                  p.planName.toLowerCase().includes(searchQuery) ||
                                  p.speedLabel.toLowerCase().includes(searchQuery) ||
                                  p.monthlyPrice.toString().includes(searchQuery)
                                )
                              : feasiblePlans.slice(0, 5); // Show top 5 by default

                            if (filteredPlans.length === 0) {
                              return (
                                <tr>
                                  <td colSpan={6} style={{ ...tdStyle, textAlign: "center", color: "#6b7280" }}>
                                    {searchQuery ? `No plans match "${niPlanSearch}"` : "No plans available"}
                                  </td>
                                </tr>
                              );
                            }

                            return filteredPlans.map((p, i) => {
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
                            });
                          })()}
                        </tbody>
                      </table>
                    </div>
                  </div>

                  {!niPlanSearch && plans.filter(p => niOltTypes.includes(p.oltType)).length > 5 && (
                    <p style={{ fontSize: "12px", color: "#6b7280", marginTop: "8px" }}>
                      Showing top 5 plans. Use search to find more.
                    </p>
                  )}

                  {/* Selected plan info + action buttons */}
                  {selectedPlanId && (
                    <div style={{ marginTop: "16px", background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "4px", padding: "12px 16px" }}>
                      <p style={{ fontSize: "13px", color: "#166534", margin: "0 0 12px 0", fontWeight: 500 }}>
                        âœ“ Selected Plan: {plans.find(p => p.planId === selectedPlanId)!.planName} â€” Rs.{plans.find(p => p.planId === selectedPlanId)!.monthlyPrice}/mo
                      </p>
                      {niError && <p style={{ ...errText, marginBottom: "12px" }}>{niError}</p>}
                      <div style={{ display: "flex", gap: "8px" }}>
                        <button type="button" onClick={resetNi} style={cancelBtn}>Clear</button>
                        <button
                          onClick={handleNewInstall}
                          disabled={niLoading || !selectedPlanId}
                          style={{ ...primaryBtn, opacity: !selectedPlanId ? 0.5 : 1 }}
                        >
                          {niLoading ? "Creating..." : "Create Connection"}
                        </button>
                      </div>
                    </div>
                  )}
                </>
              )}

              {/* âœ… Green success box â€” same width as form, compact */}
              {niCreatedConn && (
                <div style={{ marginTop: "16px", background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "4px", padding: "12px 16px", maxWidth: "480px" }}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "10px" }}>
                    <p style={{ fontSize: "13px", fontWeight: 600, color: "#166534", margin: 0 }}>âœ… Connection Created Successfully</p>
                    <button onClick={() => setNiCreatedConn(null)} style={cancelBtn}>Done</button>
                  </div>
                  <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                    {[
                      ["Connection ID", `#${niCreatedConn.connectionId}`],
                      ["Customer Code", niCreatedConn.customerCode],
                      ["Customer Name", niCreatedConn.fullName],
                      ["Pincode",       niCreatedConn.pincode],
                      ["Plan",          `${niCreatedConn.planName} â€” Rs.${niCreatedConn.monthlyPrice}/mo`],
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

      {/* â”€â”€ CHANGE PLAN â”€â”€ */}
      {view === "change" && (
        <div style={{ marginTop: "32px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "20px" }}>
            <h2 style={{ fontSize: "16px", fontWeight: 600, color: "#111827", margin: 0 }}>Change Plan</h2>
            <button onClick={goBack} style={cancelBtn}>â† Back</button>
          </div>

          {/* Search bar â€” Change Plan */}
          <div style={{ display: "flex", gap: "10px", marginBottom: "10px", alignItems: "center" }}>
            <input
              value={cpSearchName}
              onChange={(e) => { setCpSearchName(e.target.value); setSelectedConn(null); }}
              placeholder="Search by customer code, name, or pincode..."
              style={{ ...inputStyle, width: "500px" }}
              onFocus={focusBorder} onBlur={blurBorder}
            />
            {cpSearchName && (
              <button onClick={() => { setCpSearchName(""); }} style={cancelBtn}>Clear</button>
            )}
          </div>

          {/* Search Results */}
          {connsLoading && <p style={{ fontSize: "13px", color: "#6b7280" }}>Loading...</p>}
          {connsError && <p style={{ fontSize: "13px", color: "#dc2626" }}>{connsError}</p>}
          {!connsLoading && cpSearchName.length >= 2 && (() => {
            const q = cpSearchName.toLowerCase();
            const filtered = activeConns.filter((c) =>
              c.customerCode.toLowerCase().includes(q) || c.fullName.toLowerCase().includes(q) || c.pincode.includes(q)
            );
            return filtered.length === 0 ? (
              <p style={{ fontSize: "13px", color: "#6b7280" }}>No connections match "{cpSearchName}".</p>
            ) : (
              <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden", marginBottom: "24px", maxHeight: "294px", overflowY: "auto" }}>
                <table style={{ width: "100%", borderCollapse: "collapse" }}>
                  <thead style={{ position: "sticky", top: 0, zIndex: 1 }}>
                    <tr style={{ background: "#f1f5f9" }}>
                      {["Cust Code", "Customer", "Pincode", "Current Plan", "Price", "OLT"].map((h) => (
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
                          <td style={tdStyle}>{c.customerCode}</td>
                          <td style={{ ...tdStyle, fontWeight: sel ? 600 : 400 }}>{c.fullName}</td>
                          <td style={tdStyle}>{c.pincode}</td>
                          <td style={tdStyle}>{c.planName}</td>
                          <td style={{ ...tdStyle, color: "#256D85", fontWeight: 500 }}>Rs. {c.monthlyPrice}</td>
                          <td style={tdStyle}>{c.oltType}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            );
          })()}
          {!connsLoading && cpSearchName.length < 2 && !selectedConn && (
            <p style={{ fontSize: "13px", color: "#9ca3af" }}>Type at least 2 characters to search.</p>
          )}

          {/* Available Plans for selected connection */}
          {selectedConn && (
            <>
              <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "4px", padding: "12px 16px", marginBottom: "16px" }}>
                <p style={{ fontSize: "13px", color: "#166534", margin: 0 }}>
                  Selected: <strong>{selectedConn.fullName}</strong> ({selectedConn.customerCode}) â€” Current plan: <strong>{selectedConn.planName}</strong> @ Rs.{selectedConn.monthlyPrice}
                </p>
              </div>

              <p style={sectionLabel}>AVAILABLE PLANS â€” click a row to select new plan</p>
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
                    Change from <strong>{selectedConn.planName}</strong> â†’ <strong>{selectedNewPlan.planName}</strong> @ Rs.{selectedNewPlan.monthlyPrice}
                    {selectedConn.oltType !== selectedNewPlan.oltType && (
                      <span style={{ color: "#b45309", marginLeft: "8px" }}>âš  OLT type will change, port will be reallocated.</span>
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

      {/* â”€â”€ CHANGE PLAN SUCCESS BOX â”€â”€ */}
      {cpSuccessData && view === "change" && (
        <div style={{ marginTop: "16px", background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "4px", padding: "12px 16px", maxWidth: "480px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "10px" }}>
            <p style={{ fontSize: "13px", fontWeight: 600, color: "#166534", margin: 0 }}>âœ… Plan Changed Successfully</p>
            <button onClick={() => setCpSuccessData(null)} style={cancelBtn}>Done</button>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            {[
              ["Customer",  `${cpSuccessData.fullName} (${cpSuccessData.customerCode})`],
              ["Pincode",   cpSuccessData.pincode],
              ["Old Plan",  cpSuccessData.oldPlan],
              ["New Plan",  `${cpSuccessData.newPlan} â€” Rs.${cpSuccessData.newPrice}/mo`],
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

      {/* â”€â”€ MOVE â”€â”€ */}
      {view === "move" && (
        <div style={{ marginTop: "32px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "20px" }}>
            <h2 style={{ fontSize: "16px", fontWeight: 600, color: "#111827", margin: 0 }}>Move Customer</h2>
            <button onClick={goBack} style={cancelBtn}>â† Back</button>
          </div>

          {/* Search */}
          <div style={{ display: "flex", gap: "10px", marginBottom: "10px", alignItems: "center" }}>
            <input
              value={moveSearch}
              onChange={(e) => setMoveSearch(e.target.value)}
              placeholder="Search by customer code, name, or pincode..."
              style={{ ...inputStyle, width: "500px" }}
              onFocus={focusBorder}
              onBlur={blurBorder}
            />
            {moveSearch && (
              <button onClick={() => setMoveSearch("")} style={cancelBtn}>Clear</button>
            )}
          </div>

          {/* Search Results */}
          {connsLoading && <p style={{ fontSize: "13px", color: "#6b7280" }}>Loading...</p>}
          {connsError && <p style={{ fontSize: "13px", color: "#dc2626" }}>{connsError}</p>}
          {!connsLoading && moveSearch.length >= 2 && (() => {
            const q = moveSearch.toLowerCase();
            const filtered = activeConns.filter((c) =>
              c.customerCode.toLowerCase().includes(q) || c.fullName.toLowerCase().includes(q) || c.pincode.includes(q)
            );
            return filtered.length === 0 ? (
              <p style={{ fontSize: "13px", color: "#6b7280" }}>No connections match "{moveSearch}".</p>
            ) : (
              <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden", marginBottom: "24px", maxHeight: "294px", overflowY: "auto" }}>
                <table style={{ width: "100%", borderCollapse: "collapse" }}>
                  <thead style={{ position: "sticky", top: 0, zIndex: 1 }}>
                    <tr style={{ background: "#f1f5f9" }}>
                      {["Cust Code", "Customer", "Pincode", "Plan", "Price", "OLT"].map((h) => (
                        <th key={h} style={thStyle}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.map((c, i) => {
                      const sel = moveConn?.connectionId === c.connectionId;
                      return (
                        <tr key={c.connectionId} onClick={() => { setMoveConn(c); setMovePincode(""); setMoveCheck(null); setMoveError(""); setMoveSuccess(""); }}
                          style={{ background: sel ? "#e0f2fe" : i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb", cursor: "pointer" }}>
                          <td style={tdStyle}>{c.customerCode}</td>
                          <td style={{ ...tdStyle, fontWeight: sel ? 600 : 400 }}>{c.fullName}</td>
                          <td style={tdStyle}>{c.pincode}</td>
                          <td style={tdStyle}>{c.planName}</td>
                          <td style={{ ...tdStyle, color: "#256D85", fontWeight: 500 }}>Rs. {c.monthlyPrice}</td>
                          <td style={tdStyle}>{c.oltType}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            );
          })()}
          {!connsLoading && moveSearch.length < 2 && !moveConn && (
            <p style={{ fontSize: "13px", color: "#9ca3af" }}>Type at least 2 characters to search.</p>
          )}

          {/* Move form */}
          {moveConn && (
            <>
              <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "4px", padding: "12px 16px", marginBottom: "16px" }}>
                <p style={{ fontSize: "13px", color: "#166534", margin: 0 }}>
                  Selected: <strong>{moveConn.fullName}</strong> ({moveConn.customerCode}) â€” Current pincode: <strong>{moveConn.pincode}</strong> â€” OLT: <strong>{moveConn.oltType}</strong>
                </p>
              </div>

              <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", padding: "24px", maxWidth: "480px", marginBottom: "16px" }}>
                <div style={{ display: "flex", gap: "12px", alignItems: "flex-end" }}>
                  <Field label="New Pincode">
                    <div style={{ position: "relative" }}>
                      <input
                        value={movePincode}
                        onChange={(e) => {
                          const val = e.target.value.replace(/\D/g, "").slice(0, 6);
                          setMovePincode(val);
                          setMoveCheck(null);
                          if (val.length >= 4) {
                            const matches = pincodes.filter(p => p !== moveConn.pincode && p.startsWith(val)).slice(0, 8);
                            setMovePincodeSuggestions(matches);
                            setMovePincodeDropdownOpen(matches.length > 0);
                          } else {
                            setMovePincodeSuggestions([]);
                            setMovePincodeDropdownOpen(false);
                          }
                        }}
                        onBlur={() => setTimeout(() => setMovePincodeDropdownOpen(false), 150)}
                        placeholder="Enter pincode"
                        maxLength={6}
                        style={inputStyle}
                        onFocus={focusBorder}
                        autoComplete="off"
                      />
                      {movePincodeDropdownOpen && (
                        <div style={{
                          position: "absolute", top: "100%", left: 0, right: 0,
                          background: "#fff", border: "1px solid #d1d5db", borderRadius: "4px",
                          boxShadow: "0 4px 12px rgba(0,0,0,0.1)", zIndex: 100,
                          maxHeight: "180px", overflowY: "auto", marginTop: "2px",
                        }}>
                          {movePincodeSuggestions.map((p) => (
                            <div
                              key={p}
                              onMouseDown={() => {
                                setMovePincode(p);
                                setMovePincodeDropdownOpen(false);
                              }}
                              style={{
                                padding: "8px 12px", fontSize: "13px", cursor: "pointer",
                                borderBottom: "1px solid #f3f4f6",
                              }}
                              onMouseEnter={e => (e.currentTarget.style.background = "#f0f9ff")}
                              onMouseLeave={e => (e.currentTarget.style.background = "#fff")}
                            >
                              {p}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
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
                        (Move from <strong>{moveConn.pincode}</strong> â†’ <strong>{movePincode}</strong>)
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

      {/* â”€â”€ MOVE SUCCESS BOX â”€â”€ */}
      {moveSuccessData && view === "move" && (
        <div style={{ marginTop: "16px", background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "4px", padding: "12px 16px", maxWidth: "480px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "10px" }}>
            <p style={{ fontSize: "13px", fontWeight: 600, color: "#166534", margin: 0 }}>âœ… Customer Moved Successfully</p>
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

      {/* â”€â”€ DISCONNECT â”€â”€ */}
      {view === "disconnect" && (
        <div style={{ marginTop: "32px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "20px" }}>
            <h2 style={{ fontSize: "16px", fontWeight: 600, color: "#111827", margin: 0 }}>Disconnect Connection</h2>
            <button onClick={goBack} style={cancelBtn}>← Back</button>
          </div>

          {/* Toggle */}
          <div style={{ display: "flex", gap: "8px", marginBottom: "16px" }}>
            <button
              onClick={() => { setDcShowDisconnected(false); setDcSelected(null); setDcConfirming(false); setDcSearchName(""); setDcReactivateSuccess(""); }}
              style={{ ...cancelBtn, background: !dcShowDisconnected ? "#256D85" : "#ffffff", color: !dcShowDisconnected ? "#ffffff" : "#111827", border: "1px solid #256D85" }}
            >Active Connections</button>
            <button
              onClick={() => { setDcShowDisconnected(true); setDcSelected(null); setDcConfirming(false); setDcSearchName(""); setDcReactivateSuccess(""); }}
              style={{ ...cancelBtn, background: dcShowDisconnected ? "#256D85" : "#ffffff", color: dcShowDisconnected ? "#ffffff" : "#111827", border: "1px solid #256D85" }}
            >Disconnected ({dcDisconnectedConns.length})</button>
          </div>

          {dcConnsLoading && <p style={{ fontSize: "13px", color: "#6b7280" }}>Loading...</p>}
          {dcConnsError && <p style={{ fontSize: "13px", color: "#dc2626" }}>{dcConnsError}</p>}
          {dcReactivateSuccess && <p style={{ fontSize: "13px", color: "#16a34a", marginBottom: "12px" }}>{dcReactivateSuccess}</p>}
          {dcError && !dcSelected && <p style={{ fontSize: "13px", color: "#dc2626", marginBottom: "12px" }}>{dcError}</p>}

          {/* ACTIVE view */}
          {!dcShowDisconnected && (
            <>
              <div style={{ display: "flex", gap: "10px", marginBottom: "10px", alignItems: "center" }}>
                <input
                  value={dcSearchName}
                  onChange={(e) => { setDcSearchName(e.target.value); setDcSelected(null); setDcConfirming(false); }}
                  placeholder="Search by customer code, name, or pincode..."
                  style={{ ...inputStyle, width: "500px" }}
                  onFocus={focusBorder} onBlur={blurBorder}
                />
                {dcSearchName && <button onClick={() => setDcSearchName("")} style={cancelBtn}>Clear</button>}
              </div>
              {!dcConnsLoading && dcSearchName.length >= 2 && (() => {
                const q = dcSearchName.toLowerCase();
                const filtered = dcConns.filter((c) =>
                  c.customerCode.toLowerCase().includes(q) || c.fullName.toLowerCase().includes(q) || c.pincode.includes(q)
                );
                return filtered.length === 0 ? (
                  <p style={{ fontSize: "13px", color: "#6b7280" }}>No connections match "{dcSearchName}".</p>
                ) : (
                  <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden", marginBottom: "24px", maxHeight: "294px", overflowY: "auto" }}>
                    <table style={{ width: "100%", borderCollapse: "collapse" }}>
                      <thead style={{ position: "sticky", top: 0, zIndex: 1 }}>
                        <tr style={{ background: "#f1f5f9" }}>
                          {["Cust Code", "Customer", "Pincode", "Plan", "Price", "OLT"].map((h) => (
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
                              <td style={tdStyle}>{c.customerCode}</td>
                              <td style={{ ...tdStyle, fontWeight: sel ? 600 : 400 }}>{c.fullName}</td>
                              <td style={tdStyle}>{c.pincode}</td>
                              <td style={tdStyle}>{c.planName}</td>
                              <td style={{ ...tdStyle, color: "#256D85", fontWeight: 500 }}>Rs. {c.monthlyPrice}</td>
                              <td style={tdStyle}>{c.oltType}</td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                );
              })()}
              {!dcConnsLoading && dcSearchName.length < 2 && !dcSelected && (
                <p style={{ fontSize: "13px", color: "#9ca3af" }}>Type at least 2 characters to search.</p>
              )}

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
                    <button onClick={() => setDcConfirming(true)} style={{ ...primaryBtn, background: "#dc2626" }}>Disconnect</button>
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
                    <button onClick={handleDisconnect} disabled={dcLoading} style={{ ...primaryBtn, background: "#dc2626" }}>
                      {dcLoading ? "Disconnecting..." : "Yes, Disconnect"}
                    </button>
                  </div>
                </div>
              )}

              {!dcSelected && dcSuccess && (
                <p style={{ fontSize: "13px", color: "#16a34a" }}>{dcSuccess}</p>
              )}
            </>
          )}

          {/* DISCONNECTED view */}
          {dcShowDisconnected && (
            <>
              <div style={{ display: "flex", gap: "10px", marginBottom: "10px", alignItems: "center" }}>
                <input
                  value={dcSearchName}
                  onChange={(e) => setDcSearchName(e.target.value)}
                  placeholder="Search by customer code, name, or pincode..."
                  style={{ ...inputStyle, width: "500px" }}
                  onFocus={focusBorder} onBlur={blurBorder}
                />
                {dcSearchName && <button onClick={() => setDcSearchName("")} style={cancelBtn}>Clear</button>}
              </div>
              {!dcConnsLoading && (() => {
                const q = dcSearchName.toLowerCase();
                const list = dcSearchName.length >= 2
                  ? dcDisconnectedConns.filter(c => c.customerCode.toLowerCase().includes(q) || c.fullName.toLowerCase().includes(q) || c.pincode.includes(q))
                  : dcDisconnectedConns;
                if (dcSearchName.length >= 2 && list.length === 0)
                  return <p style={{ fontSize: "13px", color: "#6b7280" }}>No disconnected connections match "{dcSearchName}".</p>;
                if (list.length === 0)
                  return <p style={{ fontSize: "13px", color: "#9ca3af" }}>No disconnected connections found.</p>;
                return (
                  <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden", maxHeight: "400px", overflowY: "auto" }}>
                    <table style={{ width: "100%", borderCollapse: "collapse" }}>
                      <thead style={{ position: "sticky", top: 0, zIndex: 1 }}>
                        <tr style={{ background: "#f1f5f9" }}>
                          {["Cust Code", "Customer", "Pincode", "Last Plan", "Price", "OLT", "Disconnected On", "Action"].map(h => (
                            <th key={h} style={thStyle}>{h}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {list.map((c, i) => (
                          <tr key={c.connectionId} style={{ background: i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb" }}>
                            <td style={tdStyle}>{c.customerCode}</td>
                            <td style={{ ...tdStyle, fontWeight: 500 }}>{c.fullName}</td>
                            <td style={tdStyle}>{c.pincode}</td>
                            <td style={tdStyle}>{c.planName}</td>
                            <td style={{ ...tdStyle, color: "#256D85", fontWeight: 500 }}>Rs. {c.monthlyPrice}</td>
                            <td style={tdStyle}>{c.oltType}</td>
                            <td style={{ ...tdStyle, color: "#6b7280" }}>{(c as any).disconnectedOn || "-"}</td>
                            <td style={tdStyle}>
                              <button
                                onClick={() => handleReactivate(c)}
                                disabled={dcReactivating}
                                style={{ fontSize: "13px", color: "#16a34a", background: "none", border: "none", cursor: dcReactivating ? "not-allowed" : "pointer", padding: 0, fontWeight: 500 }}
                              >{dcReactivating ? "..." : "Reactivate"}</button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                );
              })()}
            </>
          )}
        </div>
      )}
    </div>
  );
}
