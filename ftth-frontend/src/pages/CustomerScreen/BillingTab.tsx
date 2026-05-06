import { useEffect, useState } from "react";
import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";
import { primaryBtn, cancelBtn, thStyle, tdStyle, errText } from "../Users/UsersShared";

interface BillRow {
  billId: number;
  billNo: string;
  billDate: string;
  dueDate: string;
  planCharge: number;
  gstAmount: number;
  totalAmount: number;
  billStatus: string;
}

export default function BillingTab({ customerCode }: { customerCode: string }) {
  const [bills, setBills] = useState<BillRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [genLoading, setGenLoading] = useState(false);
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");

  const loadBills = () => {
    setLoading(true);
    api.get<BillRow[]>(ENDPOINTS.CUSTOMER_BILLS(customerCode))
      .then(setBills)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadBills(); }, [customerCode]);

  const handleGenerate = async () => {
    setGenLoading(true);
    setMsg("");
    setError("");
    try {
      await api.post(ENDPOINTS.CUSTOMER_GENERATE_BILL(customerCode), {});
      setMsg("Bill generated successfully.");
      loadBills();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to generate bill.");
    } finally {
      setGenLoading(false);
    }
  };

  const handlePay = async (billId: number) => {
    setMsg("");
    setError("");
    try {
      await api.post(ENDPOINTS.BILL_PAY(billId), {});
      setMsg(`Bill #${billId} marked as PAID.`);
      loadBills();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed.");
    }
  };


  const statusBadge = (status: string) => {
    const colors: Record<string, { bg: string; text: string }> = {
      GENERATED: { bg: "#dbeafe", text: "#1e40af" },
      PAID: { bg: "#dcfce7", text: "#166534" },
    };
    const s = colors[status] || colors.GENERATED;
    return (
      <span style={{ background: s.bg, color: s.text, padding: "2px 8px", borderRadius: "3px", fontSize: "12px", fontWeight: 500 }}>
        {status}
      </span>
    );
  };

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", gap: "12px", marginBottom: "16px" }}>
        <button onClick={handleGenerate} disabled={genLoading} style={primaryBtn}>
          {genLoading ? "Generating..." : "Generate Bill"}
        </button>
        {msg && <span style={{ fontSize: "13px", color: "#16a34a" }}>{msg}</span>}
        {error && <span style={errText}>{error}</span>}
      </div>

      <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden" }}>
        {loading ? (
          <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>Loading bills...</p>
        ) : bills.length === 0 ? (
          <p style={{ padding: "16px", fontSize: "13px", color: "#6b7280" }}>No bills found.</p>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ background: "#f1f5f9" }}>
                {["Bill No", "Bill Date", "Due Date", "Plan Charge", "GST", "Total", "Status", "Actions"].map((h) => (
                  <th key={h} style={thStyle}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {bills.map((b, i) => (
                <tr key={b.billId} style={{ background: i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb" }}>
                  <td style={{ ...tdStyle, fontWeight: 500 }}>{b.billNo}</td>
                  <td style={tdStyle}>{b.billDate}</td>
                  <td style={tdStyle}>{b.dueDate}</td>
                  <td style={tdStyle}>Rs. {Number(b.planCharge).toFixed(2)}</td>
                  <td style={tdStyle}>Rs. {Number(b.gstAmount).toFixed(2)}</td>
                  <td style={{ ...tdStyle, fontWeight: 600, color: "#256D85" }}>Rs. {Number(b.totalAmount).toFixed(2)}</td>
                  <td style={tdStyle}>{statusBadge(b.billStatus)}</td>
                  <td style={tdStyle}>
                    {b.billStatus === "GENERATED" && (
                      <button onClick={() => handlePay(b.billId)} style={{ ...cancelBtn, fontSize: "11px", padding: "3px 8px" }}>Pay</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
