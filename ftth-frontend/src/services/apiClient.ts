const BASE_URL = import.meta.env.VITE_API_URL || "";

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${url}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(error.message || error.error);
  }

  return res.json();
}

export const api = {
  get: <T>(url: string) =>
    request<T>(url),

  post: <T>(url: string, body: unknown) =>
    request<T>(url, { method: "POST", body: JSON.stringify(body) }),

  put: <T>(url: string, body: unknown) =>
    request<T>(url, { method: "PUT", body: JSON.stringify(body) }),

  patch: <T>(url: string, body: unknown = {}) =>
    request<T>(url, { method: "PATCH", body: JSON.stringify(body) }),

  del: <T>(url: string) =>
    request<T>(url, { method: "DELETE" }),
};