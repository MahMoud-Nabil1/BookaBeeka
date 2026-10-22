import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import api from '../config/api';

/**
 * Periodically polls the lightweight in-memory catalog version endpoint
 * (/api/availability/catalog-version) every 20 seconds and on tab focus.
 * When the version number increments (indicating a hotel suspension, unsuspension,
 * deletion, or inventory modification), it triggers an invalidation of the
 * available-rooms and room-detail query caches.
 */
export function useCatalogSync(pollIntervalMs = 20000) {
  const queryClient = useQueryClient();
  const currentVersionRef = useRef<number | null>(null);

  useEffect(() => {
    let isMounted = true;

    const checkCatalogVersion = async () => {
      try {
        const res = await api.get('/api/availability/catalog-version');
        if (!isMounted) return;
        const latestVersion = res.data?.version;

        if (typeof latestVersion === 'number') {
          if (
            currentVersionRef.current !== null &&
            latestVersion > currentVersionRef.current
          ) {
            // Catalog version changed! Invalidate catalog queries to auto-sync
            queryClient.invalidateQueries({ queryKey: ['available-rooms'] });
            queryClient.invalidateQueries({ queryKey: ['room-detail'] });
          }
          currentVersionRef.current = latestVersion;
        }
      } catch {
        // Silently ignore background catalog version poll failures
      }
    };

    // Initial check
    checkCatalogVersion();

    // Periodic timer
    const intervalId = setInterval(checkCatalogVersion, pollIntervalMs);

    // Tab focus check
    const handleFocus = () => {
      checkCatalogVersion();
    };
    window.addEventListener('focus', handleFocus);

    return () => {
      isMounted = false;
      clearInterval(intervalId);
      window.removeEventListener('focus', handleFocus);
    };
  }, [queryClient, pollIntervalMs]);
}
