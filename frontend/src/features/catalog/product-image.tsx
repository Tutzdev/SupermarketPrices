import { Package } from "lucide-react";
import { useState } from "react";
import type { Product } from "@/types/api";

export function ProductImage({
  product,
  large = false,
}: {
  product: Product;
  large?: boolean;
}) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  return (
    <span
      className={`grid shrink-0 place-items-center overflow-hidden rounded-lg bg-white ${large ? "size-24" : "size-14"}`}
    >
      {product.imageUrl && product.imageUrl !== failedUrl ? (
        <img
          src={product.imageUrl}
          alt=""
          loading="lazy"
          referrerPolicy="no-referrer"
          width={large ? 96 : 56}
          height={large ? 96 : 56}
          className="size-full object-contain"
          onError={() => setFailedUrl(product.imageUrl)}
        />
      ) : (
        <Package className="size-7 text-muted" aria-hidden />
      )}
    </span>
  );
}
