import { useState } from "react";
import PageWrapper from "../../components/layout/PageWrapper";
import InventorySummary from "./InventorySummary";
import AddOltForm from "./AddOltForm";
import RemoveOltForm from "./RemoveOltForm";
import AddSplitterForm from "./AddSplitterForm";
import RemoveSplitterForm from "./RemoveSplitterForm";
import OltDetails from "./OltDetails";

const tabs = [
  { key: "summary", label: "Summary" },
  { key: "addOlt", label: "Add OLT" },
  { key: "removeOlt", label: "Remove OLT" },
  { key: "addSplitter", label: "Add Splitter" },
  { key: "removeSplitter", label: "Remove Splitter" },
  { key: "details", label: "OLT Details" },
] as const;

type Tab = (typeof tabs)[number]["key"];

export default function Inventory() {
  const [activeTab, setActiveTab] = useState<Tab>("summary");

  return (
    <PageWrapper title="Inventory">
      <div style={{ display: "flex", gap: "8px" }}>
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setActiveTab(t.key)}
            style={{
              flex: 1,
              padding: "8px 0",
              fontSize: "13px",
              fontWeight: 500,
              borderRadius: "6px",
              cursor: "pointer",
              border: activeTab === t.key ? "none" : "1px solid #d1d5db",
              background: activeTab === t.key ? "#2563eb" : "transparent",
              color: activeTab === t.key ? "#ffffff" : "#1e293b",
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div style={{ marginTop: "8px" }}>
        {activeTab === "summary" && <InventorySummary />}
        {activeTab === "addOlt" && <AddOltForm onSuccess={() => setActiveTab("summary")} />}
        {activeTab === "removeOlt" && <RemoveOltForm onSuccess={() => setActiveTab("summary")} />}
        {activeTab === "addSplitter" && <AddSplitterForm onSuccess={() => setActiveTab("summary")} />}
        {activeTab === "removeSplitter" && <RemoveSplitterForm onSuccess={() => setActiveTab("summary")} />}
        {activeTab === "details" && <OltDetails />}
      </div>
    </PageWrapper>
  );
}
