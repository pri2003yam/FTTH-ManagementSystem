import { useEffect, useState } from "react";
import { api } from "../../services/apiClient";
import { ENDPOINTS } from "../../services/endpoints";
import { useAuth } from "../../context/AuthContext";
import PageWrapper from "../../components/layout/PageWrapper";
import Card from "../../components/ui/Card";
import Table from "../../components/ui/Table";
import Badge from "../../components/ui/Badge";
import Button from "../../components/ui/Button";
import Loader from "../../components/ui/Loader";
import CreateUserForm from "./CreateUserForm";
import EditUserModal from "./EditUserModal";
import DeleteUserModal from "./DeleteUserModal";

interface UserRow {
  username: string;
  role: string;
  status: string;
}

export default function Users() {
  const { role: currentRole } = useAuth();
  const [users, setUsers] = useState<UserRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [showCreate, setShowCreate] = useState(false);
  const [editTarget, setEditTarget] = useState<UserRow | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const data = await api.get<UserRow[]>(ENDPOINTS.USERS);
      setUsers(data);
    } catch {
      setError("Failed to load users.");
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = async (username: string) => {
    try {
      await api.patch(ENDPOINTS.USER_TOGGLE_STATUS(username));
      fetchUsers();
    } catch {
      setError("Failed to toggle user status.");
    }
  };

  useEffect(() => { fetchUsers(); }, []);

  if (loading) return <Loader />;

  return (
    <PageWrapper title="User Management">
      {currentRole === "ADMIN" && (
        <Button onClick={() => setShowCreate(true)} style={{ padding: "6px 14px", fontSize: "13px", alignSelf: "flex-start" }}>
          + Create New User
        </Button>
      )}

      {error && <p style={{ color: "#dc2626", fontSize: "13px" }}>{error}</p>}

      <Card>
        <Table
          keyField="username"
          data={users}
          columns={[
            { key: "username", header: "Username" },
            { key: "role",     header: "Role" },
            {
              key: "status",
              header: "Status",
              render: (r) => <Badge label={r.status} variant={r.status === "Active" ? "success" : "error"} />,
            },
            {
              key: "actions",
              header: "Actions",
              render: (r) =>
                r.role !== "ADMIN" && currentRole === "ADMIN" ? (
                  <div style={{ display: "flex", gap: "12px", alignItems: "center" }}>
                    <button
                      style={{ fontSize: "13px", color: "#2563eb", background: "none", border: "none", cursor: "pointer", padding: 0 }}
                      onClick={() => setEditTarget(r)}
                    >
                      Edit
                    </button>
                    <button
                      style={{ fontSize: "13px", color: "#f59e0b", background: "none", border: "none", cursor: "pointer", padding: 0 }}
                      onClick={() => handleToggle(r.username)}
                    >
                      {r.status === "Active" ? "Disable" : "Enable"}
                    </button>
                    <button
                      style={{ fontSize: "13px", color: "#dc2626", background: "none", border: "none", cursor: "pointer", padding: 0 }}
                      onClick={() => setDeleteTarget(r.username)}
                    >
                      Delete
                    </button>
                  </div>
                ) : (
                  <span style={{ fontSize: "13px", color: "#9ca3af" }}>—</span>
                ),
            },
          ]}
        />
      </Card>

      {showCreate && (
        <CreateUserForm
          onClose={() => setShowCreate(false)}
          onSuccess={() => { setShowCreate(false); fetchUsers(); }}
        />
      )}

      {editTarget && (
        <EditUserModal
          username={editTarget.username}
          currentRole={editTarget.role}
          onClose={() => setEditTarget(null)}
          onSuccess={() => { setEditTarget(null); fetchUsers(); }}
        />
      )}

      {deleteTarget && (
        <DeleteUserModal
          username={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onSuccess={() => { setDeleteTarget(null); fetchUsers(); }}
        />
      )}
    </PageWrapper>
  );
}
