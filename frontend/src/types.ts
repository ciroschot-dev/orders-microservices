// Espejo de los DTOs del backend (order-service / inventory-service / notification-service).

export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PREPARING"
  | "DELIVERED"
  | "CANCELLED";

export const ORDER_STATUSES: OrderStatus[] = [
  "PENDING",
  "CONFIRMED",
  "PREPARING",
  "DELIVERED",
  "CANCELLED",
];

export interface Product {
  id: number;
  name: string;
  sku: string;
  availableQuantity: number;
}

export interface ProductRequest {
  name: string;
  sku: string;
  availableQuantity: number;
}

export interface OrderItem {
  id: number;
  productName: string;
  productId: number;
  quantity: number;
  unitPrice: number;
}

export interface Order {
  id: number;
  customerName: string;
  status: OrderStatus;
  createdAt: string;
  items: OrderItem[];
}

// Lo que el backend exige al crear una orden. Ojo: unitPrice es obligatorio aunque
// el inventory no modele precio — lo completamos en el form con un default.
export interface OrderItemRequest {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface OrderRequest {
  customerName: string;
  items: OrderItemRequest[];
}

export interface Notification {
  id: string;
  orderId: number;
  message: string;
  createdAt: string;
}
