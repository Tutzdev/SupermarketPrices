import { useQuery } from "@tanstack/react-query";
import { Package } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { EmptyState, ErrorState, SkeletonRows } from "@/components/ui/feedback";
import { PageHeading, Pagination } from "@/components/page/page-elements";
import { ProductPicker } from "@/features/catalog/product-picker";
import { productMetadata } from "@/features/catalog/product-label";
import { catalogApi } from "@/services/gomo-api";

export function ProductsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [searching, setSearching] = useState(false);
  const products = useQuery({ queryKey: ["products", "browse", page], queryFn: ({ signal }) => catalogApi.products({ page, size: 20, sort: "name" }, signal), enabled: !searching });

  return <>
    <PageHeading title="Produtos" description="Encontre o que precisa por nome, marca ou tamanho da embalagem." />
    <section className="surface mb-6 max-w-3xl p-4 sm:p-5" aria-label="Pesquisa no catálogo">
      <ProductPicker id="catalog-product" value="" onChange={(id) => { if (id) navigate(`/app/produtos/${id}`); }} onSearchChange={(text) => setSearching(text.trim().length >= 2)} />
    </section>
    {!searching ? <>
      <div className="mb-4 flex items-center justify-between gap-3"><h2 className="font-semibold">Explorar o catálogo</h2><span className="text-xs text-muted">{products.data?.totalElements.toLocaleString("pt-BR")} produtos</span></div>
      {products.isLoading ? <SkeletonRows rows={5} /> : products.isError ? <ErrorState retry={() => void products.refetch()} /> : !products.data?.content.length ? <EmptyState title="Catálogo vazio" description="Os produtos aparecerão após a coleta das lojas." /> : <>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{products.data.content.map((product) => <Link key={product.id} to={`/app/produtos/${product.id}`} className="surface flex items-start gap-3 p-4 transition-colors hover:bg-surface-strong focus-visible:outline-2 focus-visible:outline-focus">
          <span className="grid size-10 shrink-0 place-items-center rounded-md bg-surface-strong text-muted"><Package className="size-5" aria-hidden /></span>
          <span className="min-w-0"><span className="block text-sm font-semibold leading-6">{product.name}</span><span className="mt-1 block text-xs leading-5 text-muted">{productMetadata(product)}</span></span>
        </Link>)}</div>
        <Pagination page={page} totalPages={products.data.totalPages} onChange={setPage} />
      </>}
    </> : null}
  </>;
}
