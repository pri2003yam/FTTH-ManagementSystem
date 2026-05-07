import { useEffect, useState } from "react";
import PageWrapper from "../../components/layout/PageWrapper";
import Card from "../../components/ui/Card";
import Button from "../../components/ui/Button";
import Table from "../../components/ui/Table";
import Badge from "../../components/ui/Badge";
import Loader from "../../components/ui/Loader";
import Modal from "../../components/ui/Modal";
import Input from "../../components/ui/Input";
import Select from "../../components/ui/Select";
import { api } from "../../services/apiClient";

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

export default function PlanAdmin() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);

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

  return (
    <PageWrapper title="Plan Admin">
      <Button onClick={openAdd} style={{ padding: "6px 14px", fontSize: "13px", alignSelf: "flex-start" }}>+ Add New Plan</Button>

      <Card>
        <Table
          keyField="planId"
          data={plans}
          columns={[
            { key: "planId", header: "Key" },
            { key: "planName", header: "Name" },
            { key: "speedLabel", header: "Speed" },
            { key: "dataLimitLabel", header: "Data" },
            { key: "ottCount", header: "OTTs" },
            {
              key: "monthlyPrice",
              header: "Price",
              render: (r) => `₹${r.monthlyPrice}/mo`,
            },
            { key: "oltType", header: "OLT Type" },
            { key: "customerCount", header: "Active Customers" },
            {
              key: "active",
              header: "Status",
              render: (r) =>
                r.active ? (
                  <Badge label="Active" variant="success" />
                ) : (
                  <Badge label="Disabled" variant="error" />
                ),
            },
            {
              key: "actions",
              header: "Actions",
              render: (r) => (
                <div style={{ display: "flex", gap: "12px", alignItems: "center" }}>
                  <button style={{ fontSize: "13px", color: "#2563eb", background: "none", border: "none", cursor: "pointer", padding: 0 }} onClick={() => openEdit(r)}>Edit</button>
                  <button style={{ fontSize: "13px", color: "#f59e0b", background: "none", border: "none", cursor: "pointer", padding: 0 }} onClick={() => togglePlan(r.planId)}>{r.active ? "Disable" : "Enable"}</button>
                  <button style={{ fontSize: "13px", color: r.customerCount > 0 ? "#9ca3af" : "#dc2626", background: "none", border: "none", cursor: r.customerCount > 0 ? "not-allowed" : "pointer", padding: 0 }} onClick={() => deletePlan(r)}>Delete</button>
                </div>
              ),
            },
          ]}
        />
      </Card>

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