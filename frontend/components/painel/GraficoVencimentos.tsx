import { Card, CardSection } from "@/components/ui/card";
import { dataBr } from "@/lib/api";
import type { Item } from "./tipos";

/** "Evolução das despesas" da referência: valor das mensalidades por dia de vencimento, pago x em aberto. */
export default function GraficoVencimentos({ itens }: { itens: Item[] }) {
  const porDia = new Map<string, { pago: number; aberto: number; atrasado: number }>();
  for (const i of itens) {
    const d = porDia.get(i.vencimento) ?? { pago: 0, aberto: 0, atrasado: 0 };
    if (i.status === "PAGA") d.pago += i.valorCentavos;
    else if (i.status === "ATRASADA") d.atrasado += i.valorCentavos;
    else d.aberto += i.valorCentavos;
    porDia.set(i.vencimento, d);
  }
  const dias = [...porDia.entries()].sort(([a], [b]) => a.localeCompare(b));
  if (dias.length === 0) return null;

  const maximo = Math.max(...dias.map(([, d]) => d.pago + d.aberto + d.atrasado));
  const passo = Math.max(10000, Math.ceil(maximo / 2 / 10000) * 10000); // arredonda de R$ 100 em R$ 100
  const topo = passo * 2;
  const rotuloEixo = (v: number) => `R$ ${v / 100}`;
  const curto = (iso: string) => dataBr(iso).slice(0, 5);

  return (
    <Card className="divide-y">
      <CardSection className="flex flex-wrap items-center justify-center gap-x-4 gap-y-1 py-3 text-xs text-muted-foreground">
        <span className="flex items-center gap-1.5">
          <span className="size-2 rounded-full bg-chart" aria-hidden /> Pago
        </span>
        <span className="flex items-center gap-1.5">
          <span className="size-2 rounded-full bg-chart-soft" aria-hidden /> A vencer
        </span>
        <span className="flex items-center gap-1.5">
          <span className="size-2 rounded-full bg-destructive/70" aria-hidden /> Atrasado
        </span>
      </CardSection>
      <CardSection>
        <div className="flex gap-2">
          <div className="flex h-36 flex-col justify-between text-right text-[10px] text-muted-foreground">
            <span>{rotuloEixo(topo)}</span>
            <span>{rotuloEixo(passo)}</span>
            <span>R$ 0</span>
          </div>
          <div className="relative min-w-0 flex-1">
            <div className="pointer-events-none absolute inset-x-0 top-0 flex h-36 flex-col justify-between" aria-hidden>
              <div className="border-t border-dashed" />
              <div className="border-t border-dashed" />
              <div className="border-t" />
            </div>
            <div
              className="relative flex h-36 items-end gap-[3px]"
              role="img"
              aria-label="Valor das mensalidades por dia de vencimento"
            >
              {dias.map(([dia, d]) => (
                <div
                  key={dia}
                  className="flex h-full min-w-0 flex-1 flex-col justify-end overflow-hidden rounded-t-sm"
                  title={`${curto(dia)}: pago ${d.pago / 100}, a vencer ${d.aberto / 100}, atrasado ${d.atrasado / 100}`}
                >
                  <div className="bg-destructive/70" style={{ height: `${(d.atrasado / topo) * 100}%` }} />
                  <div className="bg-chart-soft" style={{ height: `${(d.aberto / topo) * 100}%` }} />
                  <div className="bg-chart" style={{ height: `${(d.pago / topo) * 100}%` }} />
                </div>
              ))}
            </div>
            <div className="mt-1.5 flex justify-between text-[10px] text-muted-foreground">
              <span>{curto(dias[0][0])}</span>
              <span>{curto(dias[Math.floor(dias.length / 2)][0])}</span>
              <span>{curto(dias[dias.length - 1][0])}</span>
            </div>
          </div>
        </div>
      </CardSection>
    </Card>
  );
}
