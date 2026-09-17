import { LoadingState } from "@/components/ui/feedback";

export function RouteLoadingPage() {
  return (
    <main className="grid min-h-screen place-items-center p-4">
      <LoadingState label="Carregando…" />
    </main>
  );
}
