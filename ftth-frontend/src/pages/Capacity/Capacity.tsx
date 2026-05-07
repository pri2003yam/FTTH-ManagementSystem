import { useEffect, useState } from "react";
import PageWrapper from "../../components/layout/PageWrapper";
import Badge from "../../components/ui/Badge";
import Loader from "../../components/ui/Loader";
import { api } from "../../services/apiClient";
import {
  inputStyle, cancelBtn, thStyle, tdStyle,
  focusBorder, blurBorder,
} from "../Users/UsersShared";

interface CapacityOlt {
  pincode: string;
  oltCode: string;
  oltType: string;
  splitterCount: number;
  totalPorts: number;
  usedPorts: number;
  freePorts: number;
  utilPercent: number;
  breach: boolean;
  warning: "CAPACITY_BREACH" | "ADD_SPLITTER" | "ADD_OLT" | null;
}

interface CapacityResponse {
  threshold: number;
  totalOlts: number;
  breachCount: number;
  addSplitterCount: number;
  addOltCount: number;
  olts: CapacityOlt[];
}

type SortKey = "utilPercent" | "usedPorts" | "freePorts" | "totalPorts";
type SortDir = "asc" | "desc";

export default function Capacity() {
  const [data, setData] = useState<CapacityResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [filterOlt, setFilterOlt] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("utilPercent");
  const [sortDir, setSortDir] = useState<SortDir>("desc");
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 10;

  useEffect(() => {
    api.get<CapacityResponse>("/api/capacity")
      .then(setData)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Loader />;
  if (!data) return null;

  const oltTypes = [...new Set(data.olts.map(o => o.oltType))];

  const filtered = data.olts
    .filter(o => {
      const q = search.toLowerCase();
      const matchSearch = !q ||
        o.pincode.includes(q) ||
        o.oltCode.toLowerCase().includes(q) ||
        o.oltType.toLowerCase().includes(q);
      const matchOlt = !filterOlt || o.oltType === filterOlt;
      return matchSearch && matchOlt;
    })
    .sort((a, b) => {
      const av = a[sortKey] as number;
      const bv = b[sortKey] as number;
      return sortDir === "asc" ? av - bv : bv - av;
    });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const displayed = filtered.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  const handleSort = (key: SortKey) => {
    if (sortKey === key) setSortDir(d => d === "asc" ? "desc" : "asc");
    else { setSortKey(key); setSortDir("desc"); }
    setPage(1);
  };
  const arrow = (key: SortKey) => sortKey === key ? (sortDir === "asc" ? " ↑" : " ↓") : "";

  const statusBadge = (o: CapacityOlt) => {
    if (o.warning === "ADD_OLT")        return <Badge label="Add OLT" variant="error" />;
    if (o.warning === "ADD_SPLITTER")   return <Badge label="Add Splitter" variant="warning" />;
    if (o.warning === "CAPACITY_BREACH") return <Badge label="Breach" variant="warning" />;
    return <Badge label="Normal" variant="success" />;
  };

  return (
    <PageWrapper title="Capacity Dashboard">
      {/* Summary Cards */}
      <div style={{ display: "flex", gap: "16px" }}>
        {[
          { label: "Threshold", value: `${data.threshold}%` },
          { label: "Total OLTs", value: data.totalOlts },
          { label: "Breaches", value: data.breachCount, color: "#f59e0b" },
        ].map((c) => (
          <div key={c.label} style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", padding: "24px 20px", flex: 1 }}>
            <div style={{ fontSize: "13px", color: "#6b7280" }}>{c.label}</div>
            <div style={{ fontSize: "20px", fontWeight: 600, color: c.color ?? "inherit" }}>{c.value}</div>
          </div>
        ))}
        <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", padding: "24px 20px", flex: 1 }}>
          <div style={{ fontSize: "13px", color: "#6b7280" }}>Expansion Needed</div>
          <div style={{ fontSize: "13px", marginTop: "4px" }}>Splitters: <b>{data.addSplitterCount}</b></div>
          <div style={{ fontSize: "13px" }}>OLTs: <b>{data.addOltCount}</b></div>
        </div>
      </div>

      {/* Filters */}
      <div style={{ display: "flex", gap: "10px", alignItems: "center", flexWrap: "wrap", margin: "20px 0 12px" }}>
        <input
          value={search}
          onChange={e => { setSearch(e.target.value); setPage(1); }}
          placeholder="Search by pincode, OLT code, or OLT type..."
          style={{ ...inputStyle, width: "320px" }}
          onFocus={focusBorder} onBlur={blurBorder}
        />
        <select
          value={filterOlt}
          onChange={e => { setFilterOlt(e.target.value); setPage(1); }}
          style={{ ...inputStyle, width: "140px" }}
          onFocus={focusBorder} onBlur={blurBorder}
        >
          <option value="">All OLT Types</option>
          {oltTypes.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
        <select
          value={`${sortKey}-${sortDir}`}
          onChange={e => {
            const [k, d] = e.target.value.split("-");
            setSortKey(k as SortKey);
            setSortDir(d as SortDir);
            setPage(1);
          }}
          style={{ ...inputStyle, width: "210px" }}
          onFocus={focusBorder} onBlur={blurBorder}
        >
          <option value="utilPercent-desc">Utilisation: High → Low</option>
          <option value="utilPercent-asc">Utilisation: Low → High</option>
          <option value="usedPorts-desc">Used Ports: Most first</option>
          <option value="usedPorts-asc">Used Ports: Least first</option>
          <option value="freePorts-asc">Free Ports: Least first</option>
          <option value="freePorts-desc">Free Ports: Most first</option>
          <option value="totalPorts-desc">Total Ports: Largest first</option>
        </select>
        {(search || filterOlt) && (
          <button onClick={() => { setSearch(""); setFilterOlt(""); setPage(1); }} style={cancelBtn}>Clear</button>
        )}
        <span style={{ fontSize: "13px", color: "#6b7280", marginLeft: "auto" }}>
          {filtered.length} OLT{filtered.length !== 1 ? "s" : ""}{filtered.length !== data.olts.length ? ` (of ${data.olts.length})` : ""}
        </span>
      </div>

      {/* Table */}
      <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden" }}>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr style={{ background: "#f1f5f9" }}>
              {["Pincode", "OLT Code", "OLT Type", "Splitters", "Total"].map(h => (
                <th key={h} style={thStyle}>{h}</th>
              ))}
              <th style={{ ...thStyle, cursor: "pointer", userSelect: "none" }} onClick={() => handleSort("usedPorts")}>Used{arrow("usedPorts")}</th>
              <th style={{ ...thStyle, cursor: "pointer", userSelect: "none" }} onClick={() => handleSort("freePorts")}>Free{arrow("freePorts")}</th>
              <th style={{ ...thStyle, cursor: "pointer", userSelect: "none" }} onClick={() => handleSort("utilPercent")}>Util %{arrow("utilPercent")}</th>
              <th style={thStyle}>Status</th>
            </tr>
          </thead>
          <tbody>
            {displayed.length === 0 ? (
              <tr><td colSpan={9} style={{ ...tdStyle, textAlign: "center", color: "#6b7280", padding: "24px" }}>No OLTs match your filters.</td></tr>
            ) : (
              displayed.map((o, i) => (
                <tr key={o.oltCode} style={{ background: i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb" }}>
                  <td style={tdStyle}>{o.pincode}</td>
                  <td style={{ ...tdStyle, fontWeight: 500 }}>{o.oltCode}</td>
                  <td style={tdStyle}>{o.oltType}</td>
                  <td style={tdStyle}>{o.splitterCount}</td>
                  <td style={tdStyle}>{o.totalPorts}</td>
                  <td style={tdStyle}>{o.usedPorts}</td>
                  <td style={tdStyle}>{o.freePorts}</td>
                  <td style={{ ...tdStyle, fontWeight: 500, color: o.utilPercent >= data.threshold ? "#f59e0b" : "#16a34a" }}>
                    {o.utilPercent}%
                  </td>
                  <td style={tdStyle}>{statusBadge(o)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {filtered.length > PAGE_SIZE && (
        <div style={{ marginTop: "12px", display: "flex", alignItems: "center", gap: "6px" }}>
          <button
            onClick={() => setPage(p => Math.max(1, p - 1))}
            disabled={safePage === 1}
            style={{ ...cancelBtn, width: "36px", padding: 0, opacity: safePage === 1 ? 0.4 : 1, cursor: safePage === 1 ? "not-allowed" : "pointer" }}
          >←</button>
          {Array.from({ length: totalPages }, (_, i) => i + 1).map(p => (
            <button key={p} onClick={() => setPage(p)} style={{
              width: "32px", height: "36px", padding: 0,
              background: p === safePage ? "#256D85" : "#ffffff",
              color: p === safePage ? "#ffffff" : "#374151",
              border: "1px solid " + (p === safePage ? "#256D85" : "#d1d5db"),
              borderRadius: "3px", fontSize: "13px",
              fontWeight: p === safePage ? 600 : 400, cursor: "pointer",
            }}>{p}</button>
          ))}
          <button
            onClick={() => setPage(p => Math.min(totalPages, p + 1))}
            disabled={safePage === totalPages}
            style={{ ...cancelBtn, width: "36px", padding: 0, opacity: safePage === totalPages ? 0.4 : 1, cursor: safePage === totalPages ? "not-allowed" : "pointer" }}
          >→</button>
          <span style={{ fontSize: "13px", color: "#6b7280", marginLeft: "6px" }}>
            Page {safePage} of {totalPages} · {filtered.length} OLTs
          </span>
        </div>
      )}
    </PageWrapper>
  );
}
