import type { Situacao } from "@/components/StatusBadge";

export type Item = {
  cobrancaId: number;
  aluno: string;
  turma: string;
  valorCentavos: number;
  vencimento: string;
  status: Situacao;
};
