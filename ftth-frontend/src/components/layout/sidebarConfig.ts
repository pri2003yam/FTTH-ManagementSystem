import type { Role } from "../../types/roles";

export const sidebarItems: {
  label: string;
  icon: string;
  path: string;
  roles: Role[];
}[] = [
  { label: "Dashboard",    icon: "📊", path: "/dashboard",   roles: ["ADMIN", "CSR", "MAINT"] },
  { label: "Connections",  icon: "🔗", path: "/connections",  roles: ["ADMIN", "CSR"] },
  { label: "Customers",    icon: "👥", path: "/customers",   roles: ["ADMIN", "CSR"] },
  { label: "Inventory",    icon: "🗄️", path: "/inventory",   roles: ["ADMIN", "MAINT"] },
  { label: "Plans",        icon: "📋", path: "/plans",       roles: ["ADMIN"] },
  { label: "Capacity",     icon: "📈", path: "/capacity",    roles: ["ADMIN", "MAINT"] },
  { label: "Users",        icon: "🔑", path: "/users",       roles: ["ADMIN"] },
  { label: "Maintenance",  icon: "🔧", path: "/maintenance", roles: ["ADMIN", "MAINT"] },
];
