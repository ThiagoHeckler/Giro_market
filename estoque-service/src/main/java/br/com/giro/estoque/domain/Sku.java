package br.com.giro.estoque.domain;

import java.util.regex.Pattern;

/**
 * Regras do SKU (EAN/GTIN), a identidade canônica compartilhada com o mercado.
 * Aceita GTIN-8, GTIN-12, GTIN-13 e GTIN-14 — só dígitos.
 */
public final class Sku {

    public static final String REGEX = "^([0-9]{8}|[0-9]{12,14})$";

    private static final Pattern PADRAO = Pattern.compile(REGEX);

    private Sku() {
    }

    public static String validar(String sku) {
        if (sku == null || !PADRAO.matcher(sku).matches()) {
            throw new IllegalArgumentException("SKU inválido (esperado GTIN-8/12/13/14): " + sku);
        }
        return sku;
    }
}
