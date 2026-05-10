import apiClient from "./client";
import type { PageResponse } from "../types/api";
import type {
  ContentConfirmRequest,
  DraftCreateResponse,
  FeedItem,
  PatchMetadataRequest,
  PostDetail,
} from "../types/post";

/** POST /knowposts/drafts */
export async function createDraft(): Promise<DraftCreateResponse> {
  return apiClient.post("/knowposts/drafts") as Promise<DraftCreateResponse>;
}

/** POST /knowposts/{id}/content/confirm */
export async function confirmContent(
  id: string,
  body: ContentConfirmRequest,
): Promise<void> {
  return apiClient.post(`/knowposts/${id}/content/confirm`, body);
}

/** PATCH /knowposts/{id} */
export async function patchMetadata(
  id: string,
  body: PatchMetadataRequest,
): Promise<void> {
  return apiClient.patch(`/knowposts/${id}`, body);
}

/** PATCH /knowposts/{id}/top */
export async function toggleTop(id: string, isTop: boolean): Promise<void> {
  return apiClient.patch(`/knowposts/${id}/top`, { isTop });
}

/** PATCH /knowposts/{id}/visibility */
export async function setVisibility(
  id: string,
  visible: string,
): Promise<void> {
  return apiClient.patch(`/knowposts/${id}/visibility`, { visible });
}

/** POST /knowposts/{id}/publish */
export async function publish(id: string): Promise<void> {
  return apiClient.post(`/knowposts/${id}/publish`);
}

/** DELETE /knowposts/{id} */
export async function deleteKnowpost(id: string): Promise<void> {
  return apiClient.delete(`/knowposts/${id}`);
}

/** GET /knowposts/feed?page=&size= */
export async function getFeed(
  page: number,
  size: number,
): Promise<PageResponse<FeedItem>> {
  return apiClient.get("/knowposts/feed", {
    params: { page, size },
  }) as Promise<PageResponse<FeedItem>>;
}

/** GET /knowposts/mine?page=&size=&status= */
export async function getMine(
  page: number,
  size: number,
  status?: string,
): Promise<PageResponse<FeedItem>> {
  return apiClient.get("/knowposts/mine", {
    params: { page, size, status },
  }) as Promise<PageResponse<FeedItem>>;
}

/** GET /knowposts/detail/{id} */
export async function getDetail(id: string): Promise<PostDetail> {
  return apiClient.get(`/knowposts/detail/${id}`) as Promise<PostDetail>;
}

/** POST /knowposts/description/suggest */
export async function suggestDescription(body: {
  content: string;
}): Promise<{ suggestion: string }> {
  return apiClient.post(
    "/knowposts/description/suggest",
    body,
  ) as Promise<{ suggestion: string }>;
}
