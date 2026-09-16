import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { authApi } from "@/services/gomo-api";
import { sessionStorageService } from "@/lib/api";
import type { User } from "@/types/api";

interface AuthContextValue {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<User>;
  register: (name: string, email: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(() => Boolean(sessionStorageService.read()));

  const refreshUser = async () => {
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
  };

  useEffect(() => {
    void refreshUser();

    const handleExpiredSession = () => setUser(null);
    window.addEventListener("gomo:session-expired", handleExpiredSession);
    return () => window.removeEventListener("gomo:session-expired", handleExpiredSession);
  }, []);

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
    [loading, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
