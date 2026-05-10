import apiClient from "./client";
import type {
  AuthUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ResetPasswordRequest,
  RefreshTokenRequest,
  SendCodeRequest,
} from "../types/auth";

/** POST /auth/send-code */
export async function sendCode(req: SendCodeRequest): Promise<void> {
  return apiClient.post("/auth/send-code", req);
}

/** POST /auth/register */
export async function register(req: RegisterRequest): Promise<void> {
  return apiClient.post("/auth/register", req);
}

/** POST /auth/login */
export async function login(req: LoginRequest): Promise<LoginResponse> {
  return apiClient.post("/auth/login", req) as Promise<LoginResponse>;
}

/** POST /auth/token/refresh */
export async function refreshToken(
  req: RefreshTokenRequest,
): Promise<{ accessToken: string; refreshToken: string }> {
  return apiClient.post("/auth/token/refresh", req) as Promise<{
    accessToken: string;
    refreshToken: string;
  }>;
}

/** POST /auth/logout */
export async function logout(): Promise<void> {
  return apiClient.post("/auth/logout");
}

/** POST /auth/password/reset */
export async function resetPassword(req: ResetPasswordRequest): Promise<void> {
  return apiClient.post("/auth/password/reset", req);
}

/** GET /auth/me */
export async function getMe(): Promise<AuthUser> {
  return apiClient.get("/auth/me") as Promise<AuthUser>;
}
