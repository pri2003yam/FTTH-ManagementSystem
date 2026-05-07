import { useEffect, useState } from "react";
import { inventoryService } from "../../services/inventoryService";
import type { OltInventoryDTO, OltDetail, SplitterDetail } from "../../types/models";
import Card from "../../components/ui/Card";
import Select from "../../components/ui/Select";
import Input from "../../components/ui/Input";
import Table from "../../components/ui/Table";
import Button from "../../components/ui/Button";
import Modal from "../../components/ui/Modal";
import Loader from "../../components/ui/Loader";

interface Props {
  onSuccess: () => void;
}

export default function RemoveSplitterForm({ onSuccess }: Props) {
  const [pincodes, setPincodes] = useState<string[]>([]);
  const [pincode, setPincode] = useState("");
  const [olts, setOlts] = useState<OltInventoryDTO[]>([]);
  const [selectedOlt, setSelectedOlt] = useState("");
  const [details, setDetails] = useState<OltDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [toRemove, setToRemove] = useState<number | null>(null);
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    inventoryService.getPincodes().then(setPincodes).catch(() => {});
  }, []);

  useEffect(() => {
    if (!pincode || !pincodes.includes(pincode)) { setOlts([]); setSelectedOlt(""); setDetails(null); return; }
    setLoading(true);
    inventoryService.getOltsByPincode(pincode).then(setOlts).catch(() => {}).finally(() => setLoading(false));
  }, [pincode, pincodes]);

  useEffect(() => {
    if (!selectedOlt) { setDetails(null); return; }
    setLoading(true);
    inventoryService.getOltDetails(selectedOlt).then(setDetails).catch(() => {}).finally(() => setLoading(false));
  }, [selectedOlt]);

  const handleRemove = async () => {
    if (toRemove === null || !selectedOlt) return;
    setMsg("");
    setError("");
    try {
      await inventoryService.removeSplitter(selectedOlt, toRemove);
      setMsg(`Splitter ${toRemove} removed from ${selectedOlt}.`);
      setToRemove(null);
      const updated = await inventoryService.getOltDetails(selectedOlt);
      setDetails(updated);
      setTimeout(onSuccess, 1500);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to remove splitter.");
      setToRemove(null);
    }
  };

  const splitterColumns = [
    { key: "splitterNumber", header: "Splitter #" },
    { key: "splitterCode", header: "Code", render: (r: SplitterDetail) => r.splitterCode || "-" },
    { key: "ports", header: "Ports", render: (r: SplitterDetail) => `${r.availablePorts}/${r.totalPorts} available` },
    {
      key: "action",
      header: "",
      render: (r: SplitterDetail) => {
        const hasCustomers = r.availablePorts < r.totalPorts;
        return (
          <Button
            variant="danger"
            className="text-xs px-2 py-1"
            onClick={() => setToRemove(r.splitterNumber)}
            disabled={hasCustomers}
            style={hasCustomers ? { opacity: 0.4, cursor: "not-allowed" } : {}}
            title={hasCustomers ? "Cannot remove — has assigned ports" : "Remove splitter"}
          >
            Remove
          </Button>
        );
      },
    },
  ];

  return (
    <Card>
      <h2 className="mb-4">Remove Splitter from OLT</h2>
      <div style={{ display: "flex", gap: "12px", alignItems: "flex-end", marginBottom: "16px" }}>
        <div style={{ width: "140px" }}>
          <Input
            label="Pincode"
            value={pincode}
            onChange={(e) => { setPincode(e.target.value); setSelectedOlt(""); }}
            placeholder="Pincode"
            list="remove-splitter-pincode-list"
          />
          <datalist id="remove-splitter-pincode-list">
            {pincodes.map((p) => <option key={p} value={p} />)}
          </datalist>
        </div>
        {olts.length > 0 && (
          <div style={{ width: "160px" }}>
            <Select
              label="OLT"
              value={selectedOlt}
              onChange={(e) => setSelectedOlt(e.target.value)}
              options={olts.map((o) => ({ value: o.oltCode, label: `${o.oltCode} (${o.oltType})` }))}
            />
          </div>
        )}
      </div>

      {loading && <Loader />}
      {!loading && details && (
        <Table columns={splitterColumns} data={details.splitters} keyField="splitterNumber" />
      )}

      {error && <p className="text-error mt-2">{error}</p>}
      {msg && <p className="text-success mt-2">{msg}</p>}

      <Modal
        open={toRemove !== null}
        title="Confirm Removal"
        onConfirm={handleRemove}
        onCancel={() => setToRemove(null)}
        confirmLabel="Remove"
        danger
      >
        Remove Splitter #{toRemove} from <strong>{selectedOlt}</strong>? Ports must be unassigned.
      </Modal>
    </Card>
  );
}
