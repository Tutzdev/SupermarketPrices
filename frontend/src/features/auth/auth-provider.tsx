import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { AuthContext, type AuthContextValue } from "@/features/auth/auth-context";
import { sessionStorageService } from "@/lib/api";
import { authApi } from "@/services/gomo-api";
import type { User } from "@/types/api";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(() => Boolean(sessionStorageService.read()));

  const refreshUser = useCallback(async () => {
    if (!sessionStorageService.read()) {
      setUser(null);
      setLoading(false);
      return;
    }

    try {
      setUser(await authApi.currentUser());
    } catch {
      sessionStorageService.clear();
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshUser();

    const handleExpiredSession = () => setUser(null);
    window.addEventListener("gomo:session-expired", handleExpiredSession);
    return () => window.removeEventListener("gomo:session-expired", handleExpiredSession);
  }, [refreshUser]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      login: async (email, password) => {
        const response = await authApi.login({ email, password });
        sessionStorageService.write(response.accessToken);
        setUser(response.user);
        return response.user;
      },
      register: (name, email, password) => authApi.register({ name, email, password }),
      logout: async () => {
        try {
          await authApi.logout();
        } finally {
          sessionStorageService.clear();
          setUser(null);
        }
      },
      refreshUser,
    }),
    [loading, refreshUser, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
