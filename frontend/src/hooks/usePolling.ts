import { useCallback, useEffect, useRef, useState } from "react";

interface PollingState<T> {
  data: T | undefined;
  error: string | undefined;
  loading: boolean;
  refetch: () => void;
}

// Trae datos al montar y cada `intervalMs`. Pausa cuando la pestaña está oculta
// (no tiene sentido pollear si nadie mira). Devuelve refetch para forzar a mano.
export function usePolling<T>(
  fetcher: () => Promise<T>,
  intervalMs = 2000,
): PollingState<T> {
  const [data, setData] = useState<T>();
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(true);
  const fetcherRef = useRef(fetcher);
  fetcherRef.current = fetcher;

  const load = useCallback(async () => {
    try {
      const result = await fetcherRef.current();
      setData(result);
      setError(undefined);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    let timer: number | undefined;
    const tick = () => {
      if (!document.hidden) load();
    };
    timer = window.setInterval(tick, intervalMs);
    return () => window.clearInterval(timer);
  }, [load, intervalMs]);

  return { data, error, loading, refetch: load };
}
