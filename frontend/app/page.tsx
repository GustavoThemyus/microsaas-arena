"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import Acordando from "@/components/Acordando";
import { acordarApi, api, capturarTokenDaUrl, dataBr, reais } from "@/lib/api";

type Item = {
  cobrancaId: number;
  aluno: string;
  turma: string;
  valorCentavos: number;
  vencimento: string;
  status: "PAGA" | "ABERTA" | "ATRASADA";
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

const ROTULO = { PAGA: "Pago", ABERTA: "A vencer", ATRASADA: "Atrasado" } as const;
const COR = {
  PAGA: "bg-emerald-100 text-emerald-800",
  ABERTA: "bg-slate-200 text-slate-700",
  ATRASADA: "bg-red-100 text-red-800",
} as const;
const FILTROS = ["TODOS", "ATRASADA", "ABERTA", "PAGA"] as const;

export default function PainelPage() {
  const [painel, setPainel] = useState<Painel | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [tentativa, setTentativa] = useState(0);
  const [filtro, setFiltro] = useState<(typeof FILTROS)[number]>("TODOS");

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

  if (erro && !painel) return <p className="mt-16 text-center text-red-700">{erro}</p>;
  if (!painel) return <Acordando tentativa={tentativa} />;

  const t = painel.totais;
  const itens = painel.itens.filter((i) => filtro === "TODOS" || i.status === filtro);
  const [ano, mes] = painel.mes.split("-");

  return (
    <>
      <header className="mb-4 flex items-end justify-between">
        <div>
          <h1 className="text-lg font-bold">Escolinha Modelo</h1>
          <p className="text-sm text-slate-500">
            Mensalidades de {mes}/{ano}
          </p>
        </div>
        <Link
          href="/importar"
          className="rounded-lg bg-teal-700 px-3 py-2 text-sm font-semibold text-white active:bg-teal-800"
        >
          Importar CSV
        </Link>
      </header>

      <section className="mb-4 grid grid-cols-2 gap-2">
        <Cartao titulo="Recebido no mês" valor={reais(t.recebidoCentavos)} detalhe={`${t.pagos} pagos`} cor="text-emerald-700" />
        <Cartao titulo="Em aberto" valor={reais(t.emAbertoCentavos)} detalhe={`${t.abertos + t.atrasados} alunos`} cor="text-slate-900" />
        <Cartao titulo="Atrasado" valor={reais(t.atrasadoCentavos)} detalhe={`${t.atrasados} alunos`} cor="text-red-700" className="col-span-2" />
      </section>

      <nav className="mb-3 flex gap-2 overflow-x-auto" aria-label="Filtrar por situação">
        {FILTROS.map((f) => (
          <button
            key={f}
            onClick={() => setFiltro(f)}
            className={`whitespace-nowrap rounded-full px-3 py-1.5 text-sm font-medium ${
              filtro === f ? "bg-slate-900 text-white" : "bg-white text-slate-700 ring-1 ring-slate-200"
            }`}
          >
            {f === "TODOS" ? "Todos" : ROTULO[f]}
          </button>
        ))}
      </nav>

      <ul className="space-y-2">
        {itens.map((i) => (
          <li key={i.cobrancaId} className="flex items-center justify-between rounded-xl bg-white px-3 py-3 shadow-sm ring-1 ring-slate-100">
            <div className="min-w-0">
              <p className="truncate font-medium">{i.aluno}</p>
              <p className="truncate text-xs text-slate-500">
                {i.turma} · vence {dataBr(i.vencimento)}
              </p>
            </div>
            <div className="ml-3 shrink-0 text-right">
              <p className="text-sm font-semibold">{reais(i.valorCentavos)}</p>
              <span className={`mt-0.5 inline-block rounded-full px-2 py-0.5 text-xs font-semibold ${COR[i.status]}`}>
                {ROTULO[i.status]}
              </span>
            </div>
          </li>
        ))}
        {itens.length === 0 && <li className="py-8 text-center text-sm text-slate-500">Nenhum aluno nesta situação.</li>}
      </ul>

      <footer className="mt-8 text-center">
        <button
          onClick={async () => {
            if (confirm("Restaurar os dados originais da demonstração?")) {
              await api("/api/demo/resetar", { method: "POST" });
              await carregar();
            }
          }}
          className="text-xs text-slate-400 underline"
        >
          Restaurar dados da demo
        </button>
      </footer>
    </>
  );
}

function Cartao(props: { titulo: string; valor: string; detalhe: string; cor: string; className?: string }) {
  return (
    <div className={`rounded-xl bg-white p-3 shadow-sm ring-1 ring-slate-100 ${props.className ?? ""}`}>
      <p className="text-xs text-slate-500">{props.titulo}</p>
      <p className={`text-xl font-bold ${props.cor}`}>{props.valor}</p>
      <p className="text-xs text-slate-500">{props.detalhe}</p>
    </div>
  );
}
