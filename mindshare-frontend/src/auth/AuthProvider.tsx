import { useEffect, createContext, useContext, type ReactNode } from "react";
import { useAuthStore } from "../store/authStore";
import { initClientTokens, clearPersistedTokens } from "./tokenStore";
import { setTokens, clearTokens, setOnAuthFailure } from "../api/client";
import * as authApi from "../api/auth";

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType>({
  isAuthenticated: false,
  isLoading: true,
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const { setAuth, clearAuth, setLoading, isAuthenticated, isLoading } =
    useAuthStore();

  useEffect(() => {
    const init = async () => {
      const hasTokens = initClientTokens();
      if (!hasTokens) {
        setLoading(false);
        return;
      }

      try {
        const user = await authApi.getMe();
        // Tokens loaded from storage by initClientTokens → need to re-read
        const storedAccess = localStorage.getItem("mindshare_access_token");
        const storedRefresh = localStorage.getItem("mindshare_refresh_token");
        if (storedAccess && storedRefresh) {
          setAuth(user, storedAccess, storedRefresh);
          setTokens(storedAccess, storedRefresh);
        } else {
          setLoading(false);
        }
      } catch {
        clearAuth();
        clearTokens();
        clearPersistedTokens();
      }
    };

    init();
  }, [setAuth, clearAuth, setLoading]);

  // Register auth failure callback on mount
  useEffect(() => {
    setOnAuthFailure(() => {
      clearAuth();
      clearPersistedTokens();
      window.location.href = "/login";
    });
  }, [clearAuth]);

  return (
    <AuthContext.Provider value={{ isAuthenticated, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}

export { useAuthStore };
