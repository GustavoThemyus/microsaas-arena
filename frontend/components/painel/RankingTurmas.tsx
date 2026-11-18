import { Card, CardSection } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { reais } from "@/lib/api";
import type { Item } from "./tipos";

/** "Classificações mais utilizadas" da referência: aqui, as turmas com mais dinheiro a receber. */
export default function RankingTurmas({ itens }: { itens: Item[] }) {
  const porTurma = new Map<string, { pago: number; aberto: number }>();
  for (const i of itens) {
    const t = porTurma.get(i.turma) ?? { pago: 0, aberto: 0 };
    if (i.status === "PAGA") t.pago += i.valorCentavos;
    else t.aberto += i.valorCentavos;
    porTurma.set(i.turma, t);
  }
  const linhas = [...porTurma.entries()]
    .map(([turma, v]) => ({ turma, ...v, total: v.pago + v.aberto }))
    .sort((a, b) => b.aberto - a.aberto)
    .slice(0, 5);
  const maior = Math.max(1, ...linhas.map((l) => l.total));

  return (
    <Card className="divide-y">
      <CardSection className="flex flex-wrap items-center justify-center gap-x-4 gap-y-1 py-3 text-xs text-muted-foreground">
        <span className="flex items-center gap-1.5">
          <span className="size-2 rounded-full bg-chart" aria-hidden /> Recebido
        </span>
        <span className="flex items-center gap-1.5">
          <span className="size-2 rounded-full bg-chart-soft" aria-hidden /> A receber
        </span>
      </CardSection>
      <ol className="divide-y">
        {linhas.map((l, n) => (
          <li key={l.turma} className="px-4 py-3">
            <p className="text-xs font-semibold text-muted-foreground">{n + 1}</p>
            <div className="mt-1 flex items-center justify-between gap-2">
              <Badge variant="lime">{l.turma}</Badge>
              <span className="text-xs font-semibold">{reais(l.aberto)}</span>
            </div>
            <div className="mt-2 flex h-2 w-full overflow-hidden rounded-full bg-muted" aria-hidden>
              <div className="h-full bg-chart" style={{ width: `${(l.pago / maior) * 100}%` }} />
              <div className="h-full bg-chart-soft" style={{ width: `${(l.aberto / maior) * 100}%` }} />
            </div>
          </li>
        ))}
      </ol>
    </Card>
  );
}
