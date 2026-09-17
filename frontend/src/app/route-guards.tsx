import { Navigate, Outlet } from "react-router-dom";
import { AppShell } from "@/components/layout/app-shell";
import { LoadingState } from "@/components/ui/feedback";
import { useAuth } from "@/features/auth/auth-context";

export function ProtectedLayout() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <main className="grid min-h-screen place-items-center p-4">
        <LoadingState label="Carregando sua conta…" />
      </main>
    );
  }
  if (!user) return <Navigate to="/entrar" replace />;
  return <AppShell />;
}

export function AdminGuard() {
  const { user } = useAuth();
  return user?.role === "ADMIN" ? <Outlet /> : <Navigate to="/acesso-negado" replace />;
}
