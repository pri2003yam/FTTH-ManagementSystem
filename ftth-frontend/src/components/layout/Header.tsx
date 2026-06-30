import { useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { sidebarItems } from "../layout/sidebarConfig";
import Button from "../ui/Button";
import Badge from "../ui/Badge";
import NotificationBell from "./NotificationBell";

export default function Header() {
  const { user, role, logout } = useAuth();
  const location = useLocation();

  const title = sidebarItems.find((item) => item.path === location.pathname)?.label ?? "";

  return (
    <header className="app-header">
      <div className="app-header__title">{title}</div>
      <div className="app-header__actions">
        {user && (
          <>
            <NotificationBell />
            <span className="app-header__user">{user}</span>
            <Badge label={role} variant="neutral" />
            <Button variant="danger" onClick={logout} className="text-xs px-3 py-1">Logout</Button>
          </>
        )}
      </div>
    </header>
  );
}
