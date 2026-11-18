import { LoaderCircle, Server } from "lucide-react";
import { Card, CardSection } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";

export default function Acordando({ tentativa }: { tentativa: number }) {
  return (
    <Card className="mt-10">
      <CardSection className="flex flex-col items-center gap-2 py-8 text-center">
        <div className="flex size-12 items-center justify-center rounded-full bg-accent text-primary">
          <Server className="size-6" aria-hidden />
        </div>
        <p className="flex items-center gap-2 font-semibold">
          <LoaderCircle className="size-4 animate-spin text-primary" aria-hidden />
          Acordando o sistema…
        </p>
        <p className="max-w-64 text-sm text-muted-foreground">
          {tentativa > 3 ? "Demora só na primeira vez do dia (servidor gratuito). Já vai." : "Só um instante."}
        </p>
      </CardSection>
      <Separator />
      <CardSection className="space-y-2">
        <Skeleton className="h-4 w-2/3" />
        <Skeleton className="h-4 w-1/2" />
      </CardSection>
    </Card>
  );
}
