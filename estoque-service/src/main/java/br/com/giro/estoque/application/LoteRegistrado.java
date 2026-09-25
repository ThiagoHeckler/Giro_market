package br.com.giro.estoque.application;

public record LoteRegistrado(Long loteId, String sku, int saldoDisponivel, boolean classificado,
                             int demandasAtendidas) {
}
