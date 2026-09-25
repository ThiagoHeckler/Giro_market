package br.com.giro.estoque.application;

import java.time.LocalDate;

/**
 * Comando de entrada de lote. {@code descricao} e {@code ncm} só são exigidos quando o SKU é novo;
 * para produto já cadastrado, são ignorados.
 */
public record EntradaLote(String sku, String descricao, String ncm, String codigoLote, int quantidade,
                          LocalDate validade) {
}
