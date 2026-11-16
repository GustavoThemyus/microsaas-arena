"use client";

import Link from "next/link";
import { useRef, useState } from "react";
import { api, capturarTokenDaUrl } from "@/lib/api";

type Resultado = { criados: number; atualizados: number; erros: { linha: number; motivo: string }[] };

export default function ImportarPage() {
  const input = useRef<HTMLInputElement>(null);
  const [enviando, setEnviando] = useState(false);
  const [resultado, setResultado] = useState<Resultado | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  async function enviar() {
    const arquivo = input.current?.files?.[0];
    if (!arquivo) return setErro("Escolha um arquivo CSV primeiro.");
    capturarTokenDaUrl();
    setEnviando(true);
    setErro(null);
    setResultado(null);
    try {
      const corpo = new FormData();
      corpo.append("arquivo", arquivo);
      setResultado(await api<Resultado>("/api/importacoes", { method: "POST", body: corpo }));
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <>
      <Link href="/" className="text-sm text-teal-700 underline">
        ← Voltar ao painel
      </Link>
      <h1 className="mb-1 mt-3 text-lg font-bold">Importar planilha de alunos</h1>
      <p className="mb-4 text-sm text-slate-600">
        Arquivo CSV com as colunas <b>nome, turma, valor, vencimento</b>. Vencimento pode ser o dia (10) ou a data
        (10/11/2026). Aceita planilha exportada do Excel ou do Google Planilhas.
      </p>

      <a href="/alunos-exemplo.csv" download className="mb-4 inline-block text-sm text-teal-700 underline">
        Baixar planilha de exemplo
      </a>

      <input
        ref={input}
        type="file"
        accept=".csv,text/csv"
        className="mb-3 block w-full text-sm file:mr-3 file:rounded-lg file:border-0 file:bg-slate-200 file:px-3 file:py-2"
      />
      <button
        onClick={enviar}
        disabled={enviando}
        className="w-full rounded-xl bg-teal-700 py-3 font-semibold text-white disabled:opacity-50"
      >
        {enviando ? "Importando…" : "Importar"}
      </button>

      {erro && <p className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">{erro}</p>}

      {resultado && (
        <section className="mt-4 rounded-xl bg-white p-4 shadow-sm ring-1 ring-slate-100">
          <p className="font-semibold text-emerald-700">
            {resultado.criados} aluno(s) novo(s) · {resultado.atualizados} atualizado(s)
          </p>
          {resultado.erros.length > 0 && (
            <>
              <p className="mt-3 text-sm font-semibold text-red-700">{resultado.erros.length} linha(s) com problema:</p>
              <ul className="mt-1 space-y-1 text-sm text-slate-700">
                {resultado.erros.map((e) => (
                  <li key={e.linha}>
                    Linha {e.linha}: {e.motivo}
                  </li>
                ))}
              </ul>
            </>
          )}
          <Link href="/" className="mt-4 block rounded-lg bg-slate-900 py-2 text-center text-sm font-semibold text-white">
            Ver painel
          </Link>
        </section>
      )}
    </>
  );
}
