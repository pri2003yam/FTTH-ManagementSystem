import { Routes, Route, Navigate } from "react-router-dom";
import AppLayout from "./AppLayout";

import Dashboard from "../pages/Dashboard/Dashboard";
import Connections from "../pages/Connections/Connections";
import Customers from "../pages/CustomerScreen/CustomerScreen";
import Inventory from "../pages/Inventory/Inventory";
import Plans from "../pages/Plans/Plans";
import Catalog from "../pages/Catalog/Catalog";
import Capacity from "../pages/Capacity/Capacity";
import Users from "../pages/Users/Users";
import Maintenance from "../pages/Maintenance/Maintenance";
import WorkRequests from "../pages/WorkRequests/WorkRequests";
import Login from "../pages/Login/Login";

export default function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route element={<AppLayout />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/connections" element={<Connections />} />
        <Route path="/customers" element={<Customers />} />
        <Route path="/inventory" element={<Inventory />} />
        <Route path="/plans" element={<Plans />} />
        <Route path="/catalog" element={<Catalog />} />
        <Route path="/capacity" element={<Capacity />} />
        <Route path="/users" element={<Users />} />
        <Route path="/maintenance" element={<Maintenance />} />
        <Route path="/work-requests" element={<WorkRequests />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" />} />
    </Routes>
  );
}
