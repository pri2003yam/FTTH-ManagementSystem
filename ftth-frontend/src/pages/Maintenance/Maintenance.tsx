import { useEffect, useState } from "react";
import PageWrapper from "../../components/layout/PageWrapper";
import Input from "../../components/ui/Input";
import Loader from "../../components/ui/Loader";
import { inventoryService } from "../../services/inventoryService";
import { maintenanceService, type MaintenanceInventoryRow } from "../../services/maintenanceService";

interface PortNode {
  portId: number;
  portNumber: number;
  portStatus: string;
  customerCode: string | null;
  customerName: string | null;
}

interface SplitterNode {
  splitterId: number;
  splitterNumber: number;
  splitterCode: string | null;
  ports: PortNode[];
}

interface OltNode {
  oltId: number;
  oltCode: string;
  oltType: string;
  splitters: SplitterNode[];
}

const container: React.CSSProperties = { padding: "24px", maxWidth: "1200px" };
const searchBar: React.CSSProperties = {
  display: "flex", gap: "12px", alignItems: "flex-end",
  padding: "16px 20px", background: "#f9fafb", border: "1px solid #e5e7eb",
  borderRadius: "10px", marginBottom: "24px",
};
const oltCard: React.CSSProperties = {
  background: "#fff", border: "1px solid #e5e7eb", borderRadius: "10px",
  marginBottom: "20px", overflow: "hidden",
};
const oltHeader: React.CSSProperties = {
  display: "flex", justifyContent: "space-between", alignItems: "center",
  padding: "14px 20px", background: "#1e293b", color: "#fff",
};
const splitterGrid: React.CSSProperties = {
  display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "16px",
  padding: "16px",
};
const thStyle: React.CSSProperties = {
  padding: "8px 12px", textAlign: "left", fontSize: "12px", fontWeight: 600, color: "#6b7280",
};
const tdStyle: React.CSSProperties = { padding: "8px 12px", fontSize: "13px" };
const btnStyle = (bg: string): React.CSSProperties => ({
  padding: "4px 10px", fontSize: "11px", fontWeight: 600, border: "none",
  borderRadius: "4px", cursor: "pointer", color: "#fff", background: bg,
});
const badge = (bg: string, color: string): React.CSSProperties => ({
  display: "inline-block", fontSize: "11px", fontWeight: 600,
  borderRadius: "4px", padding: "2px 8px", background: bg, color,
});
const msgBox = (bg: string, color: string): React.CSSProperties => ({
  padding: "12px 16px", borderRadius: "8px", background: bg, color,
  fontSize: "14px", fontWeight: 500,
});

