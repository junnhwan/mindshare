import apiClient from "./client";
import type { ActionRequest, CounterData, EntityType } from "../types/counter";

/** POST /action/like */
export async function like(body: ActionRequest): Promise<void> {
  return apiClient.post("/action/like", body);
}

/** POST /action/unlike */
export async function unlike(body: ActionRequest): Promise<void> {
  return apiClient.post("/action/unlike", body);
}

/** POST /action/fav */
export async function fav(body: ActionRequest): Promise<void> {
  return apiClient.post("/action/fav", body);
}

/** POST /action/unfav */
export async function unfav(body: ActionRequest): Promise<void> {
  return apiClient.post("/action/unfav", body);
}

/** GET /counter/{etype}/{eid}?metrics= */
export async function getCounter(
  etype: EntityType,
  eid: number,
  metrics?: string,
): Promise<CounterData> {
  return apiClient.get(`/counter/${etype}/${eid}`, {
    params: { metrics },
  }) as Promise<CounterData>;
}
