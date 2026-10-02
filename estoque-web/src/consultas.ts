import { MutationCache, QueryCache, QueryClient, type DefaultOptions } from "@tanstack/react-query";
import { naoAutenticado } from "./api/estoque";

export const CHAVE_SESSAO = ["sessao"] as const;

/**
 * Cliente de consultas do painel. Qualquer 401 (sessão expirada no meio do uso) zera a sessão em
 * cache, e o app troca o painel pela tela de login.
 */
export function criarClienteConsultas(padroes?: DefaultOptions): QueryClient {
  const sessaoExpirou = (erro: unknown) => {
    if (naoAutenticado(erro)) cliente.setQueryData(CHAVE_SESSAO, null);
  };
  const cliente = new QueryClient({
    defaultOptions: padroes,
    queryCache: new QueryCache({ onError: sessaoExpirou }),
    mutationCache: new MutationCache({ onError: sessaoExpirou }),
  });
  return cliente;
}
