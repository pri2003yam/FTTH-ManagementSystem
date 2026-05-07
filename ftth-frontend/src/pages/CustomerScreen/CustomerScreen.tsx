import { useEffect, useState } from "react";
import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";
import {
  inputStyle, cancelBtn, thStyle, tdStyle,
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
}

export default function CustomerScreen() {
  const [customers, setCustomers] = useState<CustomerRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState<CustomerRow | null>(null);

  useEffect(() => {
    loadCustomers();
  }, []);

  const loadCustomers = () => {
    setLoading(true);
    setError("");
    api.get<CustomerRow[]>(ENDPOINTS.CUSTOMERS)
      .then(setCustomers)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load customers."))
      .finally(() => setLoading(false));
  };

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

  const q = search.toLowerCase();
  const filtered = search.length >= 2
    ? customers.filter((c) =>
        c.customerCode.toLowerCase().includes(q) ||
        c.fullName.toLowerCase().includes(q) ||
        c.email.toLowerCase().includes(q) ||
        (c.pincode && c.pincode.includes(q))
      )
    : [];

  return (
    <div style={{ fontFamily: "'Source Sans 3', 'Segoe UI', sans-serif", padding: "24px" }}>
      <h1 style={{ fontSize: "22px", fontWeight: 600, color: "#111827", margin: "0 0 20px 0" }}>Customers</h1>

      <div style={{ display: "flex", gap: "10px", marginBottom: "12px", alignItems: "center" }}>
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by customer code, name, email, or pincode..."
          style={{ ...inputStyle, width: "500px" }}
          onFocus={focusBorder}
          onBlur={blurBorder}
        />
        {search && (
          <button onClick={() => setSearch("")} style={cancelBtn}>Clear</button>
        )}
      </div>

      {loading && <p style={{ fontSize: "13px", color: "#6b7280" }}>Loading...</p>}
      {error && <p style={{ fontSize: "13px", color: "#dc2626" }}>{error}</p>}

      {!loading && search.length >= 2 && (
        filtered.length === 0 ? (
          <p style={{ fontSize: "13px", color: "#6b7280" }}>No customers match "{search}".</p>
        ) : (
          <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden", maxHeight: "400px", overflowY: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead style={{ position: "sticky", top: 0, zIndex: 1 }}>
                <tr style={{ background: "#f1f5f9" }}>
                  {["Code", "Name", "Email", "Pincode"].map((h) => (
                    <th key={h} style={thStyle}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filtered.map((c, i) => (
                  <tr
                    key={c.customerId}
                    onClick={() => setSelected(c)}
                    style={{ background: i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb", cursor: "pointer" }}
                  >
                    <td style={{ ...tdStyle, fontWeight: 600 }}>{c.customerCode}</td>
                    <td style={tdStyle}>{c.fullName}</td>
                    <td style={{ ...tdStyle, color: "#6b7280" }}>{c.email}</td>
                    <td style={tdStyle}>{c.pincode || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {!loading && search.length < 2 && (
        <p style={{ fontSize: "13px", color: "#9ca3af" }}>Type at least 2 characters to search.</p>
      )}
    </div>
  );
}
