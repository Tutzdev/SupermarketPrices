import {
  ArrowRight,
  BellRing,
  Check,
  CircleDollarSign,
  GitCompareArrows,
  Heart,
  ListChecks,
  MapPin,
  ShieldCheck,
  Store,
  Users,
} from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { catalogApi } from "@/services/gomo-api";
import { EmptyState, ErrorState, LoadingState } from "@/components/ui/feedback";
import { Link } from "react-router-dom";
import { BrandLogo } from "@/components/brand-logo";
import { PublicNavbar } from "@/components/layout/public-navbar";
import { NativeButton } from "@/components/ui/native-button";
import { useAuth } from "@/features/auth/auth-context";
import { BRAND, formatCurrency } from "@/lib/brand";

const steps = [
  { number: "01", title: "Escolha sua cidade", description: "Defina onde você compra para consultar as lojas disponíveis na região." },
  { number: "02", title: "Pesquise ou monte uma lista", description: "Encontre produtos individualmente ou organize tudo o que precisa comprar." },
  { number: "03", title: "Compare os supermercados", description: "Veja preços, disponibilidade, promoções e a origem de cada informação." },
  { number: "04", title: "Decida com clareza", description: "Use a recomendação da lista sem esconder itens ainda sem preço recente." },
];

const features = [
  { icon: GitCompareArrows, title: "Comparação de produtos", description: "Compare o mesmo produto nas lojas da cidade selecionada." },
  { icon: ListChecks, title: "Listas de compras", description: "Crie listas, ajuste quantidades e acompanhe todos os itens." },
  { icon: Store, title: "Recomendação de loja", description: "Encontre a opção com melhor cobertura e total conhecido para sua lista." },
  { icon: CircleDollarSign, title: "Histórico de preços", description: "Consulte observações anteriores com data, origem e disponibilidade." },
  { icon: BellRing, title: "Alertas de preço", description: "Defina um valor desejado e acompanhe as notificações geradas." },
  { icon: Heart, title: "Lojas favoritas", description: "Mantenha por perto os supermercados que fazem parte da sua rotina." },
  { icon: Users, title: "Contribuição da comunidade", description: "Envie preços encontrados para análise antes da publicação." },
  { icon: ShieldCheck, title: "Dados rastreáveis", description: "Cada preço preserva informações de coleta e da fonte utilizada." },
];

const faq = [
  ["Como os preços são obtidos?", "A Gomo reúne observações de fontes cadastradas e contribuições enviadas pela comunidade. As contribuições passam por moderação antes de gerar um registro de preço."],
  ["Como funciona a comparação?", "Você escolhe uma cidade e um produto ou lista. A Gomo consulta os registros disponíveis por supermercado e indica quando não há observação recente."],
  ["Preciso de assinatura?", "O plano comercial previsto custa R$ 10,99 por mês. A ativação depende da integração segura de cobrança no backend, ainda não disponível nesta versão."],
  ["Quais cidades e supermercados estão disponíveis?", "A disponibilidade depende do catálogo real cadastrado no sistema. Depois de entrar, você poderá consultar as cidades, redes e lojas retornadas pela API."],
  ["Com que frequência os preços são atualizados?", "Cada registro mostra quando foi coletado. A frequência varia conforme a fonte e a disponibilidade de novas observações."],
  ["Como cancelo a assinatura?", "O fluxo de cancelamento será disponibilizado junto com a integração de cobrança. A Gomo não apresenta pagamento ou cancelamento como concluído sem confirmação do backend."],
];

