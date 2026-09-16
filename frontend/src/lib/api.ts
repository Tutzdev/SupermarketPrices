import type { ApiProblem } from "@/types/api";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "") ??
  "http://localhost:8080/api/v1";

const TOKEN_KEY = "gomo.session";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly problem: ApiProblem,
  ) {
    super(problem.detail ?? "Não foi possível concluir a solicitação.");
  }
}

export const sessionStorageService = {
  read: () => window.localStorage.getItem(TOKEN_KEY),
  write: (token: string) => window.localStorage.setItem(TOKEN_KEY, token),
  clear: () => window.localStorage.removeItem(TOKEN_KEY),
};

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  authenticated?: boolean;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");

  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  if (options.authenticated !== false) {
    const token = sessionStorageService.read();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch {
    throw new ApiError(0, {
      title: "Falha de rede",
      detail: "Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.",
    });
  }

  if (response.status === 401 && options.authenticated !== false) {
    sessionStorageService.clear();
    window.dispatchEvent(new Event("gomo:session-expired"));
  }

  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as ApiProblem;
    throw new ApiError(response.status, problem);
  }

  if (response.status === 204) return undefined as T;

  return (await response.json()) as T;
}

export function queryString(values: Record<string, string | number | undefined | null>) {
  const params = new URLSearchParams();

  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, String(value));
    }
  });

  return params.toString();
}
