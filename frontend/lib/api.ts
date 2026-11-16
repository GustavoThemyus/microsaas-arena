// Cliente da API. O token da demo vem do link (?k=...) e fica no localStorage.
export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const CHAVE_TOKEN = "demo-token";

export function capturarTokenDaUrl(): void {
  try {
    const url = new URL(window.location.href);
    const k = url.searchParams.get("k");
    if (k) {
      localStorage.setItem(CHAVE_TOKEN, k);
      url.searchParams.delete("k");
      window.history.replaceState({}, "", url.pathname + url.search);
    }
  } catch {
    // localStorage pode estar bloqueado (aba anônima): a API local sem token continua funcionando.
  }
}

function token(): string {
  try {
    return localStorage.getItem(CHAVE_TOKEN) ?? "";
  } catch {
    return "";
  }
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

export async function api<T>(caminho: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const t = token();
  if (t) headers.set("X-Demo-Token", t);
  const resp = await fetch(`${API_URL}${caminho}`, { ...init, headers });
  if (!resp.ok) {
    let msg = `Erro ${resp.status}`;
    try {
      const corpo = await resp.json();
      msg = corpo.erro ?? corpo.message ?? msg;
    } catch {
      /* corpo vazio */
    }
    throw new ApiError(resp.status, resp.status === 401 ? "Link de demo inválido ou expirado" : msg);
  }
  return resp.json() as Promise<T>;
}

/**
 * Acorda a API gratuita (que "dorme" após 15 min sem uso). Tenta o /api/health até responder,
 * avisando a tela por callback. Evita mostrar erro ao dono da escolinha na partida a frio.
 */
export async function acordarApi(aoTentar: (tentativa: number) => void, maxTentativas = 30): Promise<void> {
  for (let i = 1; i <= maxTentativas; i++) {
    try {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), 8000);
      const r = await fetch(`${API_URL}/api/health`, { signal: ctrl.signal });
      clearTimeout(timer);
      if (r.ok) return;
    } catch {
      /* ainda dormindo */
    }
    aoTentar(i);
    await new Promise((res) => setTimeout(res, 2000));
  }
  throw new Error("Servidor não respondeu. Tente novamente em instantes.");
}

export const reais = (centavos: number) =>
  (centavos / 100).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export const dataBr = (iso: string) => iso.split("-").reverse().join("/");
