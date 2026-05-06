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
  username: string;
  currentRole: string;
  onClose: () => void;
  onSuccess: () => void;
}

export default function EditUserModal({ username, currentRole, onClose, onSuccess }: Props) {
  const [form, setForm] = useState({ role: currentRole, password: "" });
  const [error, setError] = useState("");

  const handleEdit = async () => {
    setError("");
    try {
      await api.put(ENDPOINTS.USER_BY_USERNAME(username), { role: form.role, password: form.password });
      onSuccess();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to update user.");
    }
  };

  return (
    <Modal open title={`Edit User — ${username}`} onConfirm={handleEdit} onCancel={onClose} confirmLabel="Save Changes">
      <div className="space-y-3">
        <Input label="New Password" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="Leave blank to keep current password" />
        <Select label="Role" value={form.role} options={ROLES} onChange={(e) => setForm({ ...form, role: e.target.value })} />
        {error && <p style={errText}>{error}</p>}
      </div>
    </Modal>
  );
}