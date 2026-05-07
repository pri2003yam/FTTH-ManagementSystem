import { useEffect, useState } from "react";
import PageWrapper from "../../components/layout/PageWrapper";
import Button from "../../components/ui/Button";
import Badge from "../../components/ui/Badge";
import Loader from "../../components/ui/Loader";
import Modal from "../../components/ui/Modal";
import Input from "../../components/ui/Input";
import Select from "../../components/ui/Select";
import { api } from "../../services/apiClient";
import {
  inputStyle, primaryBtn, cancelBtn, thStyle, tdStyle,
  focusBorder, blurBorder, errText,
} from "../Users/UsersShared";

interface Plan {
  planId: number;
  planName: string;
  speedLabel: string;
  dataLimitLabel: string;
  ottCount: number;
  monthlyPrice: number;
  oltType: string;
  active: boolean;
  customerCount: number;
}

const OLT_OPTIONS = [
  { value: "OLT300", label: "OLT300" },
  { value: "OLT500", label: "OLT500" },
];

type SortKey = "monthlyPrice" | "customerCount" | "speedLabel" | "ottCount";
type SortDir = "asc" | "desc";

export default function PlanAdmin() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);

  // ── Filters / sort ──
  const [search, setSearch] = useState("");
  const [filterOlt, setFilterOlt] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("monthlyPrice");
  const [sortDir, setSortDir] = useState<SortDir>("asc");
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 10;

  const [showForm, setShowForm] = useState(false);
  const [editPlan, setEditPlan] = useState<Plan | null>(null);

  const [form, setForm] = useState<any>({
    planName: "",
    speedLabel: "",
    dataLimitLabel: "",
    ottCount: 0,
    monthlyPrice: "",
    oltType: "",
  });

  const loadPlans = async () => {
    setLoading(true);
    const data = await api.get<Plan[]>("/api/admin/plans");
    setPlans(data);
    setLoading(false);
  };

  useEffect(() => {
    loadPlans();
  }, []);

  const openAdd = () => {
    setEditPlan(null);
    setFormError("");
    setForm({
      planName: "",
      speedLabel: "",
      dataLimitLabel: "",
      ottCount: 0,
      monthlyPrice: "",
      oltType: "",
    });
    setShowForm(true);
  };

  const openEdit = (p: Plan) => {
    setEditPlan(p);
    setFormError("");
    setForm({
      ...p,
      speedLabel: p.speedLabel.replace(/MBPS$/i, "").trim(),
      dataLimitLabel: p.dataLimitLabel.toLowerCase() === "unlimited internet" ? "unlimited" : p.dataLimitLabel.replace(/GB\/Month$/i, "").replace(/GB$/i, "").trim(),
    });
    setShowForm(true);
  };

  const [formError, setFormError] = useState("");

  const savePlan = async () => {
    setFormError("");

    const rawSpeed = form.speedLabel.trim();
    const rawData = form.dataLimitLabel.trim();

    // Validate speed — must be a number
    if (!rawSpeed || isNaN(Number(rawSpeed))) {
      setFormError("Speed must be a number (e.g. 300, 500, 1000).");
      return;
    }

    // Validate data — must be a number or 'unlimited'
    const isUnlimited = rawData.toLowerCase() === "unlimited";
    if (!rawData || (!isUnlimited && isNaN(Number(rawData)))) {
      setFormError("Data must be a number (e.g. 60) or 'Unlimited'.");
      return;
    }

    const payload = {
      ...form,
      speedLabel: rawSpeed + "MBPS",
      dataLimitLabel: isUnlimited ? "Unlimited Internet" : rawData + "GB/Month",
    };

    if (editPlan) {
      await api.put(`/api/admin/plans/${editPlan.planId}`, payload);
    } else {
      await api.post("/api/admin/plans", payload);
    }
    setShowForm(false);
    loadPlans();
  };

  const togglePlan = async (id: number) => {
    await api.patch(`/api/admin/plans/${id}/toggle`, {});
    loadPlans();
  };

  const deletePlan = async (p: Plan) => {
    if (p.customerCount > 0) return;
    await api.del(`/api/admin/plans/${p.planId}`);
    loadPlans();
  };

  if (loading) return <Loader />;

  // ── derive filtered + sorted list ──
  const oltTypes = [...new Set(plans.map(p => p.oltType))];

  const filtered = plans
    .filter(p => {
      const q = search.toLowerCase();
      const matchSearch = !q ||
        p.planName.toLowerCase().includes(q) ||
        p.speedLabel.toLowerCase().includes(q) ||
        p.dataLimitLabel.toLowerCase().includes(q) ||
        p.ottCount.toString().includes(q);
      const matchOlt = !filterOlt || p.oltType === filterOlt;
      return matchSearch && matchOlt;
    })
    .sort((a, b) => {
      let av: number, bv: number;
      if (sortKey === "speedLabel") {
        av = parseFloat(a.speedLabel) || 0;
        bv = parseFloat(b.speedLabel) || 0;
      } else {
        av = a[sortKey] as number;
        bv = b[sortKey] as number;
      }
      return sortDir === "asc" ? av - bv : bv - av;
    });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const displayed = filtered.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  const handleSort = (key: SortKey) => {
    if (sortKey === key) setSortDir(d => d === "asc" ? "desc" : "asc");
    else { setSortKey(key); setSortDir("asc"); }
    setPage(1);
  };

  const sortArrow = (key: SortKey) => sortKey === key ? (sortDir === "asc" ? " ↑" : " ↓") : "";

  return (
    <PageWrapper title="Plan Admin">
      <Button onClick={openAdd} style={{ padding: "6px 14px", fontSize: "13px", alignSelf: "flex-start" }}>+ Add New Plan</Button>

      {/* ── Filters row ── */}
      <div style={{ display: "flex", gap: "10px", alignItems: "center", flexWrap: "wrap", margin: "16px 0 12px" }}>
        <input
          value={search}
          onChange={e => { setSearch(e.target.value); setPage(1); }}
          placeholder="Search by name, speed, data, OTTs..."
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
          style={{ ...inputStyle, width: "200px" }}
          onChange={e => {
            const [k, d] = e.target.value.split("-");
            setSortKey(k as SortKey);
            setSortDir(d as SortDir);
            setPage(1);
          }}
          onFocus={focusBorder} onBlur={blurBorder}
        >
          <option value="monthlyPrice-asc">Price: Low → High</option>
          <option value="monthlyPrice-desc">Price: High → Low</option>
          <option value="customerCount-desc">Customers: Most first</option>
          <option value="customerCount-asc">Customers: Least first</option>
          <option value="speedLabel-desc">Speed: Fastest first</option>
          <option value="speedLabel-asc">Speed: Slowest first</option>
          <option value="ottCount-desc">OTTs: Most first</option>
        </select>
        {(search || filterOlt) && (
          <button onClick={() => { setSearch(""); setFilterOlt(""); setPage(1); }} style={cancelBtn}>Clear</button>
        )}
        <span style={{ fontSize: "13px", color: "#6b7280", marginLeft: "auto" }}>
          {filtered.length} plan{filtered.length !== 1 ? "s" : ""} {filtered.length !== plans.length ? `(of ${plans.length})` : ""}
        </span>
      </div>

      {/* ── Table ── */}
      <div style={{ background: "#ffffff", border: "1px solid #d1d5db", borderRadius: "4px", overflow: "hidden" }}>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr style={{ background: "#f1f5f9" }}>
              {["Name", "Speed", "Data", "OTTs", "OLT Type"].map(h => (
                <th key={h} style={thStyle}>{h}</th>
              ))}
              <th style={{ ...thStyle, cursor: "pointer", userSelect: "none" }} onClick={() => handleSort("monthlyPrice")}>
                Price{sortArrow("monthlyPrice")}
              </th>
              <th style={{ ...thStyle, cursor: "pointer", userSelect: "none" }} onClick={() => handleSort("customerCount")}>
                Customers{sortArrow("customerCount")}
              </th>
              <th style={thStyle}>Status</th>
              <th style={thStyle}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {displayed.length === 0 ? (
              <tr><td colSpan={9} style={{ ...tdStyle, textAlign: "center", color: "#6b7280", padding: "24px" }}>No plans match your filters.</td></tr>
            ) : (
              displayed.map((r, i) => (
                <tr key={r.planId} style={{ background: i % 2 === 1 ? "#fafafa" : "#ffffff", borderTop: "1px solid #e5e7eb" }}>
                  <td style={{ ...tdStyle, fontWeight: 500 }}>{r.planName}</td>
                  <td style={tdStyle}>{r.speedLabel}</td>
                  <td style={tdStyle}>{r.dataLimitLabel}</td>
                  <td style={tdStyle}>{r.ottCount}</td>
                  <td style={tdStyle}>{r.oltType}</td>
                  <td style={{ ...tdStyle, color: "#256D85", fontWeight: 500 }}>Rs. {r.monthlyPrice}/mo</td>
                  <td style={tdStyle}>
                    <span style={{ display: "inline-flex", alignItems: "center", gap: "4px", fontSize: "13px", fontWeight: 500, color: r.customerCount > 0 ? "#0369a1" : "#9ca3af" }}>
                      {r.customerCount > 0 ? `👥 ${r.customerCount}` : "—"}
                    </span>
                  </td>
                  <td style={tdStyle}>
                    {r.active ? <Badge label="Active" variant="success" /> : <Badge label="Disabled" variant="error" />}
                  </td>
                  <td style={tdStyle}>
                    <div style={{ display: "flex", gap: "12px" }}>
                      <button style={{ fontSize: "13px", color: "#2563eb", background: "none", border: "none", cursor: "pointer", padding: 0 }} onClick={() => openEdit(r)}>Edit</button>
                      <button style={{ fontSize: "13px", color: "#f59e0b", background: "none", border: "none", cursor: "pointer", padding: 0 }} onClick={() => togglePlan(r.planId)}>{r.active ? "Disable" : "Enable"}</button>
                      <button style={{ fontSize: "13px", color: r.customerCount > 0 ? "#9ca3af" : "#dc2626", background: "none", border: "none", cursor: r.customerCount > 0 ? "not-allowed" : "pointer", padding: 0 }} onClick={() => deletePlan(r)}>Delete</button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* ── Pagination ── */}
      {filtered.length > PAGE_SIZE && (
        <div style={{ marginTop: "12px", display: "flex", alignItems: "center", gap: "6px" }}>
          <button
            onClick={() => setPage(p => Math.max(1, p - 1))}
            disabled={safePage === 1}
            style={{ ...cancelBtn, width: "36px", padding: 0, opacity: safePage === 1 ? 0.4 : 1, cursor: safePage === 1 ? "not-allowed" : "pointer" }}
          >←</button>

          {Array.from({ length: totalPages }, (_, i) => i + 1).map(p => (
            <button
              key={p}
              onClick={() => setPage(p)}
              style={{
                width: "32px", height: "36px", padding: 0,
                background: p === safePage ? "#256D85" : "#ffffff",
                color: p === safePage ? "#ffffff" : "#374151",
                border: "1px solid " + (p === safePage ? "#256D85" : "#d1d5db"),
                borderRadius: "3px", fontSize: "13px",
                fontWeight: p === safePage ? 600 : 400,
                cursor: "pointer",
              }}
            >{p}</button>
          ))}

          <button
            onClick={() => setPage(p => Math.min(totalPages, p + 1))}
            disabled={safePage === totalPages}
            style={{ ...cancelBtn, width: "36px", padding: 0, opacity: safePage === totalPages ? 0.4 : 1, cursor: safePage === totalPages ? "not-allowed" : "pointer" }}
          >→</button>

          <span style={{ fontSize: "13px", color: "#6b7280", marginLeft: "6px" }}>
            Page {safePage} of {totalPages} · {filtered.length} plans
          </span>
        </div>
      )}

      <Modal
        open={showForm}
        title={editPlan ? "Edit Plan" : "Add New Plan"}
        onConfirm={savePlan}
        onCancel={() => setShowForm(false)}
      >
        <div className="space-y-3">
          <Input
            label="Plan Name"
            value={form.planName}
            onChange={(e) =>
              setForm({ ...form, planName: e.target.value })
            }
          />
          <Input
            label="Speed (MBPS)"
            value={form.speedLabel}
            placeholder="e.g. 300"
            onChange={(e) =>
              setForm({ ...form, speedLabel: e.target.value })
            }
          />
          <Input
            label="Data Limit (GB or Unlimited)"
            value={form.dataLimitLabel}
            placeholder="e.g. 60 or Unlimited"
            onChange={(e) =>
              setForm({ ...form, dataLimitLabel: e.target.value })
            }
          />
          <Input
            label="OTT Count"
            type="number"
            value={form.ottCount}
            onChange={(e) =>
              setForm({ ...form, ottCount: Number(e.target.value) })
            }
          />
          <Input
            label="Monthly Price"
            type="number"
            value={form.monthlyPrice}
            onChange={(e) =>
              setForm({ ...form, monthlyPrice: Number(e.target.value) })
            }
          />

          {editPlan ? (
            <Input label="OLT Type" value={form.oltType} disabled />
          ) : (
            <Select
              label="OLT Type"
              value={form.oltType}
              options={OLT_OPTIONS}
              onChange={(e) =>
                setForm({ ...form, oltType: e.target.value })
              }
            />
          )}
          {formError && <p style={{ fontSize: "13px", color: "#dc2626", margin: "4px 0 0 0" }}>{formError}</p>}
        </div>
      </Modal>
    </PageWrapper>
  );
}