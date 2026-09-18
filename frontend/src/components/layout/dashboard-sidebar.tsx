import {
  Bell,
  ChevronLeft,
  ChevronRight,
  CircleDollarSign,
  LayoutDashboard,
  ListChecks,
  LogOut,
  PackageSearch,
  Settings,
  ShieldCheck,
  Store,
  Tags,
  type LucideIcon,
} from "lucide-react";
import { NavLink } from "react-router-dom";
import { BrandLogo } from "@/components/brand-logo";
import { cn } from "@/lib/cn";
import type { UserRole } from "@/types/api";

type NavigationItem = {
  label: string;
  to: string;
  icon: LucideIcon;
  admin?: boolean;
};

const navigation: NavigationItem[] = [
  { label: "Visão geral", to: "/app", icon: LayoutDashboard },
  { label: "Comparar preços", to: "/app/comparar", icon: Tags },
  { label: "Produtos", to: "/app/produtos", icon: PackageSearch },
  { label: "Supermercados", to: "/app/supermercados", icon: Store },
  { label: "Minhas listas", to: "/app/listas", icon: ListChecks },
  { label: "Alertas de preço", to: "/app/alertas", icon: CircleDollarSign },
  { label: "Contribuir com preço", to: "/app/contribuir", icon: Tags },
  { label: "Notificações", to: "/app/notificacoes", icon: Bell },
  { label: "Perfil e preferências", to: "/app/perfil", icon: Settings },
  { label: "Administração", to: "/app/admin", icon: ShieldCheck, admin: true },
];

interface DashboardSidebarProps {
  collapsed: boolean;
  role: UserRole;
  onToggle?: () => void;
  onNavigate?: () => void;
  onLogout: () => void;
  mobile?: boolean;
}

export function DashboardSidebar({
  collapsed,
  role,
  onToggle,
  onNavigate,
  onLogout,
  mobile = false,
}: DashboardSidebarProps) {
  const visibleNavigation = navigation.filter((item) => !item.admin || role === "ADMIN");

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className={cn("flex h-18 items-center border-b border-white/10 px-4", collapsed ? "justify-center" : "justify-between")}>
        <NavLink
          to="/app"
          aria-label="Gomo — visão geral"
          onClick={onNavigate}
          className="rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"
        >
          <BrandLogo compact={collapsed} className={collapsed ? "" : "h-8 brightness-0 invert"} />
        </NavLink>
        {!collapsed && !mobile && onToggle ? (
          <button type="button" className="sidebar-icon-button" onClick={onToggle} aria-label="Recolher menu lateral">
            <ChevronLeft className="size-4" aria-hidden />
          </button>
        ) : null}
      </div>

      {collapsed && !mobile && onToggle ? (
        <button type="button" className="sidebar-icon-button mx-auto mt-3" onClick={onToggle} aria-label="Expandir menu lateral">
          <ChevronRight className="size-4" aria-hidden />
        </button>
      ) : null}

      <nav aria-label="Área autenticada" className="min-h-0 flex-1 overflow-y-auto px-3 py-4">
        <ul className="space-y-1">
          {visibleNavigation.map((item) => {
            const Icon = item.icon;
            return (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.to === "/app"}
                  onClick={onNavigate}
                  title={collapsed ? item.label : undefined}
                  className={({ isActive }) =>
                    cn(
                      "dashboard-nav-action flex min-h-11 items-center gap-3 rounded-lg px-3 text-sm font-semibold",
                      "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-[#8e1612]",
                      isActive ? "bg-white text-[#991b16]" : "text-white/78 hover:bg-white/10 hover:text-white",
                      collapsed && "justify-center px-0",
                    )
                  }
                >
                  <Icon className="size-5 shrink-0" aria-hidden />
                  {!collapsed ? <span>{item.label}</span> : <span className="sr-only">{item.label}</span>}
                </NavLink>
              </li>
            );
          })}
        </ul>
      </nav>

      <div className="border-t border-white/10 p-3">
        <button
          type="button"
          onClick={onLogout}
          title={collapsed ? "Sair" : undefined}
          className={cn(
            "dashboard-nav-action flex min-h-11 w-full items-center gap-3 rounded-lg px-3 text-sm font-semibold text-white/78 hover:bg-white/10 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white",
            collapsed && "justify-center px-0",
          )}
        >
          <LogOut className="size-5 shrink-0" aria-hidden />
          {!collapsed ? <span>Sair</span> : <span className="sr-only">Sair</span>}
        </button>
      </div>
    </div>
  );
}
