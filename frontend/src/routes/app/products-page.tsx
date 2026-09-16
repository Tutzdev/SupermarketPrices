import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { EmptyState, ErrorState, SkeletonRows } from "@/components/ui/feedback";
import { TextField } from "@/components/ui/form-field";
import { NativeButton } from "@/components/ui/native-button";
import { PageHeading, Pagination } from "@/components/page/page-elements";
import { catalogApi } from "@/services/gomo-api";

export function ProductsPage() {
  const [page, setPage] = useState(0);
  const [draft, setDraft] = useState("");
  const [query, setQuery] = useState("");
  const products = useQuery({ queryKey: ["products", { query, page }], queryFn: () => catalogApi.products({ query, page, size: 20 }) });

  return (
    <>
      <PageHeading title="Produtos" description="Pesquise no catálogo real por nome, marca, GTIN ou categoria." />
      <form className="surface mb-5 flex flex-col gap-3 p-4 sm:flex-row sm:items-end" onSubmit={(event) => { event.preventDefault(); setPage(0); setQuery(draft.trim()); }}>
        <div className="flex-1"><TextField id="product-search" label="Buscar produto" value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="Nome, marca, GTIN ou categoria" /></div>
        <NativeButton type="submit"><Search className="size-4" aria-hidden />Buscar</NativeButton>
      </form>

      {products.isLoading ? <SkeletonRows rows={7} /> : products.isError ? <ErrorState retry={() => void products.refetch()} /> : !products.data?.content.length ? <EmptyState title="Nenhum produto encontrado" description="Revise o termo ou tente uma busca mais ampla." /> : (
        <>
          <div className="surface overflow-x-auto">
            <table className="data-table min-w-[46rem]">
              <caption className="sr-only">Produtos encontrados no catálogo</caption>
              <thead><tr><th>Produto</th><th>Marca</th><th>Categoria</th><th>GTIN</th><th><span className="sr-only">Ações</span></th></tr></thead>
              <tbody>{products.data.content.map((product) => <tr key={product.id}><td><p className="font-semibold">{product.name}</p><p className="mt-1 max-w-xs truncate text-xs text-muted">{product.quantity ? `${product.quantity} ${product.unit ?? ""}` : product.description ?? "Sem descrição"}</p></td><td>{product.brand ?? "—"}</td><td>{product.category ?? "—"}</td><td className="font-mono text-xs">{product.gtin ?? "—"}</td><td className="text-right"><Link to={`/app/produtos/${product.id}`} className="font-semibold text-primary hover:underline">Ver detalhes</Link></td></tr>)}</tbody>
            </table>
          </div>
          <Pagination page={products.data.page} totalPages={products.data.totalPages} onChange={setPage} />
        </>
      )}
    </>
  );
}
