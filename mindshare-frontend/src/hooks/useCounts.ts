import { useQuery } from "@tanstack/react-query";
import { getCounter } from "../api/counter";
import type { EntityType } from "../types/counter";

export function useCounts(entityType: EntityType, entityId: number) {
  return useQuery({
    queryKey: ["counts", entityType, entityId],
    queryFn: () => getCounter(entityType, entityId, "like,fav"),
    staleTime: 30_000,
    enabled: !!entityId,
  });
}
