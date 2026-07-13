import { useState } from "react";
import { api } from "../api";
import { usePolling } from "../hooks/usePolling";
import { ORDER_STATUSES, type Order, type OrderStatus } from "../types";
import {
  Card,
  EmptyState,
  ErrorBanner,
  Spinner,
  StatusBadge,
} from "./ui";

export function OrdersPanel() {
  const { data: orders, error, loading, refetch } = usePolling(api.getOrders, 2500);
  const [expanded, setExpanded] = useState<number | null>(null);
  const [updating, setUpdating] = useState<number | null>(null);

  const changeStatus = async (id: number, status: OrderStatus) => {
    setUpdating(id);
    try {
      await api.updateOrderStatus(id, status);
      refetch();
    } finally {
      setUpdating(null);
    }
  };

  return (
    <Card>
      <header className="flex items-center justify-between border-b border-[var(--color-border)] px-5 py-3.5">
        <h2 className="font-semibold">Órdenes</h2>
        <span className="text-xs text-[var(--color-fg-muted)]">
          {orders?.length ?? 0} · refresca solo
        </span>
      </header>

      {loading && !orders ? (
        <Spinner />
      ) : error ? (
        <div className="p-5">
          <ErrorBanner message={error} />
        </div>
      ) : orders && orders.length > 0 ? (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs uppercase tracking-wide text-[var(--color-fg-muted)]">
                <th className="px-5 py-2.5 font-medium">#</th>
                <th className="px-5 py-2.5 font-medium">Cliente</th>
                <th className="px-5 py-2.5 font-medium">Estado</th>
                <th className="px-5 py-2.5 font-medium">Cambiar a</th>
                <th className="px-5 py-2.5 font-medium" />
              </tr>
            </thead>
            <tbody>
              {[...orders].reverse().map((o) => (
                <OrderRow
                  key={o.id}
                  order={o}
                  expanded={expanded === o.id}
                  updating={updating === o.id}
                  onToggle={() => setExpanded(expanded === o.id ? null : o.id)}
                  onChangeStatus={(s) => changeStatus(o.id, s)}
                />
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <EmptyState>
          <p className="font-medium text-[var(--color-fg)]">Sin órdenes todavía</p>
          <p>Creá una desde la pestaña Crear orden.</p>
        </EmptyState>
      )}
    </Card>
  );
}

function OrderRow({
  order,
  expanded,
  updating,
  onToggle,
  onChangeStatus,
}: {
  order: Order;
  expanded: boolean;
  updating: boolean;
  onToggle: () => void;
  onChangeStatus: (s: OrderStatus) => void;
}) {
  return (
    <>
      <tr className="border-t border-[var(--color-border)] hover:bg-[var(--color-muted)]/40">
        <td className="tabular px-5 py-3 font-mono text-[var(--color-fg-muted)]">
          {order.id}
        </td>
        <td className="px-5 py-3 font-medium">{order.customerName}</td>
        <td className="px-5 py-3">
          <StatusBadge status={order.status} />
        </td>
        <td className="px-5 py-3">
          <select
            value={order.status}
            disabled={updating}
            onChange={(e) => onChangeStatus(e.target.value as OrderStatus)}
            className="cursor-pointer rounded-lg border border-[var(--color-border)] bg-[var(--color-bg)] px-2.5 py-1.5 text-xs text-[var(--color-fg)] outline-none focus:border-[var(--color-accent)] disabled:opacity-50"
          >
            {ORDER_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </td>
        <td className="px-5 py-3 text-right">
          <button
            onClick={onToggle}
            className="cursor-pointer text-xs text-[var(--color-fg-muted)] hover:text-[var(--color-fg)]"
          >
            {expanded ? "ocultar" : `${order.items.length} ítems`}
          </button>
        </td>
      </tr>
      {expanded && (
        <tr className="bg-[var(--color-bg)]">
          <td colSpan={5} className="px-5 py-3">
            <ul className="flex flex-col gap-1.5">
              {order.items.map((it) => (
                <li
                  key={it.id}
                  className="tabular flex justify-between text-sm text-[var(--color-fg-muted)]"
                >
                  <span className="text-[var(--color-fg)]">{it.productName}</span>
                  <span>
                    ×{it.quantity} · ${Number(it.unitPrice).toFixed(2)}
                  </span>
                </li>
              ))}
            </ul>
          </td>
        </tr>
      )}
    </>
  );
}
