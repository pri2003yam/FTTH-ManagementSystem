import { useState } from "react";
import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";
import Modal from "../../components/ui/Modal";
import Input from "../../components/ui/Input";
import Select from "../../components/ui/Select";
import { errText } from "./UsersShared";

const ROLES = [
  { value: "CSR", label: "CSR" },
  { value: "MAINT", label: "MAINT" },
];

interface Props {
  onClose: () => void;
  onSuccess: () => void;
}

export default function CreateUserForm({ onClose, onSuccess }: Props) {
  const [form, setForm] = useState({ username: "", password: "", role: "CSR" });
  const [error, setError] = useState("");

  const handleCreate = async () => {
    setError("");
    if (!form.username.trim()) { setError("Username is required."); return; }
    if (!form.password.trim()) { setError("Password is required."); return; }
    try {
      await api.post(ENDPOINTS.USERS, form);
      onSuccess();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to create user.");
    }
  };

  return (
    <Modal open title="Create New User" onConfirm={handleCreate} onCancel={onClose} confirmLabel="Create User">
      <div className="space-y-3">
        <Input label="Username" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} placeholder="Enter username" />
        <Input label="Password" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="Enter password" />
        <Select label="Role" value={form.role} options={ROLES} onChange={(e) => setForm({ ...form, role: e.target.value })} />
        {error && <p style={errText}>{error}</p>}
      </div>
    </Modal>
  );
}