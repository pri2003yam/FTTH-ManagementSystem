import { useEffect, useState } from "react";
import { api } from "../../services/apiClient";

interface EcmOffering {
  itemCode: string;
  name: string;
  description: string;
  status: string;
  startDate: string;
  lastUpdated: string;
  attributes: { code: string; name: string; value: string }[];
  charges: { chargeCode: string; chargeType: string; name: string; value: number; status: string }[];
}

export default function Catalog() {
  const [offerings, setOfferings] = useState<EcmOffering[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedOffering, setSelectedOffering] = useState<EcmOffering | null>(null);
  const [search, setSearch] = useState("");

  useEffect(() => {
    api.get<EcmOffering[]>("/api/ecm/offerings")
      .then(data => { setOfferings(data); setLoading(false); })
      .catch(err => { setError(err.message || "Failed to connect to ECM"); setLoading(false); });
  }, []);

  if (loading) return <div style={{ padding: 40, textAlign: "center", color: "#6b7280" }}>Loading ECM catalog...</div>;

  const filtered = offerings.filter(o =>
    !search || o.name.toLowerCase().includes(search.toLowerCase()) || o.itemCode.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div style={{ fontFamily: "'Source Sans 3', 'Segoe UI', sans-serif", padding: "24px" }}>
      {/* Header */}
      <div style={{ background: "linear-gradient(135deg, #1e293b, #334155)", borderRadius: 12, padding: "20px 24px", marginBottom: 24, color: "#fff" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div>
            <p style={{ fontSize: 11, color: "#94a3b8", margin: 0, textTransform: "uppercase", letterSpacing: 1 }}>Ericsson Catalog Manager — ECM</p>
            <h1 style={{ fontSize: 20, fontWeight: 700, margin: "4px 0 0", color: "#fff" }}>Product Catalog (Live from ECM)</h1>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span style={{ width: 8, height: 8, borderRadius: "50%", background: error ? "#ef4444" : "#22c55e" }} />
            <span style={{ fontSize: 12, color: error ? "#fca5a5" : "#a5f3fc" }}>{error ? "ECM Disconnected" : "ECM Connected"}</span>
          </div>
        </div>
        <div style={{ display: "flex", gap: 24, marginTop: 16 }}>
          <div><p style={{ fontSize: 11, color: "#94a3b8", margin: 0 }}>Product Offerings</p><p style={{ fontSize: 18, fontWeight: 700, margin: "2px 0 0", color: "#fff" }}>{offerings.length}</p></div>
          <div><p style={{ fontSize: 11, color: "#94a3b8", margin: 0 }}>Project</p><p style={{ fontSize: 18, fontWeight: 700, margin: "2px 0 0", color: "#fff" }}>FTTHplans</p></div>
          <div><p style={{ fontSize: 11, color: "#94a3b8", margin: 0 }}>Source</p><p style={{ fontSize: 18, fontWeight: 700, margin: "2px 0 0", color: "#fff" }}>ECM DB (PostgreSQL:5444)</p></div>
        </div>
      </div>

      {error && (
        <div style={{ padding: "14px 18px", background: "#fef2f2", border: "1px solid #fecaca", borderRadius: 8, marginBottom: 20, color: "#dc2626", fontSize: 13 }}>
          ⚠️ {error} — Make sure Velocity Studio is running and ECM database is accessible.
        </div>
      )}

      {/* Search + Actions */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search offerings by name or code..."
          style={{ padding: "8px 12px", border: "1px solid #d1d5db", borderRadius: 6, fontSize: 13, width: 300 }} />
        <div style={{ display: "flex", gap: 8 }}>
          <button onClick={() => { setLoading(true); api.get<EcmOffering[]>("/api/ecm/offerings").then(data => { setOfferings(data); setLoading(false); }); }}
            style={{ padding: "8px 16px", background: "#f1f5f9", border: "1px solid #e2e8f0", borderRadius: 6, fontSize: 12, cursor: "pointer" }}>🔄 Refresh</button>
        </div>
      </div>

      {/* Offerings Table */}
      {filtered.length === 0 ? (
        <div style={{ textAlign: "center", padding: 40, color: "#64748b", border: "1px solid #e2e8f0", borderRadius: 8 }}>
          <p style={{ fontSize: 16, margin: "0 0 8px" }}>No Product Offerings found</p>
          <p style={{ fontSize: 13, margin: 0 }}>Create Product Offerings in Velocity Studio (ECM) and they will appear here automatically.</p>
        </div>
      ) : (
        <div style={{ border: "1px solid #e2e8f0", borderRadius: 8, overflow: "hidden" }}>
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead><tr style={{ background: "#f8fafc" }}>
              <th style={{ padding: "10px 12px", fontSize: 12, fontWeight: 600, color: "#64748b", textAlign: "left" }}>Item Code</th>
              <th style={{ padding: "10px 12px", fontSize: 12, fontWeight: 600, color: "#64748b", textAlign: "left" }}>Name</th>
              <th style={{ padding: "10px 12px", fontSize: 12, fontWeight: 600, color: "#64748b", textAlign: "left" }}>Description</th>
              <th style={{ padding: "10px 12px", fontSize: 12, fontWeight: 600, color: "#64748b", textAlign: "left" }}>Status</th>
              <th style={{ padding: "10px 12px", fontSize: 12, fontWeight: 600, color: "#64748b", textAlign: "left" }}>Attributes</th>
              <th style={{ padding: "10px 12px", fontSize: 12, fontWeight: 600, color: "#64748b", textAlign: "left" }}>Charges</th>
              <th style={{ padding: "10px 12px", fontSize: 12, fontWeight: 600, color: "#64748b", textAlign: "left" }}>Last Updated</th>
            </tr></thead>
            <tbody>
              {filtered.map((o, i) => (
                <tr key={o.itemCode} onClick={() => setSelectedOffering(o)}
                  style={{ background: i % 2 ? "#fafafa" : "#fff", cursor: "pointer", borderTop: "1px solid #f1f5f9" }}
                  onMouseEnter={e => e.currentTarget.style.background = "#f0f9ff"}
                  onMouseLeave={e => e.currentTarget.style.background = i % 2 ? "#fafafa" : "#fff"}>
                  <td style={{ padding: "10px 12px", fontSize: 13 }}><code style={{ fontSize: 12, color: "#6366f1" }}>{o.itemCode}</code></td>
                  <td style={{ padding: "10px 12px", fontSize: 13, fontWeight: 500 }}>{o.name}</td>
                  <td style={{ padding: "10px 12px", fontSize: 12, color: "#64748b" }}>{o.description || "—"}</td>
                  <td style={{ padding: "10px 12px", fontSize: 13 }}><StatusBadge status={o.status} /></td>
                  <td style={{ padding: "10px 12px", fontSize: 12 }}>{o.attributes.length}</td>
                  <td style={{ padding: "10px 12px", fontSize: 12 }}>
                    {o.charges.length > 0
                      ? <span style={{ color: "#059669", fontWeight: 500 }}>₹{o.charges.reduce((s, c) => s + c.value, 0)}</span>
                      : <span style={{ color: "#9ca3af" }}>—</span>}
                  </td>
                  <td style={{ padding: "10px 12px", fontSize: 11, color: "#94a3b8" }}>{o.lastUpdated ? new Date(o.lastUpdated).toLocaleDateString() : "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Info banner */}
      <div style={{ marginTop: 20, padding: "12px 16px", background: "#f0f9ff", border: "1px solid #bae6fd", borderRadius: 8, fontSize: 13, color: "#0369a1" }}>
        💡 Product Offerings are managed in <b>Ericsson Catalog Manager (Velocity Studio)</b>. Changes made there are reflected here in real-time via the ECM database connection.
      </div>

      {/* Detail Modal */}
      {selectedOffering && <DetailModal offering={selectedOffering} onClose={() => setSelectedOffering(null)} />}
    </div>
  );
}

/* Detail Modal */
function DetailModal({ offering, onClose }: { offering: EcmOffering; onClose: () => void }) {
  return (
    <div onClick={onClose} style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.5)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000 }}>
      <div onClick={e => e.stopPropagation()} style={{ background: "#fff", borderRadius: 12, width: "100%", maxWidth: 520, overflow: "hidden", maxHeight: "80vh", overflowY: "auto" }}>
        <div style={{ background: "#1e293b", padding: "20px 24px", color: "#fff" }}>
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <div>
              <p style={{ fontSize: 11, color: "#94a3b8", margin: 0 }}>ECM PRODUCT OFFERING</p>
              <h2 style={{ fontSize: 20, fontWeight: 700, margin: "4px 0 0" }}>{offering.name}</h2>
              <code style={{ fontSize: 12, color: "#a5b4fc" }}>{offering.itemCode}</code>
            </div>
            <button onClick={onClose} style={{ background: "none", border: "none", color: "#94a3b8", fontSize: 20, cursor: "pointer" }}>✕</button>
          </div>
        </div>
        <div style={{ padding: 24 }}>
          <Section title="General">
            <InfoRow label="Item Code" value={offering.itemCode} />
            <InfoRow label="Status" value={offering.status === "DEF" ? "Draft" : offering.status === "ACT" ? "Active" : offering.status} />
            <InfoRow label="Description" value={offering.description || "—"} />
            <InfoRow label="Last Updated" value={offering.lastUpdated ? new Date(offering.lastUpdated).toLocaleString() : "—"} />
          </Section>

          {offering.attributes.length > 0 && (
            <Section title={`Characteristics (${offering.attributes.length})`}>
              {offering.attributes.map((a, i) => <InfoRow key={i} label={a.name || a.code} value={a.value || "—"} />)}
            </Section>
          )}

          {offering.charges.length > 0 && (
            <Section title={`Charges (${offering.charges.length})`}>
              {offering.charges.map((c, i) => (
                <InfoRow key={i} label={c.name} value={`₹${c.value} (${c.chargeType})`} />
              ))}
            </Section>
          )}

          {offering.attributes.length === 0 && offering.charges.length === 0 && (
            <p style={{ fontSize: 13, color: "#94a3b8", textAlign: "center", padding: 16 }}>
              No attributes or charges configured yet. Add them in Velocity Studio.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

/* Shared Components */
function StatusBadge({ status }: { status: string }) {
  const map: Record<string, { bg: string; color: string; text: string }> = {
    ACT: { bg: "#ecfdf5", color: "#059669", text: "Active" },
    DEF: { bg: "#fefce8", color: "#ca8a04", text: "Draft" },
    RET: { bg: "#fef2f2", color: "#dc2626", text: "Retired" },
  };
  const s = map[status] || { bg: "#f1f5f9", color: "#64748b", text: status };
  return <span style={{ fontSize: 11, padding: "2px 8px", borderRadius: 10, fontWeight: 500, background: s.bg, color: s.color }}>{s.text}</span>;
}
function InfoRow({ label, value }: { label: string; value: string }) {
  return <div style={{ display: "flex", justifyContent: "space-between", padding: "6px 0", borderBottom: "1px solid #f8fafc", fontSize: 13 }}><span style={{ color: "#64748b" }}>{label}</span><span style={{ fontWeight: 500, color: "#1e293b" }}>{value}</span></div>;
}
function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return <div style={{ marginBottom: 16 }}><h4 style={{ fontSize: 12, fontWeight: 600, color: "#94a3b8", margin: "0 0 8px", textTransform: "uppercase", letterSpacing: 0.5 }}>{title}</h4>{children}</div>;
}
