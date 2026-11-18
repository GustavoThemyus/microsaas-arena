"use client";

import Link from "next/link";
import { ChevronRight, Clock, Eye, EyeOff, Plus, RotateCcw, TriangleAlert } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import Acordando from "@/components/Acordando";
import StatusBadge, { ROTULO } from "@/components/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardSection } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import GraficoVencimentos from "@/components/painel/GraficoVencimentos";
import RankingTurmas from "@/components/painel/RankingTurmas";
import type { Item } from "@/components/painel/tipos";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { acordarApi, api, capturarTokenDaUrl, dataBr, reais } from "@/lib/api";

type Painel = {
  mes: string;
  totais: {
    recebidoCentavos: number;
    emAbertoCentavos: number;
    atrasadoCentavos: number;
    pagos: number;
    abertos: number;
    atrasados: number;
  };
  itens: Item[];
};

const FILTROS = ["TODOS", "ATRASADA", "ABERTA", "PAGA"] as const;
type Filtro = (typeof FILTROS)[number];

export default function PainelPage() {
  const [painel, setPainel] = useState<Painel | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [tentativa, setTentativa] = useState(0);
  const [filtro, setFiltro] = useState<Filtro>("TODOS");
  const [oculto, setOculto] = useState(false);

  const carregar = useCallback(async () => {
    try {
      setPainel(await api<Painel>("/api/painel"));
      setErro(null);
    } catch (e) {
      setErro((e as Error).message);
    }
  }, []);

  useEffect(() => {
    capturarTokenDaUrl();
    let ativo = true;
    let timer: ReturnType<typeof setInterval> | undefined;
    acordarApi((n) => ativo && setTentativa(n))
      .then(async () => {
        if (!ativo) return;
        await carregar();
        // "Atualiza sozinho": consulta a cada 5s (mais simples que WebSocket e suficiente aqui).
        timer = setInterval(carregar, 5000);
      })
      .catch((e: Error) => ativo && setErro(e.message));
    return () => {
      ativo = false;
      if (timer) clearInterval(timer);
    };
  }, [carregar]);

  if (erro && !painel) return <p className="mt-16 text-center text-destructive">{erro}</p>;
  if (!painel) return <Acordando tentativa={tentativa} />;

  const t = painel.totais;
  const total = t.pagos + t.abertos + t.atrasados;
  const contagem: Record<Filtro, number> = { TODOS: total, ATRASADA: t.atrasados, ABERTA: t.abertos, PAGA: t.pagos };
  const itens = painel.itens.filter((i) => filtro === "TODOS" || i.status === filtro);
  const [ano, mes] = painel.mes.split("-");

  return (
    <>
      <header>
        <h1 className="text-2xl font-bold tracking-tight">Início</h1>
        <p className="text-sm text-muted-foreground">
          Escolinha Modelo · mensalidades de {mes}/{ano}
        </p>
      </header>

      <Card className="mt-4 divide-y">
        <CardSection className="flex items-center justify-between gap-3">
          <div className="min-w-0">
            <p className="text-xs font-medium text-muted-foreground">Recebido no mês:</p>
            <p className="mt-0.5 flex items-center gap-2 text-2xl font-bold tracking-tight">
              {oculto ? "R$ ••••••" : reais(t.recebidoCentavos)}
              <button
                type="button"
                onClick={() => setOculto((o) => !o)}
                aria-label={oculto ? "Mostrar valores" : "Ocultar valores"}
                className="rounded-md p-1 text-muted-foreground outline-none hover:bg-muted focus-visible:ring-[3px] focus-visible:ring-ring/50"
              >
                {oculto ? <EyeOff className="size-4" aria-hidden /> : <Eye className="size-4" aria-hidden />}
              </button>
            </p>
          </div>
          <Button asChild size="sm" variant="outline" className="shrink-0">
            <Link href="/importar">
              <Plus aria-hidden />
              Importar alunos
            </Link>
          </Button>
        </CardSection>
        <CardSection className="py-3">
          <Progress
            value={total ? Math.round((t.pagos / total) * 100) : 0}
            aria-label="Alunos que já pagaram"
          />
          <p className="mt-1.5 text-xs text-muted-foreground">
            {t.pagos} de {total} alunos pagaram
          </p>
        </CardSection>
        <div className="grid grid-cols-2 divide-x">
          <CardSection>
            <p className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
              <Clock className="size-3.5" aria-hidden />
              Em aberto
            </p>
            <p className="mt-1 text-lg font-bold">{oculto ? "R$ ••••" : reais(t.emAbertoCentavos)}</p>
            <p className="text-xs text-muted-foreground">{t.abertos + t.atrasados} alunos</p>
          </CardSection>
          <CardSection>
            <p className="flex items-center gap-1.5 text-xs font-medium text-destructive">
              <TriangleAlert className="size-3.5" aria-hidden />
              Atrasado
            </p>
            <p className="mt-1 text-lg font-bold text-destructive">{oculto ? "R$ ••••" : reais(t.atrasadoCentavos)}</p>
            <p className="text-xs text-muted-foreground">{t.atrasados} alunos</p>
          </CardSection>
        </div>
      </Card>

      <h2 className="mb-2 mt-6 text-sm font-semibold">Mensalidades do mês</h2>
      <Tabs value={filtro} onValueChange={(v) => setFiltro(v as Filtro)}>
        <TabsList aria-label="Filtrar por situação">
          {FILTROS.map((f) => (
            <TabsTrigger key={f} value={f}>
              {f === "TODOS" ? "Todos" : ROTULO[f]}
              <span className="text-xs opacity-60">{contagem[f]}</span>
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>

      <Card className="mt-3">
        <div className="grid grid-cols-[3rem_1fr_auto] gap-3 border-b bg-muted/60 px-4 py-2 text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
          <span>Data</span>
          <span>Aluno</span>
          <span className="pr-5 text-right">Valor</span>
        </div>
        <ul className="divide-y">
          {itens.map((i) => (
            <li key={i.cobrancaId}>
              <Link
                href={`/cobranca?id=${i.cobrancaId}`}
                className="grid grid-cols-[3rem_1fr_auto] items-center gap-3 px-4 py-3 outline-none active:bg-muted focus-visible:bg-muted"
              >
                <span className="text-xs text-muted-foreground">{dataBr(i.vencimento).slice(0, 5)}</span>
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold">{i.aluno}</p>
                  <Badge variant="lime" className="mt-1 max-w-full truncate">
                    {i.turma}
                  </Badge>
                </div>
                <div className="flex items-center gap-1.5">
                  <div className="flex flex-col items-end gap-1">
                    <p className="text-sm font-semibold">{reais(i.valorCentavos)}</p>
                    <StatusBadge status={i.status} />
                  </div>
                  <ChevronRight className="size-4 shrink-0 text-muted-foreground/60" aria-hidden />
                </div>
              </Link>
            </li>
          ))}
          {itens.length === 0 && (
            <li className="py-8 text-center text-sm text-muted-foreground">Nenhum aluno nesta situação.</li>
          )}
        </ul>
      </Card>

      <h2 className="mb-2 mt-6 text-sm font-semibold">Turmas com mais a receber</h2>
      <RankingTurmas itens={painel.itens} />

      <h2 className="mb-2 mt-6 text-sm font-semibold">Evolução dos vencimentos</h2>
      <GraficoVencimentos itens={painel.itens} />

      <footer className="mt-6 flex justify-center">
        <Button
          variant="ghost"
          size="sm"
          onClick={async () => {
            if (confirm("Restaurar os dados originais da demonstração?")) {
              await api("/api/demo/resetar", { method: "POST" });
              await carregar();
            }
          }}
        >
          <RotateCcw aria-hidden />
          Restaurar dados da demo
        </Button>
      </footer>
    </>
  );
}
