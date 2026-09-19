import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { ErrorState, LoadingState } from "@/components/ui/feedback";
import { SelectField } from "@/components/ui/form-field";
import { catalogApi } from "@/services/gomo-api";
import { StoreOffers } from "./store-offers";

export function ProductOffersPanel({ productId }: { productId: string }) {
  const [selectedCity, setSelectedCity] = useState("");
  const cities = useQuery({
    queryKey: ["cities", "product-offers"],
    queryFn: () => catalogApi.cities(),
  });
  const cityId =
    selectedCity ||
    cities.data?.content.find((city) => city.name === "Volta Redonda")?.id ||
    "";
  const comparison = useQuery({
    queryKey: ["comparison", productId, cityId],
    queryFn: () => catalogApi.compareProduct(productId, cityId),
    enabled: Boolean(cityId),
  });
  return (
    <section className="mb-8" aria-label="Preços por supermercado">
      <div className="mb-5 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold">Compare nas lojas</h2>
          <p className="mt-1 text-sm text-muted">
            Mesma embalagem, com preços e atualização de cada mercado.
          </p>
        </div>
        <div className="w-full sm:w-64">
          <SelectField
            id="product-offers-city"
            label="Cidade"
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
        </div>
      </div>
      {cities.isError ? (
        <ErrorState retry={() => void cities.refetch()} />
      ) : comparison.isError ? (
        <ErrorState retry={() => void comparison.refetch()} />
      ) : cities.isPending || (cityId && comparison.isPending) ? (
        <LoadingState label="Comparando preços…" />
      ) : comparison.data ? (
        <StoreOffers stores={comparison.data.stores.content} />
      ) : (
        <p className="text-sm text-muted">Selecione a cidade para comparar.</p>
      )}
    </section>
  );
}