export default function Maintenance() {
  const [pincode, setPincode] = useState("");
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<"IDLE" | "OK" | "NO_INVENTORY">("IDLE");
  const [message, setMessage] = useState("");
  const [olts, setOlts] = useState<OltNode[]>([]);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const [pincodes, setPincodes] = useState<string[]>([]);

  useEffect(() => {
    inventoryService.getPincodes().then(setPincodes).catch(() => {});
  }, []);

  useEffect(() => {
    if (!pincode || !pincodes.includes(pincode)) {
      setOlts([]);
      setStatus("IDLE");
      return;
    }
    search();
  }, [pincode, pincodes]);

  const search = async () => {
    setLoading(true);
    setStatus("IDLE");
    try {
      const res = await maintenanceService.getInventory(pincode.trim());
      if (res.status === "NO_INVENTORY") {
        setStatus("NO_INVENTORY");
        setMessage(res.message || "No inventory found.");
        setOlts([]);
      } else {
        setStatus("OK");
        setOlts(buildTree(res.data || []));
      }
    } catch {
      setStatus("NO_INVENTORY");
      setMessage("Error fetching inventory.");
      setOlts([]);
    } finally {
      setLoading(false);
    }
  };

  const refresh = () => {
    if (pincode && pincodes.includes(pincode)) search();
  };

  const handlePortAction = async (portId: number, currentStatus: string) => {
    setActionLoading(`port-${portId}`);
    try {
      if (currentStatus === "FAULTY") {
        await maintenanceService.endPortMaintenance(portId);
      } else {
        await maintenanceService.startPortMaintenance(portId);
      }
      await refresh();
    } catch { /* ignore */ }
    setActionLoading(null);
  };

  const handleSplitterAction = async (splitterId: number, onMaint: boolean) => {
    setActionLoading(`spl-${splitterId}`);
    try {
      if (onMaint) {
        await maintenanceService.endSplitterMaintenance(splitterId);
      } else {
        await maintenanceService.startSplitterMaintenance(splitterId);
      }
      await refresh();
    } catch { /* ignore */ }
    setActionLoading(null);
  };

  const handleOltAction = async (oltId: number, onMaint: boolean) => {
    setActionLoading(`olt-${oltId}`);
    try {
      if (onMaint) {
        await maintenanceService.endOltMaintenance(oltId);
      } else {
        await maintenanceService.startOltMaintenance(oltId);
      }
      await refresh();
    } catch { /* ignore */ }
    setActionLoading(null);
  };

  return (
    <PageWrapper title="Maintenance">
      <div style={container}>
        <div style={searchBar}>
          <div style={{ width: "180px" }}>
            <Input
              label="Select Pincode"
              value={pincode}
              onChange={(e) => setPincode(e.target.value)}
              placeholder="Pincode"
              list="maint-pincode-list"
            />
            <datalist id="maint-pincode-list">
              {pincodes.map((p) => <option key={p} value={p} />)}
            </datalist>
          </div>
        </div>

        {loading && <Loader />}

        {!loading && status === "NO_INVENTORY" && (
          <div style={msgBox("#fffbeb", "#92400e")}>{message}</div>
        )}

        {!loading && status === "OK" && olts.map((olt) => {
          const allMaint = olt.splitters.every(s => s.ports.every(p => p.portStatus === "FAULTY"));
          return (
            <div key={olt.oltId} style={oltCard}>
              {/* OLT Header */}
              <div style={oltHeader}>
                <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                  <span style={{ fontWeight: 700, fontSize: "15px" }}>{olt.oltCode}</span>
                  <span style={badge("#334155", "#94a3b8")}>{olt.oltType}</span>
                  {allMaint && <span style={badge("#fbbf24", "#78350f")}>ON MAINTENANCE</span>}
                </div>
                <button
                  onClick={() => handleOltAction(olt.oltId, allMaint)}
                  disabled={actionLoading === `olt-${olt.oltId}`}
                  style={btnStyle(allMaint ? "#16a34a" : "#dc2626")}
                >
                  {actionLoading === `olt-${olt.oltId}` ? "..." : allMaint ? "End OLT Maintenance" : "Put OLT on Maintenance"}
                </button>
              </div>

              {/* Splitter tables in a row */}
              <div style={{ ...splitterGrid, gridTemplateColumns: `repeat(${olt.splitters.length}, 1fr)` }}>
                {olt.splitters.map((spl) => {
                  const splMaint = spl.ports.every(p => p.portStatus === "FAULTY");
                  return (
                    <div key={spl.splitterId} style={{ border: "1px solid #e5e7eb", borderRadius: "8px", overflow: "hidden" }}>
                      {/* Splitter header */}
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "8px 12px", background: "#f1f5f9" }}>
                        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                          <span style={{ fontWeight: 600, fontSize: "13px" }}>Splitter {spl.splitterNumber}</span>
                          {splMaint && <span style={badge("#fef3c7", "#92400e")}>MAINT</span>}
                        </div>
                        <button
                          onClick={() => handleSplitterAction(spl.splitterId, splMaint)}
                          disabled={actionLoading === `spl-${spl.splitterId}`}
                          style={btnStyle(splMaint ? "#16a34a" : "#f59e0b")}
                        >
                          {actionLoading === `spl-${spl.splitterId}` ? "..." : splMaint ? "End" : "Start"}
                        </button>
                      </div>

                      {/* Port table */}
                      <table style={{ width: "100%", borderCollapse: "collapse" }}>
                        <thead>
                          <tr style={{ background: "#f9fafb" }}>
                            <th style={thStyle}>Port</th>
                            <th style={thStyle}>Status</th>
                            <th style={thStyle}>Customer</th>
                            <th style={thStyle}>Action</th>
                          </tr>
                        </thead>
                        <tbody>
                          {spl.ports.map((port) => {
                            const isMaint = port.portStatus === "FAULTY";
                            return (
                              <tr key={port.portId} style={{ borderBottom: "1px solid #f3f4f6" }}>
                                <td style={tdStyle}>
                                  <span style={{
                                    display: "inline-flex", alignItems: "center", justifyContent: "center",
                                    width: "22px", height: "22px", borderRadius: "50%", fontSize: "11px",
                                    fontWeight: 700, color: "#fff",
                                    background: isMaint ? "#f59e0b" : port.portStatus === "ASSIGNED" ? "#dc2626" : "#16a34a",
                                  }}>
                                    {port.portNumber}
                                  </span>
                                </td>
                                <td style={tdStyle}>
                                  <span style={badge(
                                    isMaint ? "#fef3c7" : port.portStatus === "ASSIGNED" ? "#fee2e2" : "#dcfce7",
                                    isMaint ? "#92400e" : port.portStatus === "ASSIGNED" ? "#991b1b" : "#166534",
                                  )}>
                                    {isMaint ? "MAINT" : port.portStatus}
                                  </span>
                                </td>
                                <td style={{ ...tdStyle, fontSize: "12px", color: port.customerCode ? "#111827" : "#9ca3af" }}>
                                  {port.customerCode || "—"}
                                </td>
                                <td style={tdStyle}>
                                  <button
                                    onClick={() => handlePortAction(port.portId, port.portStatus)}
                                    disabled={actionLoading === `port-${port.portId}`}
                                    style={btnStyle(isMaint ? "#16a34a" : "#f59e0b")}
                                  >
                                    {actionLoading === `port-${port.portId}` ? "..." : isMaint ? "End" : "Start"}
                                  </button>
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </PageWrapper>
  );
}

function buildTree(rows: MaintenanceInventoryRow[]): OltNode[] {
  const oltMap = new Map<number, OltNode>();

  for (const r of rows) {
    if (!oltMap.has(r.oltId)) {
      oltMap.set(r.oltId, { oltId: r.oltId, oltCode: r.oltCode, oltType: r.oltType, splitters: [] });
    }
    const olt = oltMap.get(r.oltId)!;

    let spl = olt.splitters.find(s => s.splitterId === r.splitterId);
    if (!spl) {
      spl = { splitterId: r.splitterId, splitterNumber: r.splitterNumber, splitterCode: r.splitterCode, ports: [] };
      olt.splitters.push(spl);
    }

    spl.ports.push({
      portId: r.portId,
      portNumber: r.portNumber,
      portStatus: r.portStatus,
      customerCode: r.customerCode,
      customerName: r.customerName,
    });
  }

  return Array.from(oltMap.values());
}
