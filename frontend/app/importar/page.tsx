"use client";

import Link from "next/link";
import { useRef, useState } from "react";
import { ArrowLeft, CircleCheck, Download, FileSpreadsheet, LoaderCircle, TriangleAlert, Upload } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardSection } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
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
      <Button asChild variant="ghost" size="sm" className="-ml-3">
        <Link href="/">
          <ArrowLeft aria-hidden />
          Voltar ao painel
        </Link>
      </Button>

      <div className="mb-4 mt-2 flex items-center gap-3">
        <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-accent text-primary">
          <FileSpreadsheet className="size-6" aria-hidden />
        </div>
        <div>
          <h1 className="text-lg font-bold leading-tight">Importar alunos</h1>
          <p className="text-sm text-muted-foreground">Planilha CSV do Excel ou Google Planilhas</p>
        </div>
      </div>

      <Card className="divide-y">
        <CardSection className="space-y-2 text-sm text-muted-foreground">
          <p>
            Colunas: <b className="text-foreground">nome, turma, valor, vencimento</b>.
          </p>
          <p>Vencimento pode ser só o dia (10) ou a data completa (10/11/2026).</p>
          <Button asChild variant="outline" size="sm" className="mt-1">
            <a href="/alunos-exemplo.csv" download>
              <Download aria-hidden />
              Baixar planilha de exemplo
            </a>
          </Button>
        </CardSection>
        <CardSection className="space-y-3">
          <Input ref={input} type="file" accept=".csv,text/csv" aria-label="Arquivo CSV" />
          <Button onClick={enviar} disabled={enviando} size="lg" className="w-full">
            {enviando ? <LoaderCircle className="animate-spin" aria-hidden /> : <Upload aria-hidden />}
            {enviando ? "Importando…" : "Importar"}
          </Button>
        </CardSection>
      </Card>

      {erro && (
        <Alert variant="destructive" className="mt-4">
          <TriangleAlert aria-hidden />
          <AlertDescription>{erro}</AlertDescription>
        </Alert>
      )}

      {resultado && (
        <Card className="mt-4">
          <CardSection>
            <Alert variant="success">
              <CircleCheck aria-hidden />
              <AlertTitle>Importação concluída</AlertTitle>
              <AlertDescription>
                {resultado.criados} aluno(s) novo(s) · {resultado.atualizados} atualizado(s)
              </AlertDescription>
            </Alert>
          </CardSection>
          {resultado.erros.length > 0 && (
            <>
              <Separator />
              <CardSection className="pb-2">
                <p className="flex items-center gap-1.5 text-sm font-semibold text-destructive">
                  <TriangleAlert className="size-4" aria-hidden />
                  {resultado.erros.length} linha(s) com problema
                </p>
              </CardSection>
              <ul className="divide-y border-t">
                {resultado.erros.map((e) => (
                  <li key={e.linha} className="flex gap-3 px-4 py-2.5 text-sm">
                    <span className="w-16 shrink-0 font-semibold text-muted-foreground">Linha {e.linha}</span>
                    <span>{e.motivo}</span>
                  </li>
                ))}
              </ul>
            </>
          )}
          <Separator />
          <CardSection>
            <Button asChild variant="secondary" className="w-full">
              <Link href="/">Ver painel</Link>
            </Button>
          </CardSection>
        </Card>
      )}
    </>
  );
}
