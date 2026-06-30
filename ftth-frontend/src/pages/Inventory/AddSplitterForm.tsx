import { useEffect, useState } from "react";
import { inventoryService } from "../../services/inventoryService";
import type { OltInventoryDTO } from "../../types/models";
import Card from "../../components/ui/Card";
import Input from "../../components/ui/Input";
import Table from "../../components/ui/Table";
import Button from "../../components/ui/Button";
import Loader from "../../components/ui/Loader";

interface Props {
  onSuccess: () => void;
}

export default function AddSplitterForm({ onSuccess }: Props) {
  const [pincodes, setPincodes] = useState<string[]>([]);
  const [pincode, setPincode] = useState("");
  const [olts, setOlts] = useState<OltInventoryDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");
  const [maxSplitters, setMaxSplitters] = useState(3);

  useEffect(() => {
    inventoryService.getPincodes().then(setPincodes).catch(() => {});
    inventoryService.getConfig().then((c) => setMaxSplitters(c.maxSplitters)).catch(() => {});
  }, []);

  useEffect(() => {
    if (!pincode || !pincodes.includes(pincode)) { setOlts([]); return; }
    setLoading(true);
    inventoryService.getOltsByPincode(pincode).then(setOlts).catch(() => {}).finally(() => setLoading(false));
  }, [pincode, pincodes]);

  const handleAdd = async (oltCode: string) => {
    setMsg("");
    setError("");
    try {
      await inventoryService.addSplitter(oltCode);
      setMsg(`Splitter added to ${oltCode}.`);
      const updated = await inventoryService.getOltsByPincode(pincode);
      setOlts(updated);
      setTimeout(onSuccess, 1500);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to add splitter.");
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
        const atMax = r.splitterCount >= maxSplitters;
        return (
          <Button
            className="text-xs px-2 py-1"
            onClick={() => handleAdd(r.oltCode)}
            disabled={atMax}
            style={atMax ? { opacity: 0.4, cursor: "not-allowed" } : {}}
            title={atMax ? `Max splitters (${maxSplitters}) reached` : "Add splitter"}
          >
            + Splitter
          </Button>
        );
      },
    },
  ];

  return (
    <Card>
      <h2 className="mb-4">Add Splitter to OLT</h2>
      <div style={{ width: "140px", marginBottom: "16px" }}>
        <Input
          label="Select Pincode"
          value={pincode}
          onChange={(e) => setPincode(e.target.value)}
          placeholder="Pincode"
          list="add-splitter-pincode-list"
        />
        <datalist id="add-splitter-pincode-list">
          {pincodes.map((p) => (
            <option key={p} value={p} />
          ))}
        </datalist>
      </div>

      {loading && <Loader />}
      {!loading && pincode && <Table columns={columns} data={olts} keyField="oltCode" />}

      {error && <p className="text-error mt-2">{error}</p>}
      {msg && <p className="text-success mt-2">{msg}</p>}
    </Card>
  );
}
