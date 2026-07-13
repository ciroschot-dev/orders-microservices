import { useState } from "react";
import { api } from "../api";
import { usePolling } from "../hooks/usePolling";
import type { Product } from "../types";
import {
  Button,
  Card,
  EmptyState,
  ErrorBanner,
  Field,
  Input,
  Spinner,
} from "./ui";

// availableQuantity puede quedar "" mientras el usuario escribe (así el 0 no se
// "pega" y se puede borrar). Al enviar lo convertimos a número.
const EMPTY_FORM: { name: string; sku: string; availableQuantity: number | "" } = {
  name: "",
  sku: "",
  availableQuantity: 0,
};

export function ProductsPanel() {
  const { data: products, error, loading, refetch } = usePolling(
    api.getProducts,
    3000,
  );
  const [form, setForm] = useState<typeof EMPTY_FORM>(EMPTY_FORM);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [formError, setFormError] = useState<string>();

  const startEdit = (p: Product) => {
    setEditingId(p.id);
    setForm({ name: p.name, sku: p.sku, availableQuantity: p.availableQuantity });
    setFormError(undefined);
  };

  const cancelEdit = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError(undefined);
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setFormError(undefined);
    try {
      const payload = { ...form, availableQuantity: Number(form.availableQuantity) || 0 };
      if (editingId !== null) {
        await api.updateProduct(editingId, payload);
      } else {
        await api.createProduct(payload);
      }
      cancelEdit();
      refetch();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: number) => {
    setBusy(true);
    try {
      await api.deleteProduct(id);
      if (editingId === id) cancelEdit();
      refetch();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <Card>
        <header className="flex items-center justify-between border-b border-[var(--color-border)] px-5 py-3.5">
          <h2 className="font-semibold">Inventario</h2>
          <span className="text-xs text-[var(--color-fg-muted)]">
            {products?.length ?? 0} productos · refresca solo
          </span>
        </header>

        {loading && !products ? (
          <Spinner />
        ) : error ? (
          <div className="p-5">
            <ErrorBanner message={error} />
          </div>
        ) : products && products.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase tracking-wide text-[var(--color-fg-muted)]">
                  <th className="px-5 py-2.5 font-medium">Producto</th>
                  <th className="px-5 py-2.5 font-medium">SKU</th>
                  <th className="px-5 py-2.5 text-right font-medium">Stock</th>
                  <th className="px-5 py-2.5 text-right font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {products.map((p) => (
                  <tr
                    key={p.id}
                    className="border-t border-[var(--color-border)] hover:bg-[var(--color-muted)]/40"
                  >
                    <td className="px-5 py-3 font-medium">{p.name}</td>
                    <td className="px-5 py-3 font-mono text-xs text-[var(--color-fg-muted)]">
                      {p.sku}
                    </td>
                    <td className="tabular px-5 py-3 text-right">
                      <span
                        className={
                          p.availableQuantity === 0
                            ? "text-[var(--color-status-cancelled)]"
                            : ""
                        }
                      >
                        {p.availableQuantity}
                      </span>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex justify-end gap-2">
                        <Button variant="ghost" onClick={() => startEdit(p)}>
                          Editar
                        </Button>
                        <Button
                          variant="danger"
                          onClick={() => remove(p.id)}
                          disabled={busy}
                        >
                          Borrar
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState>
            <p className="font-medium text-[var(--color-fg)]">Sin productos todavía</p>
            <p>Creá el primero con el formulario de la derecha.</p>
          </EmptyState>
        )}
      </Card>

      <Card className="h-fit p-5">
        <h3 className="mb-4 font-semibold">
          {editingId !== null ? "Editar producto" : "Nuevo producto"}
        </h3>
        <form onSubmit={submit} className="flex flex-col gap-3.5">
          <Field label="Nombre">
            <Input
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="Teclado mecánico"
            />
          </Field>
          <Field label="SKU">
            <Input
              required
              value={form.sku}
              onChange={(e) => setForm({ ...form, sku: e.target.value })}
              placeholder="KB-001"
            />
          </Field>
          <Field label="Stock disponible">
            <Input
              required
              type="number"
              min={0}
              value={form.availableQuantity}
              onChange={(e) =>
                setForm({
                  ...form,
                  availableQuantity:
                    e.target.value === "" ? "" : Number(e.target.value),
                })
              }
            />
          </Field>
          {formError && <ErrorBanner message={formError} />}
          <div className="mt-1 flex gap-2">
            <Button type="submit" disabled={busy}>
              {editingId !== null ? "Guardar" : "Crear"}
            </Button>
            {editingId !== null && (
              <Button type="button" variant="ghost" onClick={cancelEdit}>
                Cancelar
              </Button>
            )}
          </div>
        </form>
      </Card>
    </div>
  );
}
