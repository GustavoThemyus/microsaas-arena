"use client";

import Link from "next/link";
import { ChevronRight, Clock, GraduationCap, RotateCcw, TriangleAlert, Upload, Wallet } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import Acordando from "@/components/Acordando";
import StatusBadge, { ROTULO, type Situacao } from "@/components/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardSection } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { acordarApi, api, capturarTokenDaUrl, dataBr, reais } from "@/lib/api";

type Item = {
  cobrancaId: number;
  aluno: string;
  turma: string;
  valorCentavos: number;
  vencimento: string;
  status: Situacao;
};
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

function iniciais(nome: string) {
  const p = nome.trim().split(/\s+/);
  return ((p[0]?.[0] ?? "") + (p.length > 1 ? p[p.length - 1][0] : "")).toUpperCase();
}

export default function PainelPage() {
  const [painel, setPainel] = useState<Painel | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [tentativa, setTentativa] = useState(0);
  const [filtro, setFiltro] = useState<Filtro>("TODOS");

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
      <header className="flex items-center gap-3">
        <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm shadow-black/10">
          <GraduationCap className="size-6" aria-hidden />
        </div>
        <div className="min-w-0 flex-1">
          <h1 className="truncate text-lg font-bold leading-tight">Escolinha Modelo</h1>
          <p className="text-sm text-muted-foreground">
            Mensalidades de {mes}/{ano}
          </p>
        </div>
        <Button asChild size="sm" variant="outline">
          <Link href="/importar">
            <Upload aria-hidden />
            Importar
          </Link>
        </Button>
      </header>

      <Separator className="my-4" />

      <Card className="divide-y">
        <CardSection>
          <p className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
            <Wallet className="size-3.5" aria-hidden />
            Recebido no mês
          </p>
          <p className="mt-1 text-3xl font-bold tracking-tight text-success">{reais(t.recebidoCentavos)}</p>
          <Progress
            value={total ? Math.round((t.pagos / total) * 100) : 0}
            className="mt-3"
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
            <p className="mt-1 text-xl font-bold">{reais(t.emAbertoCentavos)}</p>
            <p className="text-xs text-muted-foreground">{t.abertos + t.atrasados} alunos</p>
          </CardSection>
          <CardSection>
            <p className="flex items-center gap-1.5 text-xs font-medium text-destructive">
              <TriangleAlert className="size-3.5" aria-hidden />
              Atrasado
            </p>
            <p className="mt-1 text-xl font-bold text-destructive">{reais(t.atrasadoCentavos)}</p>
            <p className="text-xs text-muted-foreground">{t.atrasados} alunos</p>
          </CardSection>
        </div>
      </Card>

      <Tabs value={filtro} onValueChange={(v) => setFiltro(v as Filtro)} className="mt-4">
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
        <ul className="divide-y">
          {itens.map((i) => (
            <li key={i.cobrancaId}>
              <Link
                href={`/cobranca?id=${i.cobrancaId}`}
                className="flex items-center gap-3 px-4 py-3 outline-none active:bg-muted focus-visible:bg-muted"
              >
                <span
                  aria-hidden
                  className="flex size-9 shrink-0 items-center justify-center rounded-full bg-accent text-xs font-bold text-primary"
                >
                  {iniciais(i.aluno)}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold">{i.aluno}</p>
                  <p className="truncate text-xs text-muted-foreground">
                    {i.turma} · vence {dataBr(i.vencimento).slice(0, 5)}
                  </p>
                </div>
                <div className="flex shrink-0 flex-col items-end gap-1">
                  <p className="text-sm font-semibold">{reais(i.valorCentavos)}</p>
                  <StatusBadge status={i.status} />
                </div>
                <ChevronRight className="size-4 shrink-0 text-muted-foreground/60" aria-hidden />
              </Link>
            </li>
          ))}
          {itens.length === 0 && (
            <li className="py-8 text-center text-sm text-muted-foreground">Nenhum aluno nesta situação.</li>
          )}
        </ul>
      </Card>

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
