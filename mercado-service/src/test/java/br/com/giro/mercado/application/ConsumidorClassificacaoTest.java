package br.com.giro.mercado.application;

import br.com.giro.mercado.IntegracaoTest;
import br.com.giro.mercado.application.contrato.ProdutoClassificado;
import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumidorClassificacaoTest extends IntegracaoTest {

    private static final String COCA = "7894900011517";
    private static final Instant AGORA = Instant.parse("2026-09-25T12:00:00Z");

    @Autowired
    ConsumidorClassificacao consumidor;

    @Autowired
    ProdutoVitrineRepository produtos;

    private static ProdutoClassificado classificado(List<String> tags, String categoria, Instant em) {
        return new ProdutoClassificado(UUID.randomUUID(), COCA, tags, categoria, em);
    }

    private ProdutoVitrine produto() {
        return produtos.findById(COCA).orElseThrow();
    }

    private void cadastra() {
        produtos.save(new ProdutoVitrine(COCA, "COCA COLA 2L PET", new BigDecimal("9.99"), 10, 5, 20));
    }

    @Test
    void aplicaTagsECategoriaNaVitrine() {
        cadastra();

        consumidor.processar(classificado(List.of("refrigerante", "coca cola", "2l"), "bebidas", AGORA));

        assertThat(produto().getTags()).contains(List.of("refrigerante", "coca cola", "2l"));
        assertThat(produto().getCategoria()).contains("bebidas");
    }

    @Test
    void eventoAtrasadoNaoSobrescreveClassificacaoMaisNova() {
        cadastra();

        consumidor.processar(classificado(List.of("refrigerante"), "bebidas", AGORA));
        consumidor.processar(classificado(List.of("antiga"), "outros", AGORA.minusSeconds(300)));

        assertThat(produto().getCategoria()).contains("bebidas");
    }

    @Test
    void duplicataEhDescartadaPelaInbox() {
        cadastra();
        var evento = classificado(List.of("refrigerante"), "bebidas", AGORA);

        consumidor.processar(evento);
        consumidor.processar(evento);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM inbox_event", Integer.class)).isOne();
    }

    @Test
    void skuForaDaVitrineEhIgnoradoSemErro() {
        consumidor.processar(classificado(List.of("refrigerante"), "bebidas", AGORA));

        assertThat(produtos.count()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inbox_event", Integer.class)).isOne();
    }
}
