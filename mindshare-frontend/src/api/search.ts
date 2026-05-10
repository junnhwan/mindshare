import apiClient from "./client";
import type { SearchResponse, SuggestResponse } from "../types/search";

/** GET /search?q=&size=&tags=&after= */
export async function search(params: {
  q: string;
  size?: number;
  tags?: string;
  after?: string;
}): Promise<SearchResponse> {
  return apiClient.get("/search", { params }) as Promise<SearchResponse>;
}

/** GET /search/suggest?prefix=&size= */
export async function suggest(params: {
  prefix: string;
  size?: number;
}): Promise<SuggestResponse> {
  return apiClient.get("/search/suggest", {
    params,
  }) as Promise<SuggestResponse>;
}
