import { ChevronLeft, ChevronRight } from "lucide-react";
import type { ReactNode } from "react";
import { NativeButton } from "@/components/ui/native-button";
import { cn } from "@/lib/cn";

export function PageHeading({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return (
    <header className="page-header">
      <div>
        <h1 className="page-title">{title}</h1>
        <p className="page-description">{description}</p>
      </div>
      {action}
    </header>
  );
}

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <nav aria-label="Paginação" className="mt-5 flex items-center justify-between gap-4">
      <NativeButton variant="secondary" size="sm" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        <ChevronLeft className="size-4" aria-hidden />Anterior
      </NativeButton>
      <p className="text-sm text-muted">Página <strong className="text-foreground">{page + 1}</strong> de {totalPages}</p>
      <NativeButton variant="secondary" size="sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Próxima<ChevronRight className="size-4" aria-hidden />
      </NativeButton>
    </nav>
  );
}

export function StatusBadge({ children, tone = "neutral" }: { children: ReactNode; tone?: "success" | "warning" | "danger" | "neutral" }) {
  return (
    <span className={cn(
      "status-badge",
      tone === "success" && "bg-success-soft text-success",
      tone === "warning" && "bg-warning-soft text-warning",
      tone === "danger" && "bg-danger-soft text-danger",
      tone === "neutral" && "bg-surface-strong text-muted",
    )}>{children}</span>
  );
}

export function SurfaceTitle({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-border px-4 py-4 sm:px-5">
      <div><h2 className="font-bold text-foreground">{title}</h2>{description ? <p className="mt-1 text-sm text-muted">{description}</p> : null}</div>
      {action}
    </div>
  );
}
