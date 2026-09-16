import { Menu } from "lucide-react";
import { useEffect, useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { DashboardSidebar } from "@/components/layout/dashboard-sidebar";
import { Sheet } from "@/components/ui/sheet";
import { useAuth } from "@/features/auth/auth-context";
import { cn } from "@/lib/cn";

const pageNames: Record<string, string> = {
  "/app": "Visão geral",
  "/app/comparar": "Comparar preços",
  "/app/produtos": "Produtos",
  "/app/supermercados": "Supermercados",
  "/app/listas": "Minhas listas",
  "/app/alertas": "Alertas de preço",
  "/app/contribuir": "Contribuir com preço",
  "/app/notificacoes": "Notificações",
  "/app/perfil": "Perfil e preferências",
  "/app/admin": "Administração",
};

const COLLAPSED_KEY = "gomo.sidebar-collapsed";

export function AppShell() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(() => window.localStorage.getItem(COLLAPSED_KEY) === "true");

  useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname]);

  if (!user) return null;

  const toggleCollapsed = () => {
    setCollapsed((current) => {
      const next = !current;
      window.localStorage.setItem(COLLAPSED_KEY, String(next));
      return next;
    });
  };

  const handleLogout = async () => {
    await logout();
    navigate("/entrar", { replace: true });
  };

  const currentPage =
    pageNames[location.pathname] ??
    (location.pathname.startsWith("/app/listas/") ? "Detalhes da lista" : "Gomo");

  return (
    <div className="min-h-screen bg-[#f7f7f8]">
      <a href="#conteudo-principal" className="skip-link">Pular para o conteúdo</a>

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-40 hidden bg-[#981d18] transition-[width] duration-200 lg:block motion-reduce:transition-none",
          collapsed ? "w-20" : "w-64",
        )}
      >
        <DashboardSidebar
          collapsed={collapsed}
          role={user.role}
          onToggle={toggleCollapsed}
          onLogout={() => void handleLogout()}
        />
      </aside>

      <div className={cn("min-h-screen transition-[padding] duration-200 motion-reduce:transition-none", collapsed ? "lg:pl-20" : "lg:pl-64")}>
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between gap-4 border-b border-border bg-white/95 px-4 backdrop-blur sm:px-6 lg:px-8">
          <div className="flex min-w-0 items-center gap-3">
            <button type="button" className="icon-button lg:hidden" onClick={() => setMobileOpen(true)} aria-label="Abrir menu lateral">
              <Menu className="size-5" aria-hidden />
            </button>
            <p className="truncate font-semibold text-foreground">{currentPage}</p>
          </div>
          <div className="min-w-0 text-right">
            <p className="truncate text-sm font-semibold text-foreground">{user.name}</p>
            <p className="text-xs text-muted">
              {user.role === "ADMIN" ? "Administrador" : user.subscriber ? "Assinante Gomo" : "Conta Gomo"}
            </p>
          </div>
        </header>

        <main id="conteudo-principal" className="mx-auto w-full max-w-[96rem] p-4 sm:p-6 lg:p-8" tabIndex={-1}>
          <Outlet />
        </main>
      </div>

      <Sheet open={mobileOpen} onOpenChange={setMobileOpen} title="Menu da Gomo" side="left">
        <div className="-m-5 mt-0 h-[calc(100%+1.25rem)] bg-[#981d18]">
          <DashboardSidebar
            collapsed={false}
            role={user.role}
            mobile
            onNavigate={() => setMobileOpen(false)}
            onLogout={() => void handleLogout()}
          />
        </div>
      </Sheet>
    </div>
  );
}
