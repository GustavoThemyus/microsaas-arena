"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import {
  ArrowLeft,
  BadgeDollarSign,
  CalendarDays,
  Check,
  CircleCheck,
  Copy,
  ExternalLink,
  FlaskConical,
  LoaderCircle,
  MessageCircle,
  QrCode,
  TriangleAlert,
  Users,
} from "lucide-react";
import StatusBadge, { type Situacao } from "@/components/StatusBadge";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardSection } from "@/components/ui/card";
import { api, capturarTokenDaUrl, dataBr, reais } from "@/lib/api";

type Detalhe = {
  cobrancaId: number;
  aluno: string;
  turma: string;
  valorCentavos: number;
  vencimento: string;
  status: Situacao;
  pagoEm: string | null;
  pixCopiaECola: string | null;
  pixQrBase64: string | null;
  linkFatura: string | null;
};

type Zap = { telefone: string; texto: string; url: string };

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

  if (!d) return <p className="mt-16 text-center text-muted-foreground">{erro ?? "Carregando…"}</p>;

  return (
    <>
      <Button asChild variant="ghost" size="sm" className="-ml-3">
        <Link href="/">
          <ArrowLeft aria-hidden />
          Voltar ao painel
        </Link>
      </Button>

      <Card className="mt-2 divide-y">
        <CardSection className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h1 className="truncate text-lg font-bold leading-tight">{d.aluno}</h1>
            <p className="mt-0.5 text-sm text-muted-foreground">Mensalidade</p>
          </div>
          <StatusBadge status={d.status} />
        </CardSection>
        <CardSection>
          <p className="text-3xl font-bold tracking-tight">{reais(d.valorCentavos)}</p>
        </CardSection>
        <dl className="divide-y text-sm">
          <div className="flex items-center justify-between px-4 py-3">
            <dt className="flex items-center gap-2 text-muted-foreground">
              <Users className="size-4" aria-hidden />
              Turma
            </dt>
            <dd className="font-medium">{d.turma}</dd>
          </div>
          <div className="flex items-center justify-between px-4 py-3">
            <dt className="flex items-center gap-2 text-muted-foreground">
              <CalendarDays className="size-4" aria-hidden />
              Vencimento
            </dt>
            <dd className="font-medium">{dataBr(d.vencimento)}</dd>
          </div>
        </dl>
      </Card>

      {d.status === "PAGA" && (
        <Alert variant="success" className="mt-4">
          <CircleCheck aria-hidden />
          <AlertTitle>Pagamento confirmado!</AlertTitle>
          <AlertDescription>Baixa feita automaticamente.</AlertDescription>
        </Alert>
      )}

      {d.status !== "PAGA" && (
        <div className="mt-4 space-y-3">
          {zap && (
            <Button asChild variant={d.status === "ATRASADA" ? "success" : "outline"} size="lg" className="w-full">
              <a href={zap.url} target="_blank" rel="noreferrer">
                <MessageCircle aria-hidden />
                Lembrar no WhatsApp
              </a>
            </Button>
          )}
          {!d.pixCopiaECola && (
            <Button onClick={() => acao("pix")} disabled={ocupado !== null} size="lg" className="w-full">
              {ocupado === "pix" ? <LoaderCircle className="animate-spin" aria-hidden /> : <QrCode aria-hidden />}
              {ocupado === "pix" ? "Gerando Pix…" : "Gerar cobrança Pix"}
            </Button>
          )}
        </div>
      )}

      {d.status !== "PAGA" && d.pixCopiaECola && (
        <Card className="mt-4 divide-y">
          <CardSection className="flex flex-col items-center">
            {d.pixQrBase64 && (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={`data:image/png;base64,${d.pixQrBase64}`}
                alt="QR code Pix"
                className="size-56 rounded-lg border bg-white p-2"
              />
            )}
            <p className="mt-3 flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
              <LoaderCircle className="size-3.5 animate-spin" aria-hidden />
              Aguardando pagamento…
            </p>
          </CardSection>
          <CardSection className="space-y-3">
            <textarea
              readOnly
              value={d.pixCopiaECola}
              rows={3}
              aria-label="Pix copia e cola"
              className="w-full resize-none rounded-lg border border-input bg-muted/50 p-2.5 text-xs text-muted-foreground outline-none focus-visible:ring-[3px] focus-visible:ring-ring/30"
              onFocus={(e) => e.currentTarget.select()}
            />
            <div className="grid grid-cols-2 gap-2">
              <Button onClick={copiar} variant="secondary">
                {copiado ? <Check aria-hidden /> : <Copy aria-hidden />}
                {copiado ? "Copiado!" : "Copiar Pix"}
              </Button>
              {d.linkFatura && (
                <Button asChild variant="outline">
                  <a href={d.linkFatura} target="_blank" rel="noreferrer">
                    <ExternalLink aria-hidden />
                    Abrir link
                  </a>
                </Button>
              )}
            </div>
          </CardSection>
          <CardSection className="bg-warning-soft/50">
            <p className="mb-2 flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-warning">
              <FlaskConical className="size-3.5" aria-hidden />
              Exclusivo da demonstração
            </p>
            <Button onClick={() => acao("simular")} disabled={ocupado !== null} variant="warning" className="w-full">
              {ocupado === "simular" ? (
                <LoaderCircle className="animate-spin" aria-hidden />
              ) : (
                <BadgeDollarSign aria-hidden />
              )}
              {ocupado === "simular" ? "Simulando…" : "Simular pagamento (só demo)"}
            </Button>
            <p className="mt-2 text-xs text-muted-foreground">Na vida real, o aluno paga no app do banco.</p>
          </CardSection>
        </Card>
      )}

      {erro && (
        <Alert variant="destructive" className="mt-4">
          <TriangleAlert aria-hidden />
          <AlertDescription>{erro}</AlertDescription>
        </Alert>
      )}
    </>
  );
}
