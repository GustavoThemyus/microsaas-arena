import type { Metadata, Viewport } from "next";
import "./globals.css";
import BottomNav from "@/components/BottomNav";
import DemoBanner from "@/components/DemoBanner";
import ThemeToggle from "@/components/ThemeToggle";

export const metadata: Metadata = {
  title: "Mensalidades — Demo",
  description: "Demonstração de cobrança de mensalidades por Pix para escolinhas esportivas",
  manifest: "/manifest.webmanifest",
  appleWebApp: { capable: true, title: "Mensalidades", statusBarStyle: "default" },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f1f1f3" },
    { media: "(prefers-color-scheme: dark)", color: "#0b0b0d" },
  ],
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" className="h-full antialiased" suppressHydrationWarning>
      <head>
        {/* Aplica o tema salvo antes da primeira pintura (evita piscar claro->escuro). */}
        <script
          dangerouslySetInnerHTML={{
            __html: `try{var t=localStorage.getItem("tema");if(t==="dark"||t==="light")document.documentElement.dataset.theme=t}catch(e){}`,
          }}
        />
      </head>
      <body className="min-h-full bg-background text-foreground">
        <DemoBanner />
        <main className="relative mx-auto w-full max-w-md px-4 pb-28 pt-4">
          <div className="absolute right-4 top-4 z-10">
            <ThemeToggle />
          </div>
          {children}
        </main>
        <BottomNav />
      </body>
    </html>
  );
}
