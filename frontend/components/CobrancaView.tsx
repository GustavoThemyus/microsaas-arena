"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { api, capturarTokenDaUrl, dataBr, reais } from "@/lib/api";

type Detalhe = {
  cobrancaId: number;
  aluno: string;
  turma: string;
  valorCentavos: number;
  vencimento: string;
  status: "PAGA" | "ABERTA" | "ATRASADA";
  pagoEm: string | null;
  pixCopiaECola: string | null;
  pixQrBase64: string | null;
  linkFatura: string | null;
};

type Zap = { telefone: string; texto: string; url: string };

const ROTULO = { PAGA: "Pago", ABERTA: "A vencer", ATRASADA: "Atrasado" } as const;
const COR = {
  PAGA: "bg-emerald-100 text-emerald-800",
  ABERTA: "bg-slate-200 text-slate-700",
  ATRASADA: "bg-red-100 text-red-800",
} as const;

export default function CobrancaView({ id }: { id: string }) {
  const [d, setD] = useState<Detalhe | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState<"pix" | "simular" | null>(null);
  const [copiado, setCopiado] = useState(false);
  const [zap, setZap] = useState<Zap | null>(null);

  const carregar = useCallback(async () => {
    try {
      setD(await api<Detalhe>(`/api/cobrancas/${id}`));
    } catch (e) {
      setErro((e as Error).message);
    }
  }, [id]);

  useEffect(() => {
    capturarTokenDaUrl();
    let ativo = true;
    api<Detalhe>(`/api/cobrancas/${id}`)
      .then((r) => ativo && setD(r))
      .catch((e: Error) => ativo && setErro(e.message));
    return () => {
      ativo = false;
    };
  }, [id]);

  // Com Pix gerado e ainda não pago, confere a cada 3 s: a baixa chega pelo webhook do Asaas.
  const aguardando = d !== null && d.status !== "PAGA" && d.pixCopiaECola !== null;
  useEffect(() => {
    if (!aguardando) return;
    const t = setInterval(carregar, 3000);
    return () => clearInterval(t);
  }, [aguardando, carregar]);

  // O link do WhatsApp é buscado antes do clique e renderizado como <a>: abrir um link vindo de um clique direto
  // não é bloqueado pelo navegador do celular (abrir janela depois de um fetch pode ser).
  const pendente = d !== null && d.status !== "PAGA";
  const temPix = d?.pixCopiaECola != null;
  useEffect(() => {
    if (!pendente) return;
    let ativo = true;
    api<Zap>(`/api/cobrancas/${id}/whatsapp`)
      .then((z) => ativo && setZap(z))
      .catch(() => ativo && setZap(null));
    return () => {
      ativo = false;
    };
  }, [id, pendente, temPix]);

  async function acao(tipo: "pix" | "simular") {
    setOcupado(tipo);
    setErro(null);
    try {
      if (tipo === "pix") {
        setD(await api<Detalhe>(`/api/cobrancas/${id}/pix`, { method: "POST" }));
      } else {
        await api(`/api/demo/cobrancas/${id}/simular-pagamento`, { method: "POST" });
        await carregar();
      }
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setOcupado(null);
    }
  }

  async function copiar() {
    if (!d?.pixCopiaECola) return;
    try {
      await navigator.clipboard.writeText(d.pixCopiaECola);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2000);
    } catch {
      /* sem permissão de área de transferência: o texto continua selecionável */
    }
  }

  if (!d) return <p className="mt-16 text-center text-slate-600">{erro ?? "Carregando…"}</p>;

  return (
    <>
      <Link href="/" className="text-sm text-teal-700 underline">
        ← Voltar ao painel
      </Link>

      <section className="mt-3 rounded-xl bg-white p-4 shadow-sm ring-1 ring-slate-100">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-lg font-bold">{d.aluno}</h1>
            <p className="text-sm text-slate-500">{d.turma}</p>
          </div>
          <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${COR[d.status]}`}>{ROTULO[d.status]}</span>
        </div>
        <p className="mt-3 text-3xl font-bold">{reais(d.valorCentavos)}</p>
        <p className="text-sm text-slate-500">Vencimento {dataBr(d.vencimento)}</p>
      </section>

      {d.status === "PAGA" && (
        <section className="mt-4 rounded-xl bg-emerald-50 p-4 text-center ring-1 ring-emerald-200">
          <p className="text-2xl">✅</p>
          <p className="font-bold text-emerald-800">Pagamento confirmado!</p>
          <p className="text-sm text-emerald-700">Baixa feita automaticamente.</p>
        </section>
      )}

      {zap && d.status !== "PAGA" && (
        <a
          href={zap.url}
          target="_blank"
          rel="noreferrer"
          className={`mt-4 block w-full rounded-xl py-3 text-center font-semibold ${
            d.status === "ATRASADA" ? "bg-emerald-600 text-white" : "bg-white text-emerald-700 ring-1 ring-emerald-600"
          }`}
        >
          💬 Lembrar no WhatsApp
        </a>
      )}

      {d.status !== "PAGA" && !d.pixCopiaECola && (
        <button
          onClick={() => acao("pix")}
          disabled={ocupado !== null}
          className="mt-4 w-full rounded-xl bg-teal-700 py-3 font-semibold text-white disabled:opacity-50"
        >
          {ocupado === "pix" ? "Gerando Pix…" : "Gerar cobrança Pix"}
        </button>
      )}

      {d.status !== "PAGA" && d.pixCopiaECola && (
        <section className="mt-4 rounded-xl bg-white p-4 text-center shadow-sm ring-1 ring-slate-100">
          {d.pixQrBase64 && (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={`data:image/png;base64,${d.pixQrBase64}`}
              alt="QR code Pix"
              className="mx-auto h-56 w-56"
            />
          )}
          <p className="mt-2 text-xs text-slate-500">Aguardando pagamento…</p>
          <textarea
            readOnly
            value={d.pixCopiaECola}
            rows={3}
            className="mt-3 w-full rounded-lg bg-slate-50 p-2 text-xs text-slate-700"
            onFocus={(e) => e.currentTarget.select()}
          />
          <div className="mt-2 grid grid-cols-2 gap-2">
            <button onClick={copiar} className="rounded-lg bg-slate-900 py-2.5 text-sm font-semibold text-white">
              {copiado ? "Copiado!" : "Copiar Pix"}
            </button>
            {d.linkFatura && (
              <a
                href={d.linkFatura}
                target="_blank"
                rel="noreferrer"
                className="rounded-lg bg-white py-2.5 text-sm font-semibold text-slate-900 ring-1 ring-slate-300"
              >
                Abrir link
              </a>
            )}
          </div>

          <div className="mt-5 border-t border-dashed border-amber-400 pt-4">
            <p className="mb-2 text-xs font-bold uppercase tracking-wide text-amber-700">Exclusivo da demonstração</p>
            <button
              onClick={() => acao("simular")}
              disabled={ocupado !== null}
              className="w-full rounded-lg border-2 border-dashed border-amber-500 bg-amber-50 py-2.5 text-sm font-semibold text-amber-900 disabled:opacity-50"
            >
              {ocupado === "simular" ? "Simulando…" : "Simular pagamento (só demo)"}
            </button>
            <p className="mt-1 text-xs text-slate-500">Na vida real, o aluno paga no app do banco.</p>
          </div>
        </section>
      )}

      {erro && <p className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">{erro}</p>}
    </>
  );
}
