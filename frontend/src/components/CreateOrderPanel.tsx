import { useState } from "react";
import { api } from "../api";
import { usePolling } from "../hooks/usePolling";
import type { Order, OrderItemRequest } from "../types";
import {
  Button,
  Card,
  EmptyState,
  ErrorBanner,
  Field,
  Input,
  Spinner,
  StatusBadge,
} from "./ui";

const DEFAULT_PRICE = 10;

export function CreateOrderPanel() {
  const { data: products, loading } = usePolling(api.getProducts, 4000);
  const [customerName, setCustomerName] = useState("");
  const [items, setItems] = useState<OrderItemRequest[]>([]);
  const [selectedId, setSelectedId] = useState<number | "">("");
  const [qty, setQty] = useState<number | "">(1);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();
  const [created, setCreated] = useState<Order | null>(null);

  const addItem = () => {
    if (selectedId === "") return;
    const product = products?.find((p) => p.id === selectedId);
    if (!product) return;
    setItems([
      ...items,
      {
        productId: product.id,
        productName: product.name,
        quantity: Number(qty) || 1,
        unitPrice: DEFAULT_PRICE,
      },
    ]);
    setSelectedId("");
    setQty(1);
  };

  const removeItem = (idx: number) =>
    setItems(items.filter((_, i) => i !== idx));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(undefined);
    try {
      const order = await api.createOrder({ customerName, items });
      setCreated(order);
      setCustomerName("");
      setItems([]);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  };

  if (loading && !products) return <Spinner />;

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <Card className="p-5">
        <h2 className="mb-4 font-semibold">Nueva orden</h2>
        <form onSubmit={submit} className="flex flex-col gap-4">
          <Field label="Cliente">
            <Input
              required
              value={customerName}
              onChange={(e) => setCustomerName(e.target.value)}
              placeholder="Ada Lovelace"
            />
          </Field>

          <div className="rounded-lg border border-[var(--color-border)] p-3.5">
            <p className="mb-2.5 text-sm font-medium text-[var(--color-fg-muted)]">
              Agregar ítem
            </p>
            <div className="flex flex-wrap items-end gap-2.5">
              <label className="flex flex-1 flex-col gap-1.5 text-sm">
                <span className="text-[var(--color-fg-muted)]">Producto</span>
                <select
                  value={selectedId}
                  onChange={(e) =>
                    setSelectedId(e.target.value ? Number(e.target.value) : "")
                  }
                  className="rounded-lg border border-[var(--color-border)] bg-[var(--color-bg)] px-3 py-2 text-[var(--color-fg)] outline-none focus:border-[var(--color-accent)]"
                >
                  <option value="">Elegir…</option>
                  {products?.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} (stock {p.availableQuantity})
                    </option>
                  ))}
                </select>
              </label>
              <label className="flex w-24 flex-col gap-1.5 text-sm">
                <span className="text-[var(--color-fg-muted)]">Cantidad</span>
                <Input
                  type="number"
                  min={1}
                  value={qty}
                  onChange={(e) =>
                    setQty(e.target.value === "" ? "" : Number(e.target.value))
                  }
                />
              </label>
              <Button
                type="button"
                variant="ghost"
                onClick={addItem}
                disabled={selectedId === ""}
              >
                Agregar
              </Button>
            </div>
          </div>

          {items.length > 0 ? (
            <ul className="flex flex-col gap-1.5">
              {items.map((it, idx) => (
                <li
                  key={idx}
                  className="flex items-center justify-between rounded-lg bg-[var(--color-muted)] px-3 py-2 text-sm"
                >
                  <span>
                    {it.productName}{" "}
                    <span className="tabular text-[var(--color-fg-muted)]">
                      ×{it.quantity}
                    </span>
                  </span>
                  <button
                    type="button"
                    onClick={() => removeItem(idx)}
                    className="cursor-pointer text-xs text-[var(--color-status-cancelled)] hover:underline"
                  >
                    quitar
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-[var(--color-fg-muted)]">
              Todavía no agregaste ítems.
            </p>
          )}

          {error && <ErrorBanner message={error} />}

          <Button type="submit" disabled={busy || items.length === 0 || !customerName}>
            {busy ? "Creando…" : "Crear orden"}
          </Button>
          <p className="text-xs text-[var(--color-fg-muted)]">
            La orden nace en <span className="font-semibold">PENDING</span>. Un instante
            después, async vía RabbitMQ, el stock se descuenta y la orden pasa sola a{" "}
            <span className="font-semibold">CONFIRMED</span> (o CANCELLED si perdió una carrera
            por las últimas unidades).
          </p>
        </form>
      </Card>

      {created ? (
        <LiveEffect order={created} />
      ) : (
        <Card className="flex items-center justify-center p-5">
          <EmptyState>
            <p className="font-medium text-[var(--color-fg)]">Efecto en vivo</p>
            <p>
              Creá una orden y mirá acá cómo baja el stock de los productos pedidos, sin
              recargar.
            </p>
          </EmptyState>
        </Card>
      )}
    </div>
  );
}

// El "momento wow": tras crear la orden, pollea el stock de los productos pedidos y el
// estado de la orden para verlos cambiar en vivo (prueba visual del flujo event-driven:
// stock baja + la orden pasa sola de PENDING a CONFIRMED).
function LiveEffect({ order }: { order: Order }) {
  const { data: products } = usePolling(api.getProducts, 1500);
  const { data: orders } = usePolling(api.getOrders, 1500);
  const orderedIds = new Set(order.items.map((i) => i.productId));
  const watched = products?.filter((p) => orderedIds.has(p.id));
  // El estado en vivo lo tomamos de la lista (el `order` del prop quedó congelado al crear).
  const liveStatus = orders?.find((o) => o.id === order.id)?.status ?? order.status;

  return (
    <Card className="p-5">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-semibold">Orden #{order.id} creada</h2>
        <StatusBadge status={liveStatus} />
      </div>
      <p className="mb-4 text-sm text-[var(--color-fg-muted)]">
        Cliente: <span className="text-[var(--color-fg)]">{order.customerName}</span>
      </p>
      <p className="mb-2 text-xs uppercase tracking-wide text-[var(--color-fg-muted)]">
        Stock en vivo de lo pedido
      </p>
      <ul className="flex flex-col gap-2">
        {watched?.map((p) => (
          <li
            key={p.id}
            className="flex items-center justify-between rounded-lg bg-[var(--color-muted)] px-3.5 py-2.5 text-sm"
          >
            <span>{p.name}</span>
            <span className="tabular flex items-center gap-2">
              <span className="text-[var(--color-fg-muted)]">stock</span>
              <span
                className={
                  p.availableQuantity === 0
                    ? "text-[var(--color-status-cancelled)]"
                    : "text-[var(--color-accent)]"
                }
              >
                {p.availableQuantity}
              </span>
            </span>
          </li>
        ))}
      </ul>
      <p className="mt-4 text-xs text-[var(--color-fg-muted)]">
        Revisá también las pestañas <span className="text-[var(--color-fg)]">Órdenes</span>{" "}
        y <span className="text-[var(--color-fg)]">Notificaciones</span>.
      </p>
    </Card>
  );
}
