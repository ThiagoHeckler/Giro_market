package br.com.giro.mercado.web;

import br.com.giro.mercado.application.ItemCheckout;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(@NotEmpty @Size(max = 50) List<@Valid ItemCheckout> itens) {
}
