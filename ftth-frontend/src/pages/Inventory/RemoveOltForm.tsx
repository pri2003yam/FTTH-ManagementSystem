import { useEffect, useState } from "react";
import { inventoryService } from "../../services/inventoryService";
import type { OltInventoryDTO } from "../../types/models";
import Card from "../../components/ui/Card";
import Input from "../../components/ui/Input";
import Table from "../../components/ui/Table";
import Button from "../../components/ui/Button";
import Modal from "../../components/ui/Modal";
import Loader from "../../components/ui/Loader";

interface Props {
  onSuccess: () => void;
}

export default function RemoveOltForm({ onSuccess }: Props) {
  const [pincodes, setPincodes] = useState<string[]>([]);
  const [pincode, setPincode] = useState("");
  const [olts, setOlts] = useState<OltInventoryDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<string | null>(null);
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    inventoryService.getPincodes().then(setPincodes).catch(() => {});
  }, []);

  useEffect(() => {
    if (!pincode || !pincodes.includes(pincode)) { setOlts([]); return; }
    setLoading(true);
    inventoryService.getOltsByPincode(pincode).then(setOlts).catch(() => {}).finally(() => setLoading(false));
  }, [pincode, pincodes]);

  const handleRemove = async () => {
    if (!selected) return;
    setMsg("");
    setError("");
    try {
      await inventoryService.removeOlt(selected);
      setMsg(`OLT ${selected} removed.`);
      setSelected(null);
      setOlts((prev) => prev.filter((o) => o.oltCode !== selected));
      setTimeout(onSuccess, 1500);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to remove OLT.");
    }
  };

  const columns = [
    { key: "oltCode", header: "OLT Code" },
    { key: "oltType", header: "Type" },
    { key: "splitterCount", header: "Splitters" },
    { key: "ports", header: "Ports", render: (r: OltInventoryDTO) => `${r.availablePorts}/${r.totalPorts}` },
    {
      key: "action",
      header: "",
      render: (r: OltInventoryDTO) => {
        const hasCustomers = r.availablePorts < r.totalPorts;
        return (
          <Button
            variant="danger"
            className="text-xs px-2 py-1"
            onClick={() => setSelected(r.oltCode)}
            disabled={hasCustomers}
            style={hasCustomers ? { opacity: 0.4, cursor: "not-allowed" } : {}}
            title={hasCustomers ? "Cannot remove — has assigned ports" : "Remove OLT"}
          >
            Remove
          </Button>
        );
      },
    },
  ];

  return (
    <Card>
      <h2 className="mb-4">Remove OLT</h2>
      <div style={{ width: "140px", marginBottom: "16px" }}>
        <Input
          label="Select Pincode"
          value={pincode}
          onChange={(e) => setPincode(e.target.value)}
          placeholder="Pincode"
          list="remove-olt-pincode-list"
        />
        <datalist id="remove-olt-pincode-list">
          {pincodes.map((p) => (
            <option key={p} value={p} />
          ))}
        </datalist>
      </div>

      {loading && <Loader />}
      {!loading && pincode && <Table columns={columns} data={olts} keyField="oltCode" />}

      {error && <p className="text-error mt-2">{error}</p>}
      {msg && <p className="text-success mt-2">{msg}</p>}

      <Modal
        open={!!selected}
        title="Confirm Removal"
        onConfirm={handleRemove}
        onCancel={() => setSelected(null)}
        confirmLabel="Remove"
        danger
      >
        Are you sure you want to remove <strong>{selected}</strong>? This cannot be undone if ports are unassigned.
      </Modal>
    </Card>
  );
}
