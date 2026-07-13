import type { ButtonHTMLAttributes, ReactNode } from "react";
import type { OrderStatus } from "../types";

// Primitivos compartidos, todos sobre los tokens del design system (Dark OLED).

export function Card({
  children,
  className = "",
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={`rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] ${className}`}
    >
      {children}
    </div>
  );
}

type ButtonVariant = "primary" | "ghost" | "danger";

export function Button({
  variant = "primary",
  className = "",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant }) {
  const base =
    "inline-flex items-center justify-center gap-2 rounded-lg px-3.5 py-2 text-sm font-medium transition-colors duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] disabled:cursor-not-allowed disabled:opacity-40 cursor-pointer";
  const variants: Record<ButtonVariant, string> = {
    primary:
      "bg-[var(--color-accent)] text-[var(--color-accent-fg)] hover:brightness-110",
    ghost:
      "border border-[var(--color-border-strong)] text-[var(--color-fg)] hover:bg-[var(--color-muted)]",
    danger:
      "border border-[var(--color-status-cancelled)]/40 text-[var(--color-status-cancelled)] hover:bg-[var(--color-status-cancelled)]/10",
  };
  return <button className={`${base} ${variants[variant]} ${className}`} {...props} />;
}

const STATUS_COLOR: Record<OrderStatus, string> = {
  PENDING: "var(--color-status-pending)",
  CONFIRMED: "var(--color-status-confirmed)",
  PREPARING: "var(--color-status-preparing)",
  DELIVERED: "var(--color-status-delivered)",
  CANCELLED: "var(--color-status-cancelled)",
};

export function StatusBadge({ status }: { status: OrderStatus }) {
  const color = STATUS_COLOR[status];
  return (
    <span
      className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold"
      style={{ color, backgroundColor: `${color}1a` }}
    >
      <span
        className="h-1.5 w-1.5 rounded-full"
        style={{ backgroundColor: color }}
        aria-hidden
      />
      {status}
    </span>
  );
}

export function Field({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <label className="flex flex-col gap-1.5 text-sm">
      <span className="font-medium text-[var(--color-fg-muted)]">{label}</span>
      {children}
    </label>
  );
}

export function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className="rounded-lg border border-[var(--color-border)] bg-[var(--color-bg)] px-3 py-2 text-[var(--color-fg)] outline-none transition-colors focus:border-[var(--color-accent)]"
      {...props}
    />
  );
}

export function EmptyState({ children }: { children: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-1 px-6 py-12 text-center text-sm text-[var(--color-fg-muted)]">
      {children}
    </div>
  );
}

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 px-6 py-8 text-sm text-[var(--color-fg-muted)]">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-[var(--color-border-strong)] border-t-[var(--color-accent)]" />
      {label ?? "Cargando…"}
    </div>
  );
}

export function ErrorBanner({ message }: { message: string }) {
  return (
    <div
      role="alert"
      className="rounded-lg border border-[var(--color-status-cancelled)]/40 bg-[var(--color-status-cancelled)]/10 px-4 py-2.5 text-sm text-[var(--color-status-cancelled)]"
    >
      {message}
    </div>
  );
}
