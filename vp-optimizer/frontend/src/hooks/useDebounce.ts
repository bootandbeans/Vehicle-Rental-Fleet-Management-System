import { useEffect, useState } from 'react';

/** Debounces fast changing values (search boxes) to avoid a request per keystroke. */
export function useDebounce<T>(value: T, delayMillis = 350): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const handle = window.setTimeout(() => setDebounced(value), delayMillis);
    return () => window.clearTimeout(handle);
  }, [value, delayMillis]);

  return debounced;
}
