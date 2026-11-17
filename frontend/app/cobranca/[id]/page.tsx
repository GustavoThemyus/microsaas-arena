import { Suspense } from "react";
import CobrancaView from "@/components/CobrancaView";

// O Next 16 exige Suspense ao ler os parâmetros da rota dinâmica.
async function Conteudo({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <CobrancaView id={id} />;
}

export default function CobrancaPage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <Suspense fallback={<p className="mt-16 text-center text-slate-600">Carregando…</p>}>
      <Conteudo params={params} />
    </Suspense>
  );
}
