import { FlaskConical } from "lucide-react";

export default function DemoBanner() {
  return (
    <div
      role="note"
      className="sticky top-0 z-50 flex items-center justify-center gap-1.5 border-b border-amber-500/40 bg-amber-300 px-3 py-1.5 text-[11px] font-bold uppercase tracking-wide text-amber-950"
    >
      <FlaskConical className="size-3.5 shrink-0" aria-hidden />
      Ambiente de demonstração — dados fictícios
    </div>
  );
}
