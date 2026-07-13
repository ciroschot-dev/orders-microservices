// Wrapper mínimo sobre fetch. Todo cuelga de /api, que Vite proxya al gateway (:8080).
import type {
  Order,
  OrderRequest,
  OrderStatus,
  Product,
  ProductRequest,
  Notification,
} from "./types";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`/api${path}`, {
    headers: { "Content-Type": "application/json" },
    ...init,
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`${res.status} ${res.statusText}${body ? ` — ${body}` : ""}`);
  }
  // 204 No Content (delete) no trae body.
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  // Productos
  getProducts: () => request<Product[]>("/products"),
  createProduct: (p: ProductRequest) =>
    request<Product>("/products", { method: "POST", body: JSON.stringify(p) }),
  updateProduct: (id: number, p: ProductRequest) =>
    request<Product>(`/products/${id}`, { method: "PUT", body: JSON.stringify(p) }),
  deleteProduct: (id: number) =>
    request<void>(`/products/${id}`, { method: "DELETE" }),

  // Órdenes
  getOrders: () => request<Order[]>("/orders"),
  createOrder: (o: OrderRequest) =>
    request<Order>("/orders", { method: "POST", body: JSON.stringify(o) }),
  updateOrderStatus: (id: number, status: OrderStatus) =>
    request<Order>(`/orders/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    }),

  // Notificaciones
  getNotifications: () => request<Notification[]>("/notifications"),
};
