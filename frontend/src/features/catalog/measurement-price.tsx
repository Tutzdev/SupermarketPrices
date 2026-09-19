import { formatCurrency } from "@/lib/brand";
import type { MeasurementPrice as Measurement } from "@/types/api";

export function MeasurementPrice({
  value,
}: {
  value: Measurement | null | undefined;
}) {
  if (!value) return null;
  const unit =
    (
      { KG: "kg", L: "litro", UN: "unidade", M: "metro" } as Record<
        string,
        string
      >
    )[value.unit] ?? value.unit;
  return (
    <p className="text-xs text-muted">
      {formatCurrency(value.amount)} / {unit}
    </p>
  );
}
