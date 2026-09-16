import {
  ArrowRight,
  BellRing,
  Check,
  CircleDollarSign,
  GitCompareArrows,
  Heart,
  ListChecks,
  MapPin,
  Search,
  ShieldCheck,
  Store,
  Users,
} from "lucide-react";
import { useEffect, useState } from "react";
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

const previewStores = [
  { name: "Mercado Central", distance: "1,2 km" },
  { name: "Supermercado Sul", distance: "2,8 km" },
  { name: "Rede Popular", distance: "3,4 km" },
];

const previewProducts = [
  { name: "Arroz tipo 1 · 5 kg", prices: [24.90, 27.49, 29.99] },
  { name: "Feijão carioca · 1 kg", prices: [8.29, 7.49, 8.99] },
  { name: "Leite integral · 1 L", prices: [5.19, 4.79, 4.39] },
  { name: "Café torrado · 500 g", prices: [21.49, 22.99, 18.90] },
  { name: "Óleo de soja · 900 ml", prices: [6.29, 7.49, 6.89] },
  { name: "Açúcar refinado · 1 kg", prices: [5.39, 4.59, 4.89] },
  { name: "Macarrão espaguete · 500 g", prices: [4.29, 4.69, 3.79] },
  { name: "Detergente neutro · 500 ml", prices: [2.59, 1.99, 2.29] },
  { name: "Papel higiênico · 12 rolos", prices: [19.99, 21.49, 17.90] },
  { name: "Sabão em pó · 1,6 kg", prices: [22.39, 18.49, 20.90] },
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
  const [productIndex, setProductIndex] = useState(0);
  const [isChanging, setIsChanging] = useState(false);
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false);
  const product = previewProducts[productIndex];
  const stores = previewStores
    .map((store, index) => ({ ...store, price: product.prices[index] }))
    .sort((first, second) => first.price - second.price);
  const priceDifference = stores[stores.length - 1].price - stores[0].price;

  useEffect(() => {
    const motionPreference = window.matchMedia("(prefers-reduced-motion: reduce)");
    const updateMotionPreference = () => {
      setPrefersReducedMotion(motionPreference.matches);
      if (motionPreference.matches) setIsChanging(false);
    };

    updateMotionPreference();
    motionPreference.addEventListener("change", updateMotionPreference);
    return () => motionPreference.removeEventListener("change", updateMotionPreference);
  }, []);

  useEffect(() => {
    if (prefersReducedMotion) return;

    const fadeTimer = window.setTimeout(() => setIsChanging(true), 5600);
    const changeTimer = window.setTimeout(() => {
      setProductIndex((currentIndex) => (currentIndex + 1) % previewProducts.length);
      setIsChanging(false);
    }, 5900);

    return () => {
      window.clearTimeout(fadeTimer);
      window.clearTimeout(changeTimer);
    };
  }, [prefersReducedMotion, productIndex]);

  return (
    <section className="relative mx-auto w-full max-w-xl" aria-labelledby="gomo-preview-title">
      <div className="absolute -inset-4 -z-10 rotate-2 rounded-[1.4rem] bg-primary/10" aria-hidden />
      <div className="gomo-live-preview overflow-hidden rounded-xl border border-border bg-white shadow-[0_24px_70px_rgba(66,24,21,0.14)]">
        <div className="flex items-center gap-3 border-b border-border bg-[#fafafa] px-4 py-3">
          <BrandLogo compact />
          <div className="ml-auto flex items-center gap-1.5" aria-hidden="true">
            <span className="size-2.5 rounded-full bg-[#ff5f57]" />
            <span className="size-2.5 rounded-full bg-[#febc2e]" />
            <span className="size-2.5 rounded-full bg-[#28c840]" />
          </div>
        </div>

        <div className="p-5 sm:p-6">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-[0.68rem] font-extrabold uppercase tracking-[0.16em] text-primary">Prévia interativa</p>
              <h2 id="gomo-preview-title" className="mt-1.5 text-lg font-extrabold tracking-tight">Encontre onde vale mais a pena</h2>
            </div>
            <span className="inline-flex items-center gap-1.5 rounded-md bg-surface-strong px-2.5 py-1.5 text-xs font-semibold text-muted">
              <MapPin className="size-3.5 text-primary" aria-hidden />
              Volta Redonda
            </span>
          </div>

          <div className="gomo-preview-cycle" data-changing={isChanging}>
            <div className="gomo-preview-cycle-content" key={product.name}>
              <div className="mt-5 flex min-h-12 items-center gap-3 rounded-lg border border-border-strong bg-white px-3.5 shadow-[0_1px_0_rgba(32,33,36,0.02)]">
                <Search className="size-4 shrink-0 text-muted" aria-hidden />
                <span className="gomo-preview-query min-w-0 text-sm font-semibold">{product.name}</span>
                <span className="gomo-preview-cursor h-5 w-px bg-primary" aria-hidden />
                <span className="ml-auto hidden shrink-0 rounded-md bg-primary-soft px-2 py-1 text-[0.65rem] font-extrabold uppercase tracking-wide text-primary-dark sm:inline">Buscando</span>
              </div>

              <div className="gomo-preview-results relative mt-4 overflow-hidden rounded-xl border border-border bg-[#fcfcfc] p-2">
                <div className="gomo-scan-line" aria-hidden />
                <ol className="relative space-y-1.5">
                  {stores.map((store, index) => (
                    <li
                      key={store.name}
                      className={`gomo-preview-result grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 rounded-lg border px-3 py-3 ${index === 0 ? "border-primary/25 bg-primary-soft/70" : "border-transparent bg-white"}`}
                    >
                      <span className={`grid size-9 place-items-center rounded-lg ${index === 0 ? "bg-primary text-white" : "bg-surface-strong text-muted"}`}>
                        <Store className="size-4" aria-hidden />
                      </span>
                      <span className="min-w-0">
                        <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
                          <span className="truncate text-sm font-bold">{store.name}</span>
                          {index === 0 ? <span className="rounded-full bg-white px-2 py-0.5 text-[0.62rem] font-extrabold uppercase tracking-wide text-primary-dark">Melhor preço</span> : null}
                        </span>
                        <span className="mt-0.5 block text-xs text-muted">Disponível · {store.distance}</span>
                      </span>
                      <span className={`text-sm font-extrabold tabular-nums sm:text-base ${index === 0 ? "text-primary-dark" : "text-foreground"}`}>{formatCurrency(store.price)}</span>
                    </li>
                  ))}
                </ol>
              </div>

              <div className="mt-4 grid gap-3 rounded-xl bg-[#211b1b] p-4 text-white sm:grid-cols-[1fr_auto] sm:items-center">
                <div className="flex items-center gap-3">
                  <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-white/10 text-[#ff746d]">
                    <CircleDollarSign className="size-4" aria-hidden />
                  </span>
                  <div>
                    <p className="text-xs font-semibold text-white/58">Diferença entre os preços</p>
                    <p className="mt-0.5 text-lg font-extrabold tracking-tight">Até {formatCurrency(priceDifference)}</p>
                  </div>
                </div>
                <span className="inline-flex items-center justify-center gap-1.5 rounded-lg bg-white px-3 py-2 text-xs font-extrabold text-[#991b16]">
                  Comparação pronta
                  <Check className="size-3.5" aria-hidden />
                </span>
              </div>
            </div>
          </div>

          <p className="mt-3 text-center text-[0.68rem] font-medium text-muted">
            Demonstração visual · produto {productIndex + 1} de {previewProducts.length} · valores ilustrativos
          </p>
        </div>
      </div>
    </section>
  );
}
