import { useState } from "react";
import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";
import { Overlay, Field, inputStyle, primaryBtn, cancelBtn, modalBox, modalTitle, errText, focusBorder, blurBorder } from "./UsersShared";

const ROLES = [
  { value: "CSR", label: "CSR" },
  { value: "MAINT", label: "MAINT" },
];

interface Props {
  onClose: () => void;
  onSuccess: () => void;
}

export default function CreateUserForm({ onClose, onSuccess }: Props) {
  const [newUser, setNewUser] = useState({ username: "", password: "", role: "CSR" });
  const [createError, setCreateError] = useState("");
  const [createLoading, setCreateLoading] = useState(false);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateError("");
    if (!newUser.username.trim()) {
      setCreateError("Username is required.");
      return;
    }
    if (!newUser.password.trim()) {
      setCreateError("Password is required.");
      return;
    }
    setCreateLoading(true);
    try {
      await api.post(ENDPOINTS.USERS, newUser);
      onSuccess();
    } catch (err: unknown) {
      setCreateError(err instanceof Error ? err.message : "Failed to create user.");
    } finally {
      setCreateLoading(false);
    }
  };

  return (
    <Overlay>
      <div style={modalBox}>
        <h2 style={modalTitle}>Create New User</h2>
        <form onSubmit={handleCreate} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          <Field label="Username">
            <input
              value={newUser.username}
              onChange={(e) => setNewUser({ ...newUser, username: e.target.value })}
              placeholder="Enter username"
              style={inputStyle}
              onFocus={focusBorder}
              onBlur={blurBorder}
            />
          </Field>
          <Field label="Password">
            <input
              type="password"
              value={newUser.password}
              onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
              placeholder="Enter password"
              style={inputStyle}
              onFocus={focusBorder}
              onBlur={blurBorder}
            />
          </Field>
          <Field label="Role">
            <select
              value={newUser.role}
              onChange={(e) => setNewUser({ ...newUser, role: e.target.value })}
              style={inputStyle}
            >
              {ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
            </select>
          </Field>
          {createError && <p style={errText}>{createError}</p>}
          <div style={{ display: "flex", gap: "8px", justifyContent: "flex-end", marginTop: "4px" }}>
            <button type="button" onClick={onClose} style={cancelBtn}>Cancel</button>
            <button type="submit" disabled={createLoading} style={primaryBtn}>
              {createLoading ? "Creating..." : "Create User"}
            </button>
          </div>
        </form>
      </div>
    </Overlay>
  );
}
