import type { Metadata, Viewport } from "next";
import "./globals.css";
import BottomNav from "@/components/BottomNav";
import DemoBanner from "@/components/DemoBanner";

export const metadata: Metadata = {
  title: "Mensalidades — Demo",
  description: "Demonstração de cobrança de mensalidades por Pix para escolinhas esportivas",
  manifest: "/manifest.webmanifest",
  appleWebApp: { capable: true, title: "Mensalidades", statusBarStyle: "default" },
};

export const viewport: Viewport = {
  themeColor: "#0f766e",
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" className="h-full antialiased">
      <body className="min-h-full bg-slate-50 text-slate-900">
        <DemoBanner />
        <main className="mx-auto w-full max-w-md px-4 pb-28 pt-4">{children}</main>
        <BottomNav />
      </body>
    </html>
  );
}
