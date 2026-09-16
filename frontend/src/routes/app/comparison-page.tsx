import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, Clock3, Search, Store } from "lucide-react";
import { useMemo, useState } from "react";
import { EmptyState, ErrorState, LoadingState } from "@/components/ui/feedback";
import { SelectField, TextField } from "@/components/ui/form-field";
import { NativeButton } from "@/components/ui/native-button";
import { PageHeading, StatusBadge } from "@/components/page/page-elements";
import { catalogApi } from "@/services/gomo-api";
import { formatCurrency, formatDate } from "@/lib/brand";

export function ComparisonPage() {
  const [search, setSearch] = useState("");
  const [productId, setProductId] = useState("");
  const [cityId, setCityId] = useState("");
  const [submitted, setSubmitted] = useState<{ productId: string; cityId: string } | null>(null);
  const products = useQuery({ queryKey: ["products", "comparison", search], queryFn: () => catalogApi.products({ query: search, size: 100 }) });
  const cities = useQuery({ queryKey: ["cities", "comparison"], queryFn: () => catalogApi.cities(undefined, 0, 100) });
  const comparison = useQuery({
    queryKey: ["comparison", submitted],
    queryFn: () => catalogApi.compareProduct(submitted!.productId, submitted!.cityId),
    enabled: Boolean(submitted),
  });
  const sortedStores = useMemo(() => comparison.data?.stores.content.slice().sort((a, b) => (a.price.unitPrice ?? Number.POSITIVE_INFINITY) - (b.price.unitPrice ?? Number.POSITIVE_INFINITY)) ?? [], [comparison.data]);

  return (
    <>
      <PageHeading title="Comparar preços" description="Escolha uma cidade e um produto para consultar preços, promoções e disponibilidade por loja." />
      <form className="surface grid gap-4 p-4 sm:p-5 lg:grid-cols-[0.8fr_1.2fr_1.2fr_auto] lg:items-end" onSubmit={(event) => { event.preventDefault(); if (productId && cityId) setSubmitted({ productId, cityId }); }}>
        <TextField id="comparison-search" label="Filtrar produtos" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Digite para filtrar" />
        <SelectField id="comparison-product" label="Produto" value={productId} onChange={(event) => setProductId(event.target.value)} required><option value="">Selecione um produto</option>{products.data?.content.map((product) => <option key={product.id} value={product.id}>{product.name}{product.brand ? ` — ${product.brand}` : ""}</option>)}</SelectField>
        <SelectField id="comparison-city" label="Cidade" value={cityId} onChange={(event) => setCityId(event.target.value)} required><option value="">Selecione uma cidade</option>{cities.data?.content.map((city) => <option key={city.id} value={city.id}>{city.name}</option>)}</SelectField>
        <NativeButton type="submit" disabled={!productId || !cityId}><Search className="size-4" aria-hidden />Comparar</NativeButton>
      </form>

      <section className="mt-6" aria-live="polite" aria-busy={comparison.isFetching}>
        {!submitted ? <EmptyState title="Pronto para comparar" description="Selecione o produto e a cidade para iniciar uma consulta real." /> : comparison.isLoading ? <LoadingState label="Comparando preços…" /> : comparison.isError ? <ErrorState retry={() => void comparison.refetch()} /> : !sortedStores.length ? <EmptyState title="Nenhuma loja disponível" description="Não há supermercados ativos para esta cidade." /> : (
          <>
            <div className="mb-4 flex flex-wrap items-end justify-between gap-3"><div><h2 className="text-xl font-extrabold">{comparison.data?.productName}</h2><p className="mt-1 text-sm text-muted">Comparação realizada em {formatDate(comparison.data?.comparedAt)}</p></div><p className="text-sm text-muted">{comparison.data?.stores.totalElements} supermercado(s)</p></div>
            <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
              {sortedStores.map((item, index) => (
                <article key={item.storeId} className={`surface p-5 ${index === 0 && item.price.unitPrice !== null ? "border-primary/40" : ""}`}>
                  <div className="flex items-start justify-between gap-4"><span className="grid size-10 place-items-center rounded-lg bg-surface-strong text-muted"><Store className="size-5" aria-hidden /></span>{index === 0 && item.price.unitPrice !== null ? <StatusBadge tone="success">Menor preço</StatusBadge> : null}</div>
                  <h3 className="mt-4 font-bold">{item.storeName}</h3>
                  {item.price.unitPrice !== null ? <><p className="mt-4 text-2xl font-extrabold">{formatCurrency(item.price.unitPrice)}</p><div className="mt-3 flex flex-wrap gap-2">{item.price.promotionApplied ? <StatusBadge tone="success">Promoção</StatusBadge> : null}<StatusBadge tone={item.price.availability === "AVAILABLE" ? "success" : item.price.availability === "UNAVAILABLE" ? "danger" : "warning"}>{item.price.availability === "AVAILABLE" ? "Em estoque" : item.price.availability === "UNAVAILABLE" ? "Sem estoque" : "Estoque incerto"}</StatusBadge></div><p className="mt-4 flex items-center gap-2 text-xs text-muted"><Clock3 className="size-3.5" aria-hidden />Coletado em {formatDate(item.price.observation?.collectedAt)}</p></> : <div className="mt-5 rounded-lg bg-warning-soft p-4"><p className="flex gap-2 text-sm font-semibold text-warning"><Clock3 className="size-4" aria-hidden />Sem preço recente</p><p className="mt-1 text-xs text-muted">A Gomo não encontrou uma observação válida para esta loja.</p></div>}
                </article>
              ))}
            </div>
            <p className="mt-5 flex items-start gap-2 text-sm text-muted"><CheckCircle2 className="mt-0.5 size-4 shrink-0 text-success" aria-hidden />Os resultados exibem a data e não tratam observações ausentes como preços válidos.</p>
          </>
        )}
      </section>
    </>
  );
}
