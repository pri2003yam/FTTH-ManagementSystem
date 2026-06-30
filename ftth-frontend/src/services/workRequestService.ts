import { api } from "./apiClient";
import { ENDPOINTS } from "./endpoints";
import type { WorkRequest, WRNotification } from "../types/models";

interface CreateWRRequest {
  pincode: string;
  oltType: string;
  actionType: string;
  raisedBy: string;
  description: string;
}

interface TransitionRequest {
  newStatus: string;
  actor: string;
}

interface NotificationsResponse {
  unreadCount: number;
  notifications: WRNotification[];
}

export const workRequestService = {
  getAll: (raisedBy?: string) =>
    api.get<WorkRequest[]>(raisedBy ? `${ENDPOINTS.WORK_REQUESTS}?raisedBy=${raisedBy}` : ENDPOINTS.WORK_REQUESTS),

  getOpen: () =>
    api.get<WorkRequest[]>(ENDPOINTS.WORK_REQUESTS_OPEN),

  getById: (id: number) =>
    api.get<WorkRequest>(ENDPOINTS.WORK_REQUEST_BY_ID(id)),

  create: (data: CreateWRRequest) =>
    api.post<{ message: string; wrId: number }>(ENDPOINTS.WORK_REQUESTS, data),

  transition: (id: number, data: TransitionRequest) =>
    api.post<{ message: string; workRequest: WorkRequest }>(ENDPOINTS.WORK_REQUEST_TRANSITION(id), data),

  getNotifications: (username: string) =>
    api.get<NotificationsResponse>(`${ENDPOINTS.WORK_REQUEST_NOTIFICATIONS}?username=${username}`),

  markNotificationRead: (id: number) =>
    api.post<{ message: string }>(ENDPOINTS.WORK_REQUEST_NOTIFICATION_READ(id), {}),
};
