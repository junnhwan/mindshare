import { loadTokensFromStorage, clearStorageTokens } from "../api/client";

export function getStoredTokens(): { accessToken: string; refreshToken: string } | null {
  try {
    const access = localStorage.getItem("mindshare_access_token");
    const refresh = localStorage.getItem("mindshare_refresh_token");
    if (access && refresh) {
      return { accessToken: access, refreshToken: refresh };
    }
  } catch {
    // localStorage unavailable
  }
  return null;
}

export function persistTokens(accessToken: string, refreshToken: string) {
  try {
    localStorage.setItem("mindshare_access_token", accessToken);
    localStorage.setItem("mindshare_refresh_token", refreshToken);
  } catch {
    // localStorage unavailable
  }
}

export function clearPersistedTokens() {
  clearStorageTokens();
}

export function initClientTokens(): boolean {
  return loadTokensFromStorage();
}
