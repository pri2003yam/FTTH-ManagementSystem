// ===========================
// Enums
// ===========================

export type ConnectionStatus = "ACTIVE" | "DISCONNECTED";
export type CustomerStatus = "ACTIVE" | "INACTIVE" | "DELETED";
export type BillStatus = "GENERATED" | "PAID" | "OVERDUE";
export type PortStatus = "AVAILABLE" | "ASSIGNED" | "FAULTY" | "DISABLED";
export type EmailType = "ORDER_CONFIRMATION" | "BILL" | "OLT_ALERT" | "DISCONNECT" | "SERVICE_MOVE" | "PLAN_CHANGE" | "PLAN_ADMIN";

// ===========================
// Core Models
// ===========================

export interface Plan {
  planId: number;
  planName: string;
  speedLabel: string;
  dataLimitLabel: string;
  ottCount: number;
  monthlyPrice: number;
  oltType: string;
  active: boolean;
  customerCount?: number;
}

export interface Customer {
  customerId: number;
  customerCode: string;
  fullName: string;
  email: string;
  salary: number;
  status: CustomerStatus;
}

export interface CustomerConnection {
  connectionId: number;
  customerId: number;
  planId: number;
  portId: number;
  serviceAreaId: number;
  connectionStatus: ConnectionStatus;
  activatedOn: string;
  disconnectedOn?: string;
  billingDay: number;
}

export interface Bill {
  billId: number;
  billNo: string;
  customerId: number;
  connectionId: number;
  billDate: string;
  dueDate: string;
  planCharge: number;
  gstAmount: number;
  totalAmount: number;
  billStatus: BillStatus;
}

export interface User {
  userId: number;
  username: string;
  roleId: number;
  active: boolean;
}

// ===========================
// Inventory Models
// ===========================

export interface OltInventoryDTO {
  oltCode: string;
  pincode: string;
  oltType: string;
  splitterCount: number;
  totalPorts: number;
  availablePorts: number;
}

export interface SplitterDetail {
  splitterNumber: number;
  splitterCode: string | null;
  totalPorts: number;
  availablePorts: number;
}

export interface OltDetail {
  oltCode: string;
  oltType: string;
  active: boolean;
  totalPorts: number;
  availablePorts: number;
  splitters: SplitterDetail[];
}

export interface InventoryConfig {
  maxSplitters: number;
  portsPerSplitter: number;
}

// ===========================
// Capacity
// ===========================

export interface CapacityRow {
  pincode: string;
  oltType: string;
  totalPorts: number;
  usedPorts: number;
  freePorts: number;
  utilization: number;
  splitterCount: number;
}

// ===========================
// API Request Types
// ===========================

export interface PortDetail {
  splitterNumber: number;
  portNumber: number;
  portStatus: PortStatus;
  customerCode: string | null;
}

export interface AddOltRequest {
  pincode: string;
  oltType: string;
  splitterCount: number;
}

export interface AddConnectionRequest {
  customerName: string;
  planId: number;
  salary: number;
  pincode: number;
  oltType: string;
  email: string;
}

// ===========================
// API Response Types
// ===========================

export interface ApiMessage {
  message: string;
}

export interface AddOltResponse {
  oltCode: string;
  message: string;
}

// ===========================
// Work Request Models
// ===========================

export type WorkRequestStatus = "NEW" | "ACCEPTED" | "IN_PROGRESS" | "RESOLVED" | "CLOSED";
export type WorkRequestActionType = "ADD_OLT" | "REMOVE_OLT" | "ADD_SPLITTER" | "REMOVE_SPLITTER";

export interface WorkRequest {
  wrId: number;
  pincode: string;
  oltType: string;
  actionType: WorkRequestActionType;
  status: WorkRequestStatus;
  raisedBy: string;
  assignedTo: string | null;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface WRNotification {
  notificationId: number;
  wrId: number;
  message: string;
  isRead: boolean;
  createdAt: string;
}
