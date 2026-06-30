import { useEffect, useState } from "react";
import PageWrapper from "../../components/layout/PageWrapper";
import Button from "../../components/ui/Button";
import Badge from "../../components/ui/Badge";
import Loader from "../../components/ui/Loader";
import Modal from "../../components/ui/Modal";
import Input from "../../components/ui/Input";
import Select from "../../components/ui/Select";
import { useAuth } from "../../context/AuthContext";
import { workRequestService } from "../../services/workRequestService";
import { inventoryService } from "../../services/inventoryService";
import type { WorkRequest, WorkRequestStatus, WorkRequestActionType, OltInventoryDTO, OltDetail } from "../../types/models";

const statusVariant: Record<WorkRequestStatus, "success" | "error" | "warning" | "neutral"> = {
  NEW: "warning",
  ACCEPTED: "warning",
  IN_PROGRESS: "error",
  RESOLVED: "success",
  CLOSED: "neutral",
};

const ACTION_LABELS: Record<WorkRequestActionType, string> = {
  ADD_OLT: "Add OLT",
  REMOVE_OLT: "Remove OLT",
  ADD_SPLITTER: "Add Splitter",
  REMOVE_SPLITTER: "Remove Splitter",
};

export default function WorkRequests() {
  const { user, role } = useAuth();
  const [requests, setRequests] = useState<WorkRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [pincodes, setPincodes] = useState<string[]>([]);
  const [actionLoading, setActionLoading] = useState<number | null>(null);

  // Create form
  const [pincode, setPincode] = useState("");
  const [oltType, setOltType] = useState("OLT300");
  const [actionType, setActionType] = useState<string>("ADD_OLT");
  const [description, setDescription] = useState("");
  const [createError, setCreateError] = useState("");

  // Fulfill modal state
  const [fulfillWr, setFulfillWr] = useState<WorkRequest | null>(null);
  const [splitterCount, setSplitterCount] = useState("1");
  const [maxSplitters, setMaxSplitters] = useState(3);
  const [fulfillLoading, setFulfillLoading] = useState(false);
  const [fulfillMsg, setFulfillMsg] = useState("");
  const [fulfillError, setFulfillError] = useState("");

  // For Remove OLT / Add Splitter / Remove Splitter — need OLT list
  const [oltsForAction, setOltsForAction] = useState<OltInventoryDTO[]>([]);
  const [selectedOltCode, setSelectedOltCode] = useState("");
  const [oltDetail, setOltDetail] = useState<OltDetail | null>(null);
  const [selectedSplitterNum, setSelectedSplitterNum] = useState("");

  useEffect(() => {
    load();
    inventoryService.getPincodes().then(setPincodes).catch(() => {});
    inventoryService.getConfig().then((c) => setMaxSplitters(c.maxSplitters)).catch(() => {});
  }, []);

  const load = async () => {
    setLoading(true);
    try {
      const data = role === "CSR"
        ? await workRequestService.getAll(user!)
        : await workRequestService.getAll();
      setRequests(data);
    } catch { /* ignore */ }
    setLoading(false);
  };

  const handleCreate = async () => {
    setCreateError("");
    if (!pincode || !oltType || !actionType) { setCreateError("All fields are required."); return; }
    try {
      await workRequestService.create({ pincode, oltType, actionType, raisedBy: user!, description });
      setShowCreate(false);
      setPincode(""); setOltType("OLT300"); setActionType("ADD_OLT"); setDescription("");
      await load();
    } catch (e: any) {
      setCreateError(e.message || "Failed to create.");
    }
  };

  const handleTransition = async (wrId: number, newStatus: WorkRequestStatus) => {
    setActionLoading(wrId);
    try {
      await workRequestService.transition(wrId, { newStatus, actor: user! });
      await load();
    } catch { /* ignore */ }
    setActionLoading(null);
  };

  // ─── Fulfill Actions ───────────────────────────────────────

  const openFulfill = async (wr: WorkRequest) => {
    setFulfillWr(wr);
    setSplitterCount("1");
    setFulfillMsg("");
    setFulfillError("");
    setSelectedOltCode("");
    setOltDetail(null);
    setSelectedSplitterNum("");
    setOltsForAction([]);

    // For actions that need OLT list, fetch them
    if (wr.actionType !== "ADD_OLT") {
      try {
        const olts = await inventoryService.getOltsByPincode(wr.pincode);
        // Filter by OLT type for the request
        setOltsForAction(olts.filter(o => o.oltType === wr.oltType));
      } catch { /* ignore */ }
    }
  };

  const handleOltSelect = async (oltCode: string) => {
    setSelectedOltCode(oltCode);
    setOltDetail(null);
    setSelectedSplitterNum("");
    if (oltCode && fulfillWr?.actionType === "REMOVE_SPLITTER") {
      try {
        const detail = await inventoryService.getOltDetails(oltCode);
        setOltDetail(detail);
      } catch { /* ignore */ }
    }
  };

  const handleFulfill = async () => {
    if (!fulfillWr) return;
    setFulfillLoading(true);
    setFulfillError("");
    setFulfillMsg("");

    try {
      switch (fulfillWr.actionType) {
        case "ADD_OLT": {
          const res = await inventoryService.addOlt({
            pincode: fulfillWr.pincode,
            oltType: fulfillWr.oltType,
            splitterCount: parseInt(splitterCount),
          });
          setFulfillMsg(`OLT ${res.oltCode} added successfully!`);
          break;
        }
        case "REMOVE_OLT": {
          if (!selectedOltCode) { setFulfillError("Select an OLT to remove."); setFulfillLoading(false); return; }
          await inventoryService.removeOlt(selectedOltCode);
          setFulfillMsg(`OLT ${selectedOltCode} removed successfully!`);
          break;
        }
        case "ADD_SPLITTER": {
          if (!selectedOltCode) { setFulfillError("Select an OLT."); setFulfillLoading(false); return; }
          await inventoryService.addSplitter(selectedOltCode);
          setFulfillMsg(`Splitter added to ${selectedOltCode}!`);
          break;
        }
        case "REMOVE_SPLITTER": {
          if (!selectedOltCode || !selectedSplitterNum) { setFulfillError("Select OLT and Splitter."); setFulfillLoading(false); return; }
          await inventoryService.removeSplitter(selectedOltCode, parseInt(selectedSplitterNum));
          setFulfillMsg(`Splitter #${selectedSplitterNum} removed from ${selectedOltCode}!`);
          break;
        }
      }

      // Auto-resolve the work request
      await workRequestService.transition(fulfillWr.wrId, { newStatus: "RESOLVED", actor: user! });
      setFulfillMsg((prev) => prev + " Request resolved.");
      setTimeout(() => { setFulfillWr(null); load(); }, 1200);
    } catch (e: any) {
      setFulfillError(e.message || "Action failed.");
    } finally {
      setFulfillLoading(false);
    }
  };

  // ─── Action Buttons ────────────────────────────────────────

  const getActions = (wr: WorkRequest) => {
    const actions: { label: string; status: WorkRequestStatus; variant: "primary" | "danger" | "outline" }[] = [];

    if (role === "MAINT") {
      if (wr.status === "NEW") actions.push({ label: "Accept", status: "ACCEPTED", variant: "primary" });
      if (wr.status === "ACCEPTED" && wr.assignedTo === user) {
        actions.push({ label: "Start Work", status: "IN_PROGRESS", variant: "primary" });
        actions.push({ label: "Release", status: "NEW", variant: "danger" });
      }
      if (wr.status === "IN_PROGRESS" && wr.assignedTo === user) {
        actions.push({ label: "Resolve (Manual)", status: "RESOLVED", variant: "outline" });
      }
      if (wr.status === "RESOLVED" && wr.assignedTo === user) actions.push({ label: "Close", status: "CLOSED", variant: "primary" });
    }

    if (role === "CSR" && wr.status === "NEW" && wr.raisedBy === user) {
      actions.push({ label: "Cancel", status: "CLOSED", variant: "danger" });
    }

    return actions;
  };

  const splitterOptions = Array.from({ length: maxSplitters }, (_, i) => ({
    value: String(i + 1),
    label: String(i + 1),
  }));

  // ─── Render ────────────────────────────────────────────────

  return (
    <PageWrapper title="Work Requests">
      <div style={{ padding: "24px", maxWidth: "1200px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
          <h2 style={{ margin: 0, fontSize: "18px", fontWeight: 600 }}>
            {role === "CSR" ? "My Work Requests" : "All Work Requests"}
          </h2>
          {role === "CSR" && (
            <Button variant="primary" onClick={() => setShowCreate(true)}>+ Raise Request</Button>
          )}
        </div>

        {loading && <Loader />}

        {!loading && requests.length === 0 && (
          <div style={{ padding: "40px", textAlign: "center", color: "#6b7280" }}>No work requests found.</div>
        )}

        {!loading && requests.length > 0 && (
          <table style={{ width: "100%", borderCollapse: "collapse", background: "#fff", borderRadius: "8px", overflow: "hidden", border: "1px solid #e5e7eb" }}>
            <thead>
              <tr style={{ background: "#f9fafb" }}>
                <th style={th}>#</th>
                <th style={th}>Pincode</th>
                <th style={th}>OLT Type</th>
                <th style={th}>Action</th>
                <th style={th}>Status</th>
                <th style={th}>Raised By</th>
                <th style={th}>Assigned To</th>
                <th style={th}>Created</th>
                <th style={th}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((wr) => (
                <tr key={wr.wrId} style={{ borderBottom: "1px solid #f3f4f6" }}>
                  <td style={td}>{wr.wrId}</td>
                  <td style={td}>{wr.pincode}</td>
                  <td style={td}>{wr.oltType}</td>
                  <td style={td}>
                    <span style={{ fontSize: "11px", fontWeight: 600, background: "#e0e7ff", color: "#3730a3", borderRadius: "4px", padding: "2px 6px" }}>
                      {ACTION_LABELS[wr.actionType] || wr.actionType}
                    </span>
                  </td>
                  <td style={td}><Badge label={wr.status.replace("_", " ")} variant={statusVariant[wr.status]} /></td>
                  <td style={td}>{wr.raisedBy}</td>
                  <td style={td}>{wr.assignedTo || "—"}</td>
                  <td style={td}>{new Date(wr.createdAt).toLocaleDateString()}</td>
                  <td style={td}>
                    <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
                      {/* Fulfill button when IN_PROGRESS */}
                      {role === "MAINT" && wr.status === "IN_PROGRESS" && wr.assignedTo === user && (
                        <Button
                          variant="primary"
                          onClick={() => openFulfill(wr)}
                          disabled={actionLoading === wr.wrId}
                          className="text-xs px-2 py-1"
                        >
                          {ACTION_LABELS[wr.actionType]}
                        </Button>
                      )}
                      {getActions(wr).map((a) => (
                        <Button
                          key={a.status}
                          variant={a.variant}
                          onClick={() => handleTransition(wr.wrId, a.status)}
                          disabled={actionLoading === wr.wrId}
                          className="text-xs px-2 py-1"
                        >
                          {actionLoading === wr.wrId ? "..." : a.label}
                        </Button>
                      ))}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {/* ─── Create Modal ─── */}
        <Modal
          open={showCreate}
          title="Raise Work Request"
          onConfirm={handleCreate}
          onCancel={() => { setShowCreate(false); setCreateError(""); }}
          confirmLabel="Submit"
        >
          <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
            <Select
              label="Action Type"
              value={actionType}
              onChange={(e) => setActionType(e.target.value)}
              options={[
                { label: "Add OLT", value: "ADD_OLT" },
                { label: "Remove OLT", value: "REMOVE_OLT" },
                { label: "Add Splitter", value: "ADD_SPLITTER" },
                { label: "Remove Splitter", value: "REMOVE_SPLITTER" },
              ]}
            />

            <Input
              label="Pincode"
              value={pincode}
              onChange={(e) => setPincode(e.target.value)}
              placeholder="Enter pincode"
              list="wr-pincode-list"
            />
            <datalist id="wr-pincode-list">
              {pincodes.map((p) => <option key={p} value={p} />)}
            </datalist>

            <Select
              label="OLT Type"
              value={oltType}
              onChange={(e) => setOltType(e.target.value)}
              options={[
                { label: "OLT300", value: "OLT300" },
                { label: "OLT500", value: "OLT500" },
              ]}
            />

            <Input
              label="Description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe the requirement..."
            />

            {createError && <div style={{ color: "#dc2626", fontSize: "13px" }}>{createError}</div>}
          </div>
        </Modal>

        {/* ─── Fulfill Modal ─── */}
        <Modal
          open={!!fulfillWr}
          title={`Fulfill — ${fulfillWr ? ACTION_LABELS[fulfillWr.actionType] : ""}`}
          onConfirm={handleFulfill}
          onCancel={() => setFulfillWr(null)}
          confirmLabel={fulfillLoading ? "Processing..." : `${fulfillWr ? ACTION_LABELS[fulfillWr.actionType] : ""} & Resolve`}
        >
          {fulfillWr && (
            <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              <div style={{ background: "#f0f9ff", border: "1px solid #bae6fd", borderRadius: "6px", padding: "12px", fontSize: "13px" }}>
                <strong>WR #{fulfillWr.wrId}</strong> — {ACTION_LABELS[fulfillWr.actionType]} for <strong>{fulfillWr.oltType}</strong> at pincode <strong>{fulfillWr.pincode}</strong>
                {fulfillWr.description && <div style={{ marginTop: "4px", color: "#6b7280" }}>{fulfillWr.description}</div>}
              </div>

              {/* ADD_OLT: just pick splitter count */}
              {fulfillWr.actionType === "ADD_OLT" && (
                <>
                  <Input label="Pincode" value={fulfillWr.pincode} disabled onChange={() => {}} />
                  <Input label="OLT Type" value={fulfillWr.oltType} disabled onChange={() => {}} />
                  <Select
                    label="Splitter Count"
                    value={splitterCount}
                    onChange={(e) => setSplitterCount(e.target.value)}
                    options={splitterOptions}
                  />
                </>
              )}

              {/* REMOVE_OLT: pick which OLT to remove */}
              {fulfillWr.actionType === "REMOVE_OLT" && (
                <Select
                  label="Select OLT to Remove"
                  value={selectedOltCode}
                  onChange={(e) => setSelectedOltCode(e.target.value)}
                  options={oltsForAction.map(o => ({ value: o.oltCode, label: `${o.oltCode} (${o.splitterCount} splitters, ${o.availablePorts}/${o.totalPorts} free)` }))}
                />
              )}

              {/* ADD_SPLITTER: pick which OLT to add splitter to */}
              {fulfillWr.actionType === "ADD_SPLITTER" && (
                <Select
                  label="Select OLT"
                  value={selectedOltCode}
                  onChange={(e) => setSelectedOltCode(e.target.value)}
                  options={oltsForAction.map(o => ({ value: o.oltCode, label: `${o.oltCode} (${o.splitterCount}/${maxSplitters} splitters)` }))}
                />
              )}

              {/* REMOVE_SPLITTER: pick OLT then splitter */}
              {fulfillWr.actionType === "REMOVE_SPLITTER" && (
                <>
                  <Select
                    label="Select OLT"
                    value={selectedOltCode}
                    onChange={(e) => handleOltSelect(e.target.value)}
                    options={oltsForAction.map(o => ({ value: o.oltCode, label: `${o.oltCode} (${o.splitterCount} splitters)` }))}
                  />
                  {oltDetail && (
                    <Select
                      label="Select Splitter to Remove"
                      value={selectedSplitterNum}
                      onChange={(e) => setSelectedSplitterNum(e.target.value)}
                      options={oltDetail.splitters.map(s => ({
                        value: String(s.splitterNumber),
                        label: `Splitter #${s.splitterNumber} (${s.availablePorts}/${s.totalPorts} free)`,
                      }))}
                    />
                  )}
                </>
              )}

              {fulfillMsg && <div style={{ color: "#059669", fontSize: "13px", fontWeight: 500 }}>{fulfillMsg}</div>}
              {fulfillError && <div style={{ color: "#dc2626", fontSize: "13px" }}>{fulfillError}</div>}
            </div>
          )}
        </Modal>
      </div>
    </PageWrapper>
  );
}

const th: React.CSSProperties = { padding: "10px 12px", textAlign: "left", fontSize: "12px", fontWeight: 600, color: "#6b7280" };
const td: React.CSSProperties = { padding: "10px 12px", fontSize: "13px" };
