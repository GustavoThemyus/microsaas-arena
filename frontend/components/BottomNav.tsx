"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, Upload } from "lucide-react";
import { cn } from "@/lib/utils";

const ITENS = [
  { href: "/", rotulo: "Painel", Icone: LayoutDashboard },
  { href: "/importar", rotulo: "Importar", Icone: Upload },
] as const;

/** Barra de navegação fixa no rodapé (padrão de app de celular). A tela de cobrança conta como "Painel". */
export default function BottomNav() {
  const caminho = usePathname();
  const ativo = (href: string) => (href === "/" ? !caminho.startsWith("/importar") : caminho.startsWith(href));

  return (
    <nav
      aria-label="Navegação principal"
      className="fixed inset-x-0 bottom-0 z-40 border-t bg-card/95 pb-[env(safe-area-inset-bottom)] backdrop-blur"
    >
      <ul className="mx-auto grid max-w-md grid-cols-2 divide-x">
        {ITENS.map(({ href, rotulo, Icone }) => (
          <li key={href}>
            <Link
              href={href}
              aria-current={ativo(href) ? "page" : undefined}
              className={cn(
                "flex h-16 flex-col items-center justify-center gap-0.5 text-xs font-medium outline-none focus-visible:bg-muted",
                ativo(href) ? "text-foreground" : "text-muted-foreground",
              )}
            >
              <span
                className={cn(
                  "flex size-8 items-center justify-center rounded-lg transition-colors",
                  ativo(href) && "bg-lime text-lime-foreground",
                )}
              >
                <Icone className="size-5" aria-hidden />
              </span>
              {rotulo}
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}
