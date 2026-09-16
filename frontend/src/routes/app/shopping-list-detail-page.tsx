import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CircleDollarSign, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { EmptyState, ErrorState, InlineError, LoadingState } from "@/components/ui/feedback";
import { SelectField, TextField } from "@/components/ui/form-field";
import { NativeButton } from "@/components/ui/native-button";
import { PageHeading, StatusBadge } from "@/components/page/page-elements";
import { ApiError } from "@/lib/api";
import { formatCurrency, formatDate } from "@/lib/brand";
import { catalogApi, shoppingListApi } from "@/services/gomo-api";

export function ShoppingListDetailPage() {
  const { id = "" } = useParams();
  const queryClient = useQueryClient();
  const [productId, setProductId] = useState("");
  const [quantity, setQuantity] = useState("1");
  const [cityId, setCityId] = useState("");
  const [compareCityId, setCompareCityId] = useState("");
  const [apiError, setApiError] = useState<string | null>(null);
  const list = useQuery({ queryKey: ["shopping-list", id], queryFn: () => shoppingListApi.get(id), enabled: Boolean(id) });
  const products = useQuery({ queryKey: ["products", "list-item"], queryFn: () => catalogApi.products({ size: 100 }) });
  const cities = useQuery({ queryKey: ["cities", "list-compare"], queryFn: () => catalogApi.cities(undefined, 0, 100) });
  const comparison = useQuery({ queryKey: ["shopping-list-comparison", id, compareCityId], queryFn: () => shoppingListApi.compare(id, compareCityId), enabled: Boolean(compareCityId) });
  const recommendation = useQuery({ queryKey: ["shopping-list-recommendation", id, compareCityId], queryFn: () => shoppingListApi.recommendation(id, compareCityId), enabled: Boolean(compareCityId) });
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["shopping-list", id] });
  const addItem = useMutation({ mutationFn: () => shoppingListApi.addItem(id, { productId, quantity: Number(quantity) }), onSuccess: async () => { setProductId(""); setQuantity("1"); setApiError(null); await invalidate(); }, onError: (error) => setApiError(error instanceof ApiError ? error.message : "Não foi possível adicionar o item.") });
  const removeItem = useMutation({ mutationFn: (itemId: string) => shoppingListApi.removeItem(id, itemId), onSuccess: invalidate });

  if (list.isLoading) return <LoadingState />;
  if (list.isError || !list.data) return <ErrorState retry={() => void list.refetch()} />;

  return (
    <>
      <Link to="/app/listas" className="mb-4 inline-flex items-center gap-2 text-sm font-semibold text-muted hover:text-foreground"><ArrowLeft className="size-4" aria-hidden />Voltar às listas</Link>
      <PageHeading title={list.data.name} description={`Lista ${list.data.shoppingType === "DAILY" ? "diária" : list.data.shoppingType === "WEEKLY" ? "semanal" : list.data.shoppingType === "MONTHLY" ? "mensal" : "personalizada"} • Atualizada em ${formatDate(list.data.updatedAt)}`} />
      <div className="grid gap-6 xl:grid-cols-[1fr_0.8fr]">
        <section className="surface overflow-hidden"><div className="border-b border-border p-5"><h2 className="font-bold">Itens da lista</h2><form className="mt-4 grid gap-3 sm:grid-cols-[1fr_7rem_auto] sm:items-end" onSubmit={(event) => { event.preventDefault(); if (productId && Number(quantity) > 0) addItem.mutate(); }}><SelectField id="list-product" label="Produto" value={productId} onChange={(event) => setProductId(event.target.value)} required><option value="">Selecione</option>{products.data?.content.map((product) => <option key={product.id} value={product.id}>{product.name}</option>)}</SelectField><TextField id="list-quantity" label="Quantidade" type="number" min="0.001" max="999999" step="0.001" value={quantity} onChange={(event) => setQuantity(event.target.value)} /><NativeButton type="submit" loading={addItem.isPending} disabled={!productId}><Plus className="size-4" aria-hidden />Adicionar</NativeButton></form><InlineError>{apiError}</InlineError></div>{!list.data.items.length ? <div className="p-5"><EmptyState title="Lista vazia" description="Adicione produtos para preparar a comparação." /></div> : <ul className="divide-y divide-border">{list.data.items.map((item) => <li key={item.id} className="flex items-center justify-between gap-4 px-5 py-4"><div className="min-w-0"><p className="truncate font-semibold">{item.productName}</p><p className="mt-1 text-sm text-muted">Quantidade: {item.quantity}</p></div><button type="button" className="icon-button" aria-label={`Remover ${item.productName}`} onClick={() => removeItem.mutate(item.id)}><Trash2 className="size-4" aria-hidden /></button></li>)}</ul>}</section>
        <section className="surface p-5"><h2 className="font-bold">Comparar a lista</h2><p className="mt-1 text-sm text-muted">Escolha a cidade para calcular cobertura e recomendação.</p><div className="mt-5"><SelectField id="list-city" label="Cidade" value={cityId} onChange={(event) => setCityId(event.target.value)}><option value="">Selecione</option>{cities.data?.content.map((city) => <option key={city.id} value={city.id}>{city.name}</option>)}</SelectField><NativeButton className="mt-4 w-full" disabled={!cityId || !list.data.items.length} onClick={() => setCompareCityId(cityId)}><CircleDollarSign className="size-4" aria-hidden />Comparar lista</NativeButton></div>{compareCityId ? <div className="mt-6 border-t border-border pt-5" aria-live="polite">{comparison.isLoading || recommendation.isLoading ? <LoadingState label="Calculando comparação…" /> : comparison.isError || recommendation.isError ? <ErrorState retry={() => { void comparison.refetch(); void recommendation.refetch(); }} /> : recommendation.data?.recommendation ? <><div className="rounded-lg border border-primary/20 bg-primary-soft p-4"><StatusBadge tone={recommendation.data.status === "COMPLETE_STORE_FOUND" ? "success" : "warning"}>{recommendation.data.status === "COMPLETE_STORE_FOUND" ? "Lista completa" : "Melhor cobertura parcial"}</StatusBadge><h3 className="mt-3 font-bold">{recommendation.data.recommendation.storeName}</h3><p className="mt-2 text-2xl font-extrabold">{formatCurrency(recommendation.data.recommendation.total)}</p><p className="mt-2 text-sm text-muted">{recommendation.data.recommendation.pricedItems} de {recommendation.data.recommendation.requestedItems} itens com preço.</p></div><div className="mt-4 space-y-2">{comparison.data?.stores.content.slice(0, 5).map((store) => <div key={store.storeId} className="flex items-center justify-between gap-4 rounded-lg border border-border p-3 text-sm"><span className="font-semibold">{store.storeName}</span><span>{formatCurrency(store.subtotalKnown)}</span></div>)}</div></> : <EmptyState title="Sem recomendação" description="Nenhuma loja possui preços suficientes para esta lista." />}</div> : null}</section>
      </div>
    </>
  );
}
