import { ArrowLeft, Check, LockKeyhole } from "lucide-react";
import { Link } from "react-router-dom";
import { BrandLogo } from "@/components/brand-logo";
import { NativeButton } from "@/components/ui/native-button";
import { BRAND, formatCurrency } from "@/lib/brand";

export function SubscriptionPage() {
  return (
    <main className="min-h-screen bg-[#f7f7f8] px-4 py-8 sm:py-14">
      <div className="mx-auto max-w-4xl">
        <div className="flex items-center justify-between gap-4">
          <Link to="/" aria-label="Voltar à página inicial" className="rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-focus"><BrandLogo /></Link>
          <Link to="/" className="inline-flex items-center gap-2 text-sm font-semibold text-muted hover:text-foreground"><ArrowLeft className="size-4" aria-hidden />Voltar</Link>
        </div>

        <div className="mt-12 grid overflow-hidden rounded-xl border border-border bg-white lg:grid-cols-[1fr_0.82fr]">
          <section className="p-6 sm:p-10">
            <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-primary">Plano Gomo</p>
            <h1 className="mt-3 text-3xl font-extrabold tracking-[-0.04em]">Compare melhor. Compre com clareza.</h1>
            <p className="mt-4 leading-7 text-muted">Uma assinatura para usar os recursos de comparação, organização e acompanhamento da Gomo.</p>
            <ul className="mt-8 space-y-4">
              {["Comparações de produtos e listas", "Histórico e alertas de preço", "Favoritos, notificações e contribuições", "Acesso às evoluções do produto"].map((item) => (
                <li key={item} className="flex gap-3 text-sm font-semibold"><span className="grid size-6 shrink-0 place-items-center rounded-full bg-success-soft text-success"><Check className="size-4" aria-hidden /></span>{item}</li>
              ))}
            </ul>
          </section>
          <aside className="border-t border-border bg-[#211b1b] p-6 text-white sm:p-10 lg:border-l lg:border-t-0">
            <p className="text-sm font-semibold text-white/65">Assinatura mensal</p>
            <p className="mt-3 text-4xl font-extrabold">{formatCurrency(BRAND.monthlyPrice)}<span className="text-base text-white/65">/mês</span></p>
            <div className="mt-8 rounded-lg border border-white/15 bg-white/6 p-4">
              <div className="flex gap-3"><LockKeyhole className="mt-0.5 size-5 shrink-0 text-[#ff746d]" aria-hidden /><div><p className="font-bold">Checkout ainda não conectado</p><p className="mt-1 text-sm leading-6 text-white/65">O backend atual não possui cobrança nem status de assinatura. Nenhum pagamento será simulado.</p></div></div>
            </div>
            <NativeButton to="/entrar?modo=cadastro" className="mt-6 w-full" glow>Criar conta Gomo</NativeButton>
            <p className="mt-3 text-center text-xs text-white/55">O acesso pago será liberado somente após confirmação segura do backend.</p>
          </aside>
        </div>
      </div>
    </main>
  );
}