export function LandingPage() {
  const { user } = useAuth();
  const platformPath = user ? "/app" : "/entrar";

  return (
    <div className="min-h-screen bg-white text-foreground">
      <a href="#conteudo-principal" className="skip-link">Pular para o conteúdo</a>
      <PublicNavbar />

      <main id="conteudo-principal">
        <section className="relative overflow-hidden border-b border-border bg-[#fffafa]">
          <div className="absolute -right-24 top-16 size-96 rounded-full bg-primary/7 blur-3xl" aria-hidden />
          <div className="mx-auto grid max-w-7xl items-center gap-14 px-4 py-18 sm:px-6 sm:py-24 lg:grid-cols-[1fr_0.92fr] lg:px-8 lg:py-28">
            <div className="relative">
              <p className="mb-5 inline-flex items-center gap-2 rounded-full border border-primary/20 bg-white px-3 py-1.5 text-sm font-semibold text-primary-dark">
                <MapPin className="size-4" aria-hidden />
                Compare onde você realmente compra
              </p>
              <h1 className="max-w-3xl text-4xl font-extrabold leading-[1.08] tracking-[-0.045em] sm:text-5xl lg:text-6xl">
                Seu mercado começa antes de sair de casa.
              </h1>
              <p className="mt-6 max-w-2xl text-lg leading-8 text-muted">
                Pesquise produtos, organize sua lista e compare preços entre supermercados com informações claras sobre origem, validade e disponibilidade.
              </p>
              <div className="mt-8 flex flex-col gap-3 sm:flex-row">
                <NativeButton to="/assinar" size="lg" glow className="w-full sm:w-auto">
                  Assinar por {formatCurrency(BRAND.monthlyPrice)}/mês
                  <ArrowRight className="size-4" aria-hidden />
                </NativeButton>
                <NativeButton href="#recursos" variant="secondary" size="lg" className="w-full sm:w-auto">
                  Conhecer recursos
                </NativeButton>
              </div>
              <p className="mt-4 text-sm text-muted">Sem promessa de economia artificial: a comparação usa somente os dados disponíveis.</p>
            </div>

            <DashboardPreview />
          </div>
        </section>

        <section id="como-funciona" className="scroll-mt-24 py-18 sm:py-24">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <SectionHeading eyebrow="Como funciona" title="Da lista à decisão em quatro etapas" description="Um fluxo direto para transformar preços dispersos em uma compra mais bem informada." />
            <ol className="mt-12 grid gap-px overflow-hidden rounded-xl border border-border bg-border sm:grid-cols-2 lg:grid-cols-4">
              {steps.map((step) => (
                <li key={step.number} className="bg-white p-6 sm:p-7">
                  <span className="text-sm font-extrabold text-primary">{step.number}</span>
                  <h3 className="mt-8 text-lg font-bold tracking-tight">{step.title}</h3>
                  <p className="mt-2 text-sm leading-6 text-muted">{step.description}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section id="recursos" className="scroll-mt-24 border-y border-border bg-[#f7f7f8] py-18 sm:py-24">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <SectionHeading eyebrow="Recursos" title="Tudo o que importa para comparar sem ruído" description="A Gomo organiza os recursos já suportados pela API em uma experiência única e coerente." />
            <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {features.map((feature) => {
                const Icon = feature.icon;
                return (
                  <article key={feature.title} className="surface p-5 transition-colors hover:border-primary/30">
                    <span className="grid size-10 place-items-center rounded-lg bg-primary-soft text-primary-dark">
                      <Icon className="size-5" aria-hidden />
                    </span>
                    <h3 className="mt-5 font-bold tracking-tight">{feature.title}</h3>
                    <p className="mt-2 text-sm leading-6 text-muted">{feature.description}</p>
                  </article>
                );
              })}
            </div>
          </div>
        </section>

        <section className="py-18 sm:py-24">
          <div className="mx-auto grid max-w-7xl items-center gap-12 px-4 sm:px-6 lg:grid-cols-2 lg:px-8">
            <div>
              <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-primary">Demonstração do sistema</p>
              <h2 className="mt-3 text-3xl font-extrabold tracking-[-0.04em] sm:text-4xl">Dados úteis, sem esconder as limitações.</h2>
              <p className="mt-5 text-base leading-7 text-muted">
                Uma comparação só é confiável quando mostra também o que ainda não foi encontrado. Por isso, a Gomo separa itens com preço, itens ausentes e observações desatualizadas.
              </p>
              <ul className="mt-7 space-y-3">
                {["Busca real no catálogo", "Preço e promoção quando disponíveis", "Data e origem da observação", "Tabela alternativa para históricos"].map((item) => (
                  <li key={item} className="flex items-center gap-3 text-sm font-semibold">
                    <span className="grid size-6 place-items-center rounded-full bg-success-soft text-success"><Check className="size-4" aria-hidden /></span>
                    {item}
                  </li>
                ))}
              </ul>
            </div>
            <div className="surface overflow-hidden">
              <div className="border-b border-border bg-[#fafafa] px-5 py-4">
                <p className="font-bold">Comparação da lista</p>
                <p className="mt-1 text-xs text-muted">Prévia da interface — os valores vêm da API após a consulta.</p>
              </div>
              <div className="divide-y divide-border">
                {["Cobertura da lista", "Total conhecido", "Itens sem preço recente"].map((label) => (
                  <div key={label} className="flex items-center justify-between gap-6 px-5 py-4">
                    <span className="text-sm text-muted">{label}</span>
                    <span className="font-semibold">—</span>
                  </div>
                ))}
              </div>
              <div className="m-5 rounded-lg border border-primary/20 bg-primary-soft p-4">
                <p className="text-sm font-bold text-primary-dark">A recomendação aparece somente com dados reais.</p>
                <p className="mt-1 text-sm text-muted">Listas incompletas continuam identificadas como parciais.</p>
              </div>
            </div>
          </div>
        </section>

        <section id="preco" className="scroll-mt-24 border-y border-border bg-[#211b1b] py-18 text-white sm:py-24">
          <div className="mx-auto grid max-w-5xl items-center gap-10 px-4 sm:px-6 lg:grid-cols-[1fr_24rem] lg:px-8">
            <div>
              <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-[#ff746d]">Preço simples</p>
              <h2 className="mt-3 text-3xl font-extrabold tracking-[-0.04em] sm:text-4xl">Uma assinatura. Todos os recursos.</h2>
              <p className="mt-5 max-w-xl leading-7 text-white/66">A estrutura comercial está pronta. A ativação do acesso será conectada quando o backend oferecer confirmação segura de pagamento.</p>
            </div>
            <div className="rounded-xl border border-white/14 bg-white p-6 text-foreground sm:p-7">
              <p className="font-bold">Plano Gomo</p>
              <p className="mt-4 text-4xl font-extrabold tracking-tight">{formatCurrency(BRAND.monthlyPrice)}<span className="text-base font-semibold text-muted">/mês</span></p>
              <ul className="my-6 space-y-3 text-sm">
                {["Comparações de produtos e listas", "Histórico e alertas de preço", "Lojas favoritas e notificações", "Contribuições para a comunidade"].map((item) => (
                  <li key={item} className="flex gap-2"><Check className="mt-0.5 size-4 shrink-0 text-success" aria-hidden />{item}</li>
                ))}
              </ul>
              <NativeButton to="/assinar" className="w-full" glow>Assinar por {formatCurrency(BRAND.monthlyPrice)}/mês</NativeButton>
            </div>
          </div>
        </section>

        <section id="duvidas" className="scroll-mt-24 py-18 sm:py-24">
          <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
            <SectionHeading eyebrow="Dúvidas" title="Respostas diretas antes de começar" description="Sem promessas que o produto ainda não consegue cumprir." centered />
            <div className="mt-10 divide-y divide-border border-y border-border">
              {faq.map(([question, answer]) => (
                <details key={question} className="group py-1">
                  <summary className="flex min-h-14 cursor-pointer list-none items-center justify-between gap-5 py-3 font-bold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-focus">
                    {question}
                    <span className="text-xl text-primary transition-transform group-open:rotate-45 motion-reduce:transition-none" aria-hidden>+</span>
                  </summary>
                  <p className="max-w-2xl pb-5 pr-10 text-sm leading-6 text-muted">{answer}</p>
                </details>
              ))}
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-border bg-[#f7f7f8]">
        <div className="mx-auto flex max-w-7xl flex-col gap-8 px-4 py-10 sm:px-6 md:flex-row md:items-end md:justify-between lg:px-8">
          <div>
            <BrandLogo />
            <p className="mt-4 max-w-sm text-sm leading-6 text-muted">Comparação de preços com contexto para decisões de compra mais claras.</p>
          </div>
          <nav aria-label="Rodapé" className="flex flex-wrap gap-x-6 gap-y-3 text-sm font-semibold text-muted">
            <a href="/#como-funciona" className="hover:text-foreground">Como funciona</a>
            <a href="/#recursos" className="hover:text-foreground">Recursos</a>
            <a href="/#preco" className="hover:text-foreground">Preço</a>
            <a href="/#duvidas" className="hover:text-foreground">Dúvidas</a>
            <Link to={platformPath} className="hover:text-foreground">Acessar plataforma</Link>
          </nav>
        </div>
      </footer>
    </div>
  );
}

function SectionHeading({ eyebrow, title, description, centered = false }: { eyebrow: string; title: string; description: string; centered?: boolean }) {
  return (
    <div className={centered ? "text-center" : ""}>
      <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-primary">{eyebrow}</p>
      <h2 className="mt-3 text-3xl font-extrabold tracking-[-0.04em] sm:text-4xl">{title}</h2>
      <p className={`mt-4 max-w-2xl leading-7 text-muted ${centered ? "mx-auto" : ""}`}>{description}</p>
    </div>
  );
}
function DashboardPreview() {
  const products = useQuery({ queryKey: ["products", "preview"], queryFn: () => catalogApi.products({ size: 5 }) });

  return (
    <section className="surface mx-auto w-full max-w-xl overflow-hidden" aria-labelledby="gomo-preview-title">
      <div className="border-b border-border bg-surface-strong p-5">
        <BrandLogo compact />
        <h2 id="gomo-preview-title" className="mt-4 text-lg font-extrabold">No catálogo agora</h2>
        <p className="mt-1 text-sm text-muted">Produtos recebidos das fontes reais. Consulte os preços por cidade.</p>
      </div>
      {products.isLoading ? <LoadingState /> : products.isError ? <ErrorState message={products.error.message} retry={() => void products.refetch()} /> : !products.data?.content.length ? <EmptyState title="Catálogo aguardando coleta" description="Os produtos aparecem após a primeira coleta das fontes." /> : (
        <ul className="divide-y divide-border">
          {products.data.content.map((product) => <li key={product.id} className="px-5 py-4"><p className="font-semibold">{product.name}</p><p className="mt-1 text-xs text-muted">{product.brand ?? "Marca não informada"}</p></li>)}
        </ul>
      )}
      <div className="border-t border-border p-5"><Link to="/app/comparar" className="font-semibold text-primary hover:underline">Consultar preços por supermercado</Link></div>
    </section>
  );
}
