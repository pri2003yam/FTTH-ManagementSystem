import { useEffect, useState } from "react";
import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";
import {
  inputStyle, cancelBtn, primaryBtn, thStyle, tdStyle,
  focusBorder, blurBorder,
} from "../Users/UsersShared";
import CustomerDetail from "./CustomerDetail";

interface CustomerRow {
  customerId: number;
  customerCode: string;
  fullName: string;
  email: string;
  status: string;
  pincode: string | null;
  planName: string | null;
}

const PAGE_SIZE = 10;

export default function CustomerScreen() {
  const [customers, setCustomers] = useState<CustomerRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [filterPincode, setFilterPincode] = useState("");
  const [pincodeInput, setPincodeInput] = useState("");
  const [pincodeSuggestions, setPincodeSuggestions] = useState<string[]>([]);
  const [pincodeDropdownOpen, setPincodeDropdownOpen] = useState(false);
  const [filterPlan, setFilterPlan] = useState("");
  const [planInput, setPlanInput] = useState("");
  const [planSuggestions, setPlanSuggestions] = useState<string[]>([]);
  const [planDropdownOpen, setPlanDropdownOpen] = useState(false);
  const [filterStatus, setFilterStatus] = useState("");
  const [page, setPage] = useState(1);
  const [selected, setSelected] = useState<CustomerRow | null>(null);

  const loadCustomers = () => {
    setLoading(true);
    setError("");
    api.get<CustomerRow[]>(ENDPOINTS.CUSTOMERS)
      .then(data => { setCustomers(data); setLoaded(true); })
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load customers."))
      .finally(() => setLoading(false));
  };

  // Auto-load when user starts searching and data not yet loaded
  useEffect(() => {
    if (search.length >= 2 && !loaded && !loading) loadCustomers();
  }, [search]);

  if (selected) {
    return (
      <div style={{ fontFamily: "'Source Sans 3', 'Segoe UI', sans-serif", padding: "24px" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "20px" }}>
          <h1 style={{ fontSize: "22px", fontWeight: 600, color: "#111827", margin: 0 }}>Customer: {selected.customerCode}</h1>
          <button onClick={() => { setSelected(null); loadCustomers(); }} style={cancelBtn}>← Back to List</button>
        </div>
        <CustomerDetail customerCode={selected.customerCode} />
      </div>
    );
  }

  // derive filter options from loaded data
  const pincodes = [...new Set(customers.map(c => c.pincode).filter(Boolean) as string[])].sort();
  const plans = [...new Set(customers.map(c => c.planName).filter(Boolean) as string[])].sort();

  const q = search.toLowerCase();
  const filtered = customers.filter(c => {
    const matchSearch = !q ||
      c.customerCode.toLowerCase().includes(q) ||
      c.fullName.toLowerCase().includes(q) ||
      c.email.toLowerCase().includes(q);
    const matchPincode = !filterPincode || c.pincode === filterPincode;
    const matchPlan = !filterPlan || c.planName === filterPlan;
    const matchStatus = !filterStatus || c.status === filterStatus;
    return matchSearch && matchPincode && matchPlan && matchStatus;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const displayed = filtered.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  const hasFilters = search || filterPincode || filterPlan || filterStatus;

  const clearFilters = () => {
    setSearch(""); setFilterPincode(""); setPincodeInput(""); setFilterPlan(""); setPlanInput(""); setFilterStatus(""); setPage(1);
    setPincodeSuggestions([]); setPincodeDropdownOpen(false);
    setPlanSuggestions([]); setPlanDropdownOpen(false);
  };

  return (
    <div style={{ fontFamily: "'Source Sans 3', 'Segoe UI', sans-serif", padding: "24px" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "20px" }}>
        <h1 style={{ fontSize: "22px", fontWeight: 600, color: "#111827", margin: 0 }}>Customers</h1>
        {!loaded ? (
          <button onClick={loadCustomers} disabled={loading} style={primaryBtn}>
            {loading ? "Loading..." : "View All Customers"}
          </button>
        ) : (
          <button onClick={() => { setLoaded(false); setCustomers([]); clearFilters(); }} style={cancelBtn}>
            ← Back to Search
          </button>
        )}
      </div>

      {/* Quick search — only shown before View All is clicked */}
      {!loaded && (
        <div style={{ display: "flex", gap: "10px", marginBottom: "16px", alignItems: "center" }}>
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(1); }}
            placeholder="Search by customer code or name..."
            style={{ ...inputStyle, width: "400px" }}
            onFocus={focusBorder} onBlur={blurBorder}
          />
          {search && <button onClick={() => { setSearch(""); setPage(1); }} style={cancelBtn}>Clear</button>}
        </div>
      )}

      {/* Quick search results before View All is clicked */}
      {!loaded && loading && (
        <p style={{ fontSize: "13px", color: "#6b7280" }}>Loading...</p>
      )}
      {!loaded && !loading && search.length >= 2 && customers.length > 0 && (() => {
        const q2 = search.toLowerCase();
        const qs = customers.filter(c =>
          c.customerCode.toLowerCase().includes(q2) ||
          c.fullName.toLowerCase().includes(q2) ||
          c.email.toLowerCase().includes(q2) ||
          (c.pincode && c.pincode.includes(q2))
        );
        return qs.length === 0 ? (
          <p style={{ fontSize: "13px", color: "#6b7280" }}>No customers match "{search}".</p>
        ) : (
          <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden", maxHeight: "400px", overflowY: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead style={{ position: "sticky", top: 0, zIndex: 1 }}>
                <tr style={{ background: "#f1f5f9" }}>
                  {["Code", "Name", "Email", "Pincode", "Plan", "Status"].map(h => <th key={h} style={thStyle}>{h}</th>)}
                </tr>
              </thead>
              <tbody>
                {qs.map((c, i) => (
                  <tr key={c.customerId} onClick={() => setSelected(c)}
                    style={{ background: i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb", cursor: "pointer" }}>
                    <td style={{ ...tdStyle, fontWeight: 600 }}>{c.customerCode}</td>
                    <td style={tdStyle}>{c.fullName}</td>
                    <td style={{ ...tdStyle, color: "#6b7280" }}>{c.email}</td>
                    <td style={tdStyle}>{c.pincode || "-"}</td>
                    <td style={tdStyle}>{c.planName || "-"}</td>
                    <td style={tdStyle}>
                      <span style={{ fontSize: "12px", fontWeight: 500, padding: "2px 8px", borderRadius: "3px", background: c.status === "ACTIVE" ? "#dcfce7" : "#fee2e2", color: c.status === "ACTIVE" ? "#166534" : "#991b1b" }}>{c.status}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );
      })()}

      {error && <p style={{ fontSize: "13px", color: "#dc2626" }}>{error}</p>}

      {!loaded && !loading && search.length < 2 && (
        <p style={{ fontSize: "13px", color: "#9ca3af" }}>Type at least 2 characters to search, or click "View All Customers".</p>
      )}

      {loaded && (
        <>
          {/* Filters row */}
          <div style={{ display: "flex", gap: "10px", marginBottom: "12px", alignItems: "center", flexWrap: "wrap" }}>
            <input
              value={search}
              onChange={e => { setSearch(e.target.value); setPage(1); }}
              placeholder="Search by code or name..."
              style={{ ...inputStyle, width: "260px" }}
              onFocus={focusBorder} onBlur={blurBorder}
            />
            <div style={{ position: "relative" }}>
              <input
                value={pincodeInput}
                onChange={e => {
                  const val = e.target.value.replace(/\D/g, "").slice(0, 6);
                  setPincodeInput(val);
                  setFilterPincode("");  // clear filter while typing
                  setPage(1);
                  if (val.length >= 4) {
                    const matches = pincodes.filter(p => p.startsWith(val)).slice(0, 8);
                    setPincodeSuggestions(matches);
                    setPincodeDropdownOpen(matches.length > 0);
                  } else {
                    setPincodeSuggestions([]);
                    setPincodeDropdownOpen(false);
                  }
                }}
                onBlur={() => setTimeout(() => setPincodeDropdownOpen(false), 150)}
                placeholder="Pincode"
                maxLength={6}
                style={{ ...inputStyle, width: "120px", borderColor: filterPincode ? "#256D85" : undefined }}
                onFocus={focusBorder}
                autoComplete="off"
              />
              {pincodeDropdownOpen && (
                <div style={{
                  position: "absolute", top: "100%", left: 0, right: 0,
                  background: "#fff", border: "1px solid #d1d5db", borderRadius: "4px",
                  boxShadow: "0 4px 12px rgba(0,0,0,0.1)", zIndex: 100,
                  maxHeight: "160px", overflowY: "auto", marginTop: "2px",
                }}>
                  {pincodeSuggestions.map(p => (
                    <div key={p}
                      onMouseDown={() => {
                        setPincodeInput(p);
                        setFilterPincode(p);  // apply filter only on selection
                        setPincodeDropdownOpen(false);
                        setPage(1);
                      }}
                      style={{ padding: "8px 12px", fontSize: "13px", cursor: "pointer", borderBottom: "1px solid #f3f4f6" }}
                      onMouseEnter={e => (e.currentTarget.style.background = "#f0f9ff")}
                      onMouseLeave={e => (e.currentTarget.style.background = "#fff")}
                    >{p}</div>
                  ))}
                </div>
              )}
            </div>
            <div style={{ position: "relative" }}>
              <input
                value={planInput}
                onChange={e => {
                  const val = e.target.value;
                  setPlanInput(val);
                  setFilterPlan("");
                  setPage(1);
                  if (val.length >= 2) {
                    const m = plans.filter(pl => pl.toLowerCase().includes(val.toLowerCase())).slice(0, 8);
                    setPlanSuggestions(m);
                    setPlanDropdownOpen(m.length > 0);
                  } else {
                    setPlanSuggestions([]);
                    setPlanDropdownOpen(false);
                  }
                }}
                onBlur={() => setTimeout(() => setPlanDropdownOpen(false), 150)}
                placeholder="Plan name..."
                style={{ ...inputStyle, width: "160px", borderColor: filterPlan ? "#256D85" : undefined }}
                onFocus={focusBorder}
                autoComplete="off"
              />
              {planDropdownOpen && (
                <div style={{ position: "absolute", top: "100%", left: 0, right: 0, background: "#fff", border: "1px solid #d1d5db", borderRadius: "4px", boxShadow: "0 4px 12px rgba(0,0,0,0.1)", zIndex: 100, maxHeight: "160px", overflowY: "auto", marginTop: "2px" }}>
                  {planSuggestions.map(pl => (
                    <div key={pl} onMouseDown={() => { setPlanInput(pl); setFilterPlan(pl); setPlanDropdownOpen(false); setPage(1); }}
                      style={{ padding: "8px 12px", fontSize: "13px", cursor: "pointer", borderBottom: "1px solid #f3f4f6" }}
                      onMouseEnter={e => (e.currentTarget.style.background = "#f0f9ff")}
                      onMouseLeave={e => (e.currentTarget.style.background = "#fff")}>{pl}</div>
                  ))}
                </div>
              )}
            </div>
            <select
              value={filterStatus}
              onChange={e => { setFilterStatus(e.target.value); setPage(1); }}
              style={{ ...inputStyle, width: "130px" }}
              onFocus={focusBorder} onBlur={blurBorder}
            >
              <option value="">All Status</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
            {hasFilters && <button onClick={clearFilters} style={cancelBtn}>Clear</button>}
            <span style={{ fontSize: "13px", color: "#6b7280", marginLeft: "auto" }}>
              {filtered.length} customer{filtered.length !== 1 ? "s" : ""}{filtered.length !== customers.length ? ` (of ${customers.length})` : ""}
            </span>
          </div>

          {/* Table */}
          {filtered.length === 0 ? (
            <p style={{ fontSize: "13px", color: "#6b7280" }}>No customers match your filters.</p>
          ) : (
            <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden" }}>
              <table style={{ width: "100%", borderCollapse: "collapse" }}>
                <thead style={{ position: "sticky", top: 0, zIndex: 1 }}>
                  <tr style={{ background: "#f1f5f9" }}>
                    {["Code", "Name", "Email", "Pincode", "Plan", "Status"].map(h => (
                      <th key={h} style={thStyle}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {displayed.map((c, i) => (
                    <tr
                      key={c.customerId}
                      onClick={() => setSelected(c)}
                      style={{ background: i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb", cursor: "pointer" }}
                    >
                      <td style={{ ...tdStyle, fontWeight: 600 }}>{c.customerCode}</td>
                      <td style={tdStyle}>{c.fullName}</td>
                      <td style={{ ...tdStyle, color: "#6b7280" }}>{c.email}</td>
                      <td style={tdStyle}>{c.pincode || "-"}</td>
        <td style={tdStyle}>{c.planName || "-"}</td>
                      <td style={tdStyle}>
                        <span style={{
                          fontSize: "12px", fontWeight: 500, padding: "2px 8px", borderRadius: "3px",
                          background: c.status === "ACTIVE" ? "#dcfce7" : "#fee2e2",
                          color: c.status === "ACTIVE" ? "#166534" : "#991b1b",
                        }}>{c.status}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

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
                Page {safePage} of {totalPages} · {filtered.length} customers
              </span>
            </div>
          )}
        </>
      )}
    </div>
  );
}
