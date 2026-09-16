import gomoIcon from "../../../gomo-icone.png";
import gomoLogo from "../../../gomo-logo.png";
import { cn } from "@/lib/cn";

interface BrandLogoProps {
  compact?: boolean;
  className?: string;
}

export function BrandLogo({ compact = false, className }: BrandLogoProps) {
  return (
    <img
      src={compact ? gomoIcon : gomoLogo}
      alt="Gomo"
      className={cn(
        "block object-contain",
        compact ? "size-9" : "h-8 w-auto sm:h-9",
        className,
      )}
    />
  );
}
