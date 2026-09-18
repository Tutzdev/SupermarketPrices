import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { AuthContext, type AuthContextValue } from "@/features/auth/auth-context";
import { ApiError, sessionStorageService } from "@/lib/api";
import { authApi } from "@/services/gomo-api";
import type { User } from "@/types/api";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(() => Boolean(sessionStorageService.read()));
  const [sessionError, setSessionError] = useState<string | null>(null);

  const refreshUser = useCallback(async () => {
    setSessionError(null);
    if (!sessionStorageService.read()) {
      setUser(null);
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      setUser(await authApi.currentUser());
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        sessionStorageService.clear();
        setUser(null);
      } else {
        setSessionError("Não foi possível verificar sua sessão. Tente novamente em instantes.");
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshUser();

    const handleExpiredSession = () => {
      setUser(null);
      setSessionError(null);
    };
    window.addEventListener("gomo:session-expired", handleExpiredSession);
    return () => window.removeEventListener("gomo:session-expired", handleExpiredSession);
  }, [refreshUser]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      sessionError,
      login: async (email, password) => {
        const response = await authApi.login({ email, password });
        sessionStorageService.write(response.accessToken);
        setUser(response.user);
        setSessionError(null);
        return response.user;
      },
      register: (name, email, password) => authApi.register({ name, email, password }),
      logout: async () => {
        try {
          await authApi.logout();
        } finally {
          sessionStorageService.clear();
          setUser(null);
          setSessionError(null);
        }
      },
      refreshUser,
    }),
    [loading, refreshUser, sessionError, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
