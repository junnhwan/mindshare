import apiClient from "./client";
import type { PageResponse } from "../types/api";
import type {
  RelationCountsResponse,
  RelationStatusResponse,
  RelationUser,
} from "../types/relation";

/** POST /relation/follow?toUserId= */
export async function follow(toUserId: number): Promise<void> {
  return apiClient.post("/relation/follow", null, {
    params: { toUserId },
  });
}

/** POST /relation/unfollow?toUserId= */
export async function unfollow(toUserId: number): Promise<void> {
  return apiClient.post("/relation/unfollow", null, {
    params: { toUserId },
  });
}

/** GET /relation/status?userId= */
export async function getRelationStatus(
  userId: number,
): Promise<RelationStatusResponse> {
  return apiClient.get("/relation/status", {
    params: { userId },
  }) as Promise<RelationStatusResponse>;
}

/** GET /relation/following?userId=&page=&size= */
export async function getFollowing(params: {
  userId?: number;
  page: number;
  size: number;
}): Promise<PageResponse<RelationUser>> {
  return apiClient.get("/relation/following", {
    params,
  }) as Promise<PageResponse<RelationUser>>;
}

/** GET /relation/followers?userId=&page=&size= */
export async function getFollowers(params: {
  userId?: number;
  page: number;
  size: number;
}): Promise<PageResponse<RelationUser>> {
  return apiClient.get("/relation/followers", {
    params,
  }) as Promise<PageResponse<RelationUser>>;
}

/** GET /relation/counter?userId= */
export async function getRelationCounter(
  userId: number,
): Promise<RelationCountsResponse> {
  return apiClient.get("/relation/counter", {
    params: { userId },
  }) as Promise<RelationCountsResponse>;
}
