import axios, { AxiosError } from "axios";
import type { InternalAxiosRequestConfig } from "axios";
import type { ApiResponse } from "../types/api";

const apiClient = axios.create({
  baseURL: "/api/v1",
  timeout: 15000,
  headers: { "Content-Type": "application/json" },
});

let accessToken: string | null = null;
let refreshToken: string | null = null;
let refreshPromise: Promise<string> | null = null;
let onAuthFailure: (() => void) | null = null;

export function setTokens(access: string, refresh: string) {
  accessToken = access;
  refreshToken = refresh;
}

export function clearTokens() {
  accessToken = null;
  refreshToken = null;
}

export function getAccessToken() {
  return accessToken;
}

export function setOnAuthFailure(cb: () => void) {
  onAuthFailure = cb;
}

function decodeJwt(token: string): { exp?: number } | null {
  try {
    const payload = token.split(".")[1];
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

function isTokenExpiring(token: string): boolean {
  const decoded = decodeJwt(token);
  if (!decoded?.exp) return false;
  const now = Math.floor(Date.now() / 1000);
  return decoded.exp - now < 60;
}

async function doRefreshToken(): Promise<string> {
  if (!refreshToken) throw new Error("No refresh token");

  const res = await axios.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
    "/api/v1/auth/token/refresh",
    { refreshToken }
  );

  if (res.data.code !== "OK") {
    throw new Error("Refresh failed");
  }

  const { accessToken: newAccess, refreshToken: newRefresh } = res.data.data;
  setTokens(newAccess, newRefresh);

  // Persist to localStorage
  try {
    localStorage.setItem("mindshare_access_token", newAccess);
    localStorage.setItem("mindshare_refresh_token", newRefresh);
  } catch {
    // localStorage might be full or unavailable
  }

  return newAccess;
}

export async function refreshTokens(): Promise<string> {
  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = doRefreshToken()
    .then((token) => {
      refreshPromise = null;
      return token;
    })
    .catch((err) => {
      refreshPromise = null;
      throw err;
    });

  return refreshPromise;
}

export function loadTokensFromStorage(): boolean {
  try {
    const access = localStorage.getItem("mindshare_access_token");
    const refresh = localStorage.getItem("mindshare_refresh_token");
    if (access && refresh) {
      accessToken = access;
      refreshToken = refresh;
      return true;
    }
  } catch {
    // localStorage unavailable
  }
  return false;
}

export function clearStorageTokens() {
  try {
    localStorage.removeItem("mindshare_access_token");
    localStorage.removeItem("mindshare_refresh_token");
  } catch {
    // localStorage unavailable
  }
}

// Request interceptor: attach token + proactive refresh
apiClient.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    if (accessToken) {
      // Proactive refresh if token is about to expire
      if (isTokenExpiring(accessToken)) {
        try {
          const newToken = await refreshTokens();
          config.headers.Authorization = `Bearer ${newToken}`;
        } catch {
          // If proactive refresh fails, still try the request with the old token
          config.headers.Authorization = `Bearer ${accessToken}`;
        }
      } else {
        config.headers.Authorization = `Bearer ${accessToken}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: unwrap ApiResponse, handle 401
const unwrapResponse = ((response: any): any => {
  const body = response.data as ApiResponse<unknown>;
  if (!body || response.status === 204) return body;
  if (body.code && body.code !== "OK") {
    const err: Error & { code: string } = Object.assign(
      new Error(body.message || "请求失败"),
      { code: body.code }
    );
    return Promise.reject(err);
  }
  return body.data !== undefined ? body.data : body;
}) as any;

apiClient.interceptors.response.use(
  unwrapResponse,
  async (error) => {
    const axiosError = error as AxiosError<ApiResponse<unknown>>;
    if (!axiosError.response) {
      throw new Error("网络连接失败，请检查网络后重试");
    }
    const { status, config } = axiosError.response;
    if (status === 401 && config && !config.url?.includes("/auth/token/refresh")) {
      try {
        const newToken = await refreshTokens();
        config.headers.set("Authorization", `Bearer ${newToken}`);
        return apiClient.request(config);
      } catch {
        clearTokens();
        clearStorageTokens();
        onAuthFailure?.();
        throw new Error("登录已过期，请重新登录");
      }
    }
    const body = axiosError.response.data;
    const err: Error & { code?: string } = Object.assign(
      new Error(body?.message || "请求失败，请稍后再试"),
      { code: body?.code }
    );
    throw err;
  }
);

export default apiClient;
