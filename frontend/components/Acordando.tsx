export default function Acordando({ tentativa }: { tentativa: number }) {
  return (
    <div className="mt-24 text-center text-slate-600">
      <div className="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-4 border-teal-700 border-t-transparent" />
      <p className="font-medium">Acordando o sistema…</p>
      <p className="mt-1 text-sm">
        {tentativa > 3 ? "Demora só na primeira vez do dia (servidor gratuito). Já vai." : "Só um instante."}
      </p>
    </div>
  );
}
