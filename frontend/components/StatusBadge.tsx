import { CircleCheck, Clock, TriangleAlert } from "lucide-react";
import { Badge } from "@/components/ui/badge";

export type Situacao = "PAGA" | "ABERTA" | "ATRASADA";

export const ROTULO: Record<Situacao, string> = { PAGA: "Pago", ABERTA: "A vencer", ATRASADA: "Atrasado" };

const CONFIG = {
  PAGA: { variant: "success", Icone: CircleCheck },
  ABERTA: { variant: "muted", Icone: Clock },
  ATRASADA: { variant: "destructive", Icone: TriangleAlert },
} as const;

export default function StatusBadge({ status }: { status: Situacao }) {
  const { variant, Icone } = CONFIG[status];
  return (
    <Badge variant={variant}>
      <Icone aria-hidden />
      {ROTULO[status]}
    </Badge>
  );
}
