"use client";

import { useSyncExternalStore } from "react";
import { Moon, Sun } from "lucide-react";

type Tema = "light" | "dark";

function lerTema(): Tema {
  const escolhido = document.documentElement.dataset.theme;
  if (escolhido === "dark" || escolhido === "light") return escolhido;
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function assinar(aviso: () => void) {
  const mq = window.matchMedia("(prefers-color-scheme: dark)");
  mq.addEventListener("change", aviso);
  window.addEventListener("tema", aviso);
  return () => {
    mq.removeEventListener("change", aviso);
    window.removeEventListener("tema", aviso);
  };
}

/** Botão de ícone (canto superior direito) que alterna claro/escuro. Sem escolha salva, segue o tema do aparelho. */
export default function ThemeToggle() {
  const tema = useSyncExternalStore(assinar, lerTema, () => "light" as Tema);
  const escuro = tema === "dark";

  function alternar() {
    const novo: Tema = escuro ? "light" : "dark";
    document.documentElement.dataset.theme = novo;
    try {
      localStorage.setItem("tema", novo);
    } catch {
      /* sem armazenamento (aba anônima): vale só para esta visita */
    }
    window.dispatchEvent(new Event("tema"));
  }

  return (
    <button
      type="button"
      onClick={alternar}
      aria-label={escuro ? "Mudar para o tema claro" : "Mudar para o tema escuro"}
      className="flex size-10 items-center justify-center rounded-lg border bg-card text-muted-foreground shadow-xs shadow-black/5 outline-none transition-colors hover:bg-muted hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring/50"
    >
      {escuro ? <Sun className="size-5" aria-hidden /> : <Moon className="size-5" aria-hidden />}
    </button>
  );
}
