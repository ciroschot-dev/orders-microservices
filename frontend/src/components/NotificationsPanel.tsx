import { api } from "../api";
import { usePolling } from "../hooks/usePolling";
import { Card, EmptyState, ErrorBanner, Spinner } from "./ui";

export function NotificationsPanel() {
  const { data: notifications, error, loading } = usePolling(
    api.getNotifications,
    2000,
  );

  const sorted = notifications
    ? [...notifications].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    : [];

  return (
    <Card>
      <header className="flex items-center justify-between border-b border-[var(--color-border)] px-5 py-3.5">
        <div className="flex items-center gap-2">
          <span className="h-2 w-2 animate-pulse rounded-full bg-[var(--color-accent)]" />
          <h2 className="font-semibold">Notificaciones en vivo</h2>
        </div>
        <span className="text-xs text-[var(--color-fg-muted)]">
          {sorted.length} · vía RabbitMQ (fan-out)
        </span>
      </header>

      {loading && !notifications ? (
        <Spinner />
      ) : error ? (
        <div className="p-5">
          <ErrorBanner message={error} />
        </div>
      ) : sorted.length > 0 ? (
        <ul className="divide-y divide-[var(--color-border)]">
          {sorted.map((n) => (
            <li key={n.id} className="flex items-start gap-3 px-5 py-3.5">
              <span className="mt-1 rounded-md bg-[var(--color-accent)]/15 px-2 py-0.5 text-xs font-semibold text-[var(--color-accent)]">
                #{n.orderId}
              </span>
              <div className="min-w-0">
                <p className="text-sm">{n.message}</p>
                <p className="tabular mt-0.5 text-xs text-[var(--color-fg-muted)]">
                  {new Date(n.createdAt).toLocaleString()}
                </p>
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <EmptyState>
          <p className="font-medium text-[var(--color-fg)]">Sin notificaciones</p>
          <p>Cuando crees una orden, el notification-service registra el aviso acá.</p>
        </EmptyState>
      )}
    </Card>
  );
}
