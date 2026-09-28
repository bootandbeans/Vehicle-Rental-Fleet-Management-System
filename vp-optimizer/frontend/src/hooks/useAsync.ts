import { useCallback, useEffect, useRef, useState } from 'react';
import { messageOf } from '../services/errors';

export interface AsyncState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  reload: () => void;
  setData: (data: T | null) => void;
}

/**
 * Minimal data-fetching hook: loading / error / reload, with stale responses ignored.
 *
 * Keeps REST calls out of the components by pairing it with the API adapter from `useApp()`.
 */
export function useAsync<T>(loader: () => Promise<T>, deps: unknown[] = [], options: { enabled?: boolean } = {}): AsyncState<T> {
  const enabled = options.enabled ?? true;
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState<string | null>(null);
  const [token, setToken] = useState(0);
  const requestId = useRef(0);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  useEffect(() => {
    if (!enabled) {
      setLoading(false);
      return;
    }
    const current = requestId.current + 1;
    requestId.current = current;
    setLoading(true);
    setError(null);

    loaderRef
      .current()
      .then((result) => {
        if (requestId.current === current) {
          setData(result);
        }
      })
      .catch((failure: unknown) => {
        if (requestId.current === current) {
          setError(messageOf(failure));
        }
      })
      .finally(() => {
        if (requestId.current === current) {
          setLoading(false);
        }
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, token, enabled]);

  const reload = useCallback(() => setToken((value) => value + 1), []);

  return { data, loading, error, reload, setData };
}
