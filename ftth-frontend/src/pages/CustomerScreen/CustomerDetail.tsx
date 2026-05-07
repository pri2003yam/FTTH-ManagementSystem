import { useEffect, useState } from "react";
import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";
import BillingTab from "./BillingTab";
import ChangePlanTab from "./ChangePlanTab";
import MoveTab from "./MoveTab";
import DisconnectTab from "./DisconnectTab";

interface CustomerInfo {
  customerId: number;
  customerCode: string;
  fullName: string;
  email: string;
  status: string;
}

interface ConnectionInfo {
  connectionId: number;
  planId: number;
  serviceAreaId: number;
  planName: string;
  monthlyPrice: number;
  speedLabel: string;
  oltType: string;
  pincode: string;
  oltCode: string;
  splitterNumber: number;
  portNumber: number;
}

type Tab = "billing" | "change" | "move" | "disconnect";

const tabBtn = (active: boolean): React.CSSProperties => ({
  padding: "8px 16px",
  fontSize: "13px",
  fontWeight: active ? 600 : 400,
  color: active ? "#256D85" : "#6b7280",
  background: active ? "#e0f2fe" : "transparent",
  border: "1px solid " + (active ? "#256D85" : "#d1d5db"),
  borderRadius: "4px",
  cursor: "pointer",
});

export default function CustomerDetail({ customerCode }: { customerCode: string }) {
  const [customer, setCustomer] = useState<CustomerInfo | null>(null);
  const [connection, setConnection] = useState<ConnectionInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<Tab>("billing");

  const loadData = () => {
    setLoading(true);
    Promise.all([
      api.get<CustomerInfo>(ENDPOINTS.CUSTOMER_BY_CODE(customerCode)),
      api.get<ConnectionInfo | Record<string, never>>(ENDPOINTS.CUSTOMER_CONNECTION(customerCode)),
    ])
      .then(([c, conn]) => {
        setCustomer(c);
        setConnection(conn && "connectionId" in conn ? conn as ConnectionInfo : null);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadData(); }, [customerCode]);

  if (loading) return <p style={{ fontSize: "13px", color: "#6b7280" }}>Loading...</p>;
  if (!customer) return <p style={{ fontSize: "13px", color: "#dc2626" }}>Customer not found.</p>;

  return (
    <div>
      {/* Customer Card */}
      <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", padding: "20px", marginBottom: "20px", maxWidth: "600px" }}>
        <div style={{ display: "grid", gridTemplateColumns: "140px 1fr", gap: "6px 16px", fontSize: "13px" }}>
          <span style={{ color: "#6b7280" }}>Customer Code</span><span style={{ fontWeight: 600 }}>{customer.customerCode}</span>
          <span style={{ color: "#6b7280" }}>Name</span><span>{customer.fullName}</span>
          <span style={{ color: "#6b7280" }}>Email</span><span>{customer.email}</span>
        </div>

        {connection && (
          <>
            <hr style={{ border: "none", borderTop: "1px solid #e5e7eb", margin: "14px 0" }} />
            <div style={{ display: "grid", gridTemplateColumns: "140px 1fr", gap: "6px 16px", fontSize: "13px" }}>
              <span style={{ color: "#6b7280" }}>Plan</span><span>{connection.planName} @ Rs.{connection.monthlyPrice}</span>
              <span style={{ color: "#6b7280" }}>Speed</span><span>{connection.speedLabel}</span>
              <span style={{ color: "#6b7280" }}>OLT Type</span><span>{connection.oltType}</span>
              <span style={{ color: "#6b7280" }}>Pincode</span><span>{connection.pincode}</span>
              <span style={{ color: "#6b7280" }}>Port</span><span>{connection.oltCode}/Spl{connection.splitterNumber}/Port{connection.portNumber}</span>
            </div>
          </>
        )}
      </div>

      {!connection && (
        <div style={{ background: "#fff7ed", border: "1px solid #fed7aa", borderRadius: "4px", padding: "12px 16px", marginBottom: "20px" }}>
          <p style={{ fontSize: "13px", color: "#9a3412", margin: 0 }}>No active connection found for this customer.</p>
        </div>
      )}

      {/* Tabs */}
      {connection && (
        <>
          <div style={{ display: "flex", gap: "8px", marginBottom: "20px", flexWrap: "wrap" }}>
            <button onClick={() => setTab("billing")} style={tabBtn(tab === "billing")}>Billing</button>
            <button onClick={() => setTab("change")} style={tabBtn(tab === "change")}>Change Plan</button>
            <button onClick={() => setTab("move")} style={tabBtn(tab === "move")}>Move</button>
            <button onClick={() => setTab("disconnect")} style={tabBtn(tab === "disconnect")}>Disconnect</button>
          </div>

          {tab === "billing" && <BillingTab customerCode={customerCode} />}
          {tab === "change" && <ChangePlanTab connectionId={connection.connectionId} onDone={loadData} />}
          {tab === "move" && <MoveTab connectionId={connection.connectionId} currentPincode={connection.pincode} onDone={loadData} />}
          {tab === "disconnect" && <DisconnectTab connectionId={connection.connectionId} customerName={customer.fullName} onDone={loadData} />}
        </>
      )}
    </div>
  );
}
