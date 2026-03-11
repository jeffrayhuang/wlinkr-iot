import { useEffect, useState, useCallback } from 'react';
import type { Device, Page } from '../types';
import { devicesApi } from '../api/devices';

export function useDevices(page = 0) {
  const [data, setData] = useState<Page<Device> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(() => {
    setLoading(true);
    devicesApi
      .list(page)
      .then((res) => setData(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { data, loading, error, refetch: fetch };
}
