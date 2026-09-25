package br.com.giro.mercado.domain;

/** A prateleira não tem unidades suficientes para o checkout. */
public class EstoqueInsuficienteException extends RuntimeException {

    private final String sku;

    public EstoqueInsuficienteException(String sku, int solicitado, int disponivel) {
        super("Estoque insuficiente para o SKU %s: solicitado %d, disponível %d".formatted(sku, solicitado, disponivel));
        this.sku = sku;
    }

    public String getSku() {
        return sku;
    }
}
