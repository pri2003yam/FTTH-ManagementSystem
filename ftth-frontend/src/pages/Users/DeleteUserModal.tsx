import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";
import Modal from "../../components/ui/Modal";

interface Props {
  username: string;
  onClose: () => void;
  onSuccess: () => void;
}

export default function DeleteUserModal({ username, onClose, onSuccess }: Props) {
  const handleDelete = async () => {
    try {
      await api.del(ENDPOINTS.USER_BY_USERNAME(username));
      onSuccess();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : "Failed to delete user.");
      onClose();
    }
  };

  return (
    <Modal open title="Delete User" onConfirm={handleDelete} onCancel={onClose} confirmLabel="Delete" danger>
      <p style={{ fontSize: "14px", color: "#6b7280", margin: 0 }}>
        Are you sure you want to delete <strong style={{ color: "#111827" }}>{username}</strong>? This cannot be undone.
      </p>
    </Modal>
  );
}