import { formatCurrency, formatDate } from "@/lib/brand";
import type { ShoppingComparisonItem } from "@/types/api";

const statusLabels = { KNOWN: "Preço atual", EXPIRED: "Preço desatualizado", OUT_OF_STOCK: "Indisponível", NO_OBSERVATION: "Sem preço nesta loja" };

export function ComparisonItems({ items }: { items: ShoppingComparisonItem[] }) {
  return <div className="overflow-x-auto">
    <table className="w-full text-left text-sm">
      <thead><tr className="border-b border-border text-muted"><th className="p-3 font-medium">Produto e situação</th><th className="p-3 text-right font-medium">Qtd.</th><th className="p-3 text-right font-medium">Unitário</th><th className="p-3 text-right font-medium">Subtotal</th></tr></thead>
      <tbody>{items.map((item) => <tr key={item.productId} className="border-b border-border last:border-0">
        <td className="min-w-52 p-3"><p className="font-semibold">{item.productName}</p><p className="mt-1 text-xs text-muted">{statusLabels[item.price.status]}{item.price.status === "KNOWN" && item.price.availability === "UNKNOWN" ? " · estoque não informado" : ""}</p>
          {item.price.observation ? <details className="mt-1 text-xs text-muted"><summary className="cursor-pointer">Coleta: {formatDate(item.price.observation.collectedAt)}</summary><p className="mt-1 break-all">Origem: {item.price.observation.sourceReference}</p>{item.price.observation.promotionalPrice !== null && !item.price.promotionApplied ? <p className="mt-1">Promoção não aplicada: {item.price.observation.promotionCondition ?? "validade não confirmada pela fonte"}.</p> : null}</details> : null}
        </td>
        <td className="p-3 text-right tabular-nums">{item.quantity.toLocaleString("pt-BR")}</td>
        <td className="whitespace-nowrap p-3 text-right tabular-nums">{item.price.unitPrice === null ? "—" : formatCurrency(item.price.unitPrice)}</td>
        <td className="whitespace-nowrap p-3 text-right font-semibold tabular-nums">{item.lineTotal === null ? "—" : formatCurrency(item.lineTotal)}</td>
      </tr>)}</tbody>
    </table>
  </div>;
}
