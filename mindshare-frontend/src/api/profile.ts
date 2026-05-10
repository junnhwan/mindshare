import apiClient from "./client";
import type { ProfileData, UpdateProfileRequest } from "../types/profile";

/** GET /profile */
export async function getProfile(): Promise<ProfileData> {
  return apiClient.get("/profile") as Promise<ProfileData>;
}

/** PATCH /profile */
export async function updateProfile(
  req: UpdateProfileRequest,
): Promise<ProfileData> {
  return apiClient.patch("/profile", req) as Promise<ProfileData>;
}

/** POST /profile/avatar (multipart/form-data, field "file") */
export async function uploadAvatar(file: File): Promise<ProfileData> {
  const formData = new FormData();
  formData.append("file", file);
  return apiClient.post("/profile/avatar", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  }) as Promise<ProfileData>;
}
