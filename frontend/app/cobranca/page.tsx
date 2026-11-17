"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import CobrancaView from "@/components/CobrancaView";

// O id vai na query (/cobranca?id=7) em vez de /cobranca/7 para o front ser 100% estático
// (exportado em HTML puro): roda em qualquer hospedagem de arquivos, como o Cloudflare Pages.
function Conteudo() {
  const id = useSearchParams().get("id");
  if (!id) return <p className="mt-16 text-center text-slate-600">Cobrança não informada.</p>;
  return <CobrancaView id={id} />;
}

export default function CobrancaPage() {
  return (
    <Suspense fallback={<p className="mt-16 text-center text-slate-600">Carregando…</p>}>
      <Conteudo />
    </Suspense>
  );
}
