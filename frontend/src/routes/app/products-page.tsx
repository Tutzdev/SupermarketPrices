import { useQuery } from "@tanstack/react-query";
import { Search, SlidersHorizontal } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { EmptyState, ErrorState, SkeletonRows } from "@/components/ui/feedback";
import { SelectField } from "@/components/ui/form-field";
import { NativeButton } from "@/components/ui/native-button";
import {
  PageHeading,
  Pagination,
  StatusBadge,
} from "@/components/page/page-elements";
import { AddToList } from "@/features/catalog/add-to-list";
import { MeasurementPrice } from "@/features/catalog/measurement-price";
import { ProductImage } from "@/features/catalog/product-image";
import { ProductSearchFilters } from "@/features/catalog/product-search-filters";
import { productMetadata } from "@/features/catalog/product-label";
import { formatCurrency, formatDate } from "@/lib/brand";
import { catalogApi, type ProductFilters } from "@/services/gomo-api";

export function ProductsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [selectedCity, setSelectedCity] = useState("");
  const [filters, setFilters] = useState<ProductFilters>({});
  const [showFilters, setShowFilters] = useState(false);
  useEffect(() => {
    const timer = window.setTimeout(() => setQuery(search.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [search]);
  const cities = useQuery({
    queryKey: ["cities", "catalog"],
    queryFn: () => catalogApi.cities(),
  });
  const cityId =
    selectedCity ||
    cities.data?.content.find((city) => city.name === "Volta Redonda")?.id ||
    "";
  const products = useQuery({
    queryKey: ["products", "browse", query, page, filters],
    queryFn: ({ signal }) =>
      catalogApi.products({ ...filters, query, page, size: 20 }, signal),
  });
  const ids = products.data?.content.map((product) => product.id) ?? [];
  const offers = useQuery({
    queryKey: ["product-offers", ids, cityId],
    queryFn: ({ signal }) => catalogApi.offers(ids, cityId, signal),
    enabled: Boolean(ids.length && cityId),
  });
  const byProduct = new Map(
    offers.data?.map((item) => [item.productId, item.offers]),
  );
  const waiting = products.isPending || search.trim() !== query;

  return (
    <>
      <PageHeading
        title="Produtos"
        description="Pesquise do seu jeito e encontre os preços nas lojas da sua cidade."
      />
      <section className="surface mb-6" aria-label="Pesquisa no catálogo">
        <div className="grid gap-4 p-4 sm:p-5 lg:grid-cols-[1fr_16rem_auto] lg:items-end">
          <div>
            <label
              htmlFor="catalog-search"
              className="mb-2 block text-sm font-medium"
            >
              Buscar produto
            </label>
            <div className="relative">
              <Search
                className="pointer-events-none absolute left-3 top-3.5 size-5 text-muted"
                aria-hidden
              />
              <input
                id="catalog-search"
                type="search"
                className="field-control product-search-input w-full pl-10"
                placeholder="Ex.: coca cola 1 litro, arroz 5kg, detergente"
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
                autoComplete="off"
              />
            </div>
          </div>
          <SelectField
            id="catalog-city"
            label="Cidade dos preços"
            value={cityId}
            onChange={(event) => setSelectedCity(event.target.value)}
          >
            <option value="">Selecione a cidade</option>
            {cities.data?.content.map((city) => (
              <option key={city.id} value={city.id}>
                {city.name}
              </option>
            ))}
          </SelectField>
          <NativeButton
            variant="secondary"
            aria-expanded={showFilters}
            aria-controls="catalog-filters"
            onClick={() => setShowFilters(!showFilters)}
          >
            <SlidersHorizontal className="size-4" aria-hidden />
            Filtros
          </NativeButton>
        </div>
        {showFilters ? (
          <div id="catalog-filters">
            <ProductSearchFilters
              id="catalog"
              query={query}
              value={filters}
              onChange={(value) => {
                setFilters(value);
                setPage(0);
              }}
            />
          </div>
        ) : null}
      </section>
      {cities.isError ? (
        <ErrorState retry={() => void cities.refetch()} />
      ) : null}
      {offers.isError ? (
        <p role="alert" className="mb-4 text-sm text-danger">
          Não foi possível consultar os preços.{" "}
          <button
            type="button"
            className="underline"
            onClick={() => void offers.refetch()}
          >
            Tentar novamente
          </button>
        </p>
      ) : null}
      <section aria-live="polite" aria-busy={waiting}>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="font-semibold">
            {query ? `Resultados para “${query}”` : "Explorar o catálogo"}
          </h2>
          {!waiting && products.data ? (
            <span className="text-xs text-muted">
              {products.data.totalElements.toLocaleString("pt-BR")} produtos
            </span>
          ) : null}
        </div>
        {waiting ? (
          <SkeletonRows rows={5} />
        ) : products.isError ? (
          <ErrorState retry={() => void products.refetch()} />
        ) : !products.data?.content.length ? (
          <EmptyState
            title="Nenhum produto encontrado"
            description="Tente uma palavra mais curta ou remova os filtros."
          />
        ) : (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {products.data.content.map((product) => {
                const available = (byProduct.get(product.id) ?? []).filter(
                  (offer) =>
                    !filters.storeId || offer.storeId === filters.storeId,
                );
                const best = available[0];
                return (
                  <article
                    key={product.id}
                    className="surface flex min-w-0 flex-col p-4 sm:p-5"
                  >
                    <div className="flex items-start gap-3">
                      <ProductImage product={product} />
                      <div className="min-w-0">
                        <h3 className="text-sm font-semibold leading-6">
                          <Link
                            to={`/app/produtos/${product.id}`}
                            className="hover:text-primary hover:underline focus-visible:outline-2 focus-visible:outline-focus"
                          >
                            {product.name}
                          </Link>
                        </h3>
                        <p className="mt-1 text-xs leading-5 text-muted">
                          {productMetadata(product)}
                        </p>
                      </div>
                    </div>
                    <div className="mb-5 mt-5 flex-1">
                      {offers.isFetching ? (
                        <p className="text-sm text-muted">
                          Consultando preços…
                        </p>
                      ) : best && best.price.unitPrice !== null ? (
                        <>
                          {available.length > 1 ? (
                            <StatusBadge tone="success">
                              Menor preço entre {available.length} lojas
                            </StatusBadge>
                          ) : null}
                          <p className="mt-2 text-2xl font-extrabold tabular-nums">
                            {formatCurrency(best.price.unitPrice)}
                          </p>
                          <MeasurementPrice value={best.measurementPrice} />
                          <p className="mt-2 text-sm font-medium">
                            {best.storeName}
                          </p>
                          <p className="mt-1 text-xs text-muted">
                            Coletado em{" "}
                            {formatDate(best.price.observation?.collectedAt)}
                          </p>
                          {best.price.availability === "UNKNOWN" ? (
                            <p className="mt-1 text-xs text-warning">
                              Estoque não informado pela loja
                            </p>
                          ) : null}
                        </>
                      ) : (
                        <p className="text-sm text-muted">
                          {offers.isError
                            ? "Preços indisponíveis no momento."
                            : cityId
                              ? "Sem preço atual nesta cidade."
                              : "Selecione a cidade para consultar preços."}
                        </p>
                      )}
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <NativeButton
                        to={`/app/produtos/${product.id}`}
                        size="sm"
                      >
                        Comparar
                      </NativeButton>
                      <AddToList product={product} />
                    </div>
                  </article>
                );
              })}
            </div>
            <Pagination
              page={page}
              totalPages={products.data.totalPages}
              onChange={setPage}
            />
          </>
        )}
      </section>
    </>
  );
}
