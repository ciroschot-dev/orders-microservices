import { useState } from "react";
import { ProductsPanel } from "./components/ProductsPanel";
import { CreateOrderPanel } from "./components/CreateOrderPanel";
import { OrdersPanel } from "./components/OrdersPanel";
import { NotificationsPanel } from "./components/NotificationsPanel";

const TABS = [
  { id: "products", label: "Inventario", Panel: ProductsPanel },
  { id: "create", label: "Crear orden", Panel: CreateOrderPanel },
  { id: "orders", label: "Órdenes", Panel: OrdersPanel },
  { id: "notifications", label: "Notificaciones", Panel: NotificationsPanel },
] as const;

type TabId = (typeof TABS)[number]["id"];

export function App() {
  const [active, setActive] = useState<TabId>("products");
  const ActivePanel = TABS.find((t) => t.id === active)!.Panel;

  return (
    <div className="mx-auto min-h-dvh max-w-6xl px-4 py-6 sm:px-6">
      <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight">
            <span className="text-[var(--color-accent)]">◆</span> OrderFlow
          </h1>
          <p className="text-sm text-[var(--color-fg-muted)]">
            Panel de microservicios event-driven · Spring Boot + RabbitMQ
          </p>
        </div>
        <nav
          className="flex flex-wrap gap-1 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-1"
          role="tablist"
        >
          {TABS.map((t) => (
            <button
              key={t.id}
              role="tab"
              aria-selected={active === t.id}
              onClick={() => setActive(t.id)}
              className={`cursor-pointer rounded-lg px-3.5 py-1.5 text-sm font-medium transition-colors duration-150 ${
                active === t.id
                  ? "bg-[var(--color-accent)] text-[var(--color-accent-fg)]"
                  : "text-[var(--color-fg-muted)] hover:bg-[var(--color-muted)] hover:text-[var(--color-fg)]"
              }`}
            >
              {t.label}
            </button>
          ))}
        </nav>
      </header>

      <main>
        <ActivePanel />
      </main>
    </div>
  );
}
