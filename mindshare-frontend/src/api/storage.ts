import apiClient from "./client";
import type { PresignRequest, PresignResponse } from "../types/storage";

/** POST /storage/presign */
export async function presign(req: PresignRequest): Promise<PresignResponse> {
  return apiClient.post("/storage/presign", req) as Promise<PresignResponse>;
}
