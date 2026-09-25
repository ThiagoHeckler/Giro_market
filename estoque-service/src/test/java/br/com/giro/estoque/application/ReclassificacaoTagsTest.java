package br.com.giro.estoque.application;

import br.com.giro.estoque.IntegracaoTest;
import br.com.giro.estoque.WorkerFalso;
import br.com.giro.estoque.domain.ProdutoEstoque;
import br.com.giro.estoque.infra.persistencia.ProdutoEstoqueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ReclassificacaoTagsTest extends IntegracaoTest {

    @Autowired
    ReclassificacaoTags reclassificacao;

    @Autowired
    ProdutoEstoqueRepository produtos;

    @Test
    void produtoQueFicouSemTagsEhClassificadoQuandoOWorkerVolta() {
        produtos.save(new ProdutoEstoque("7894900011517", "COCA COLA 2L PET", "22021000"));

        assertThat(reclassificacao.reclassificarPendentes()).isOne();

        assertThat(produtos.findById("7894900011517").orElseThrow().getCategoria()).contains("bebidas");
        assertThat(reclassificacao.reclassificarPendentes()).isZero();   // nada mais pendente
    }

    @Test
    void comWorkerForaParaNoPrimeiroErroSemMartelarOsDemais() {
        produtos.save(new ProdutoEstoque("7894900011517", "COCA COLA 2L PET", "22021000"));
        produtos.save(new ProdutoEstoque("7891000100103", "LEITE INTEGRAL 1L", "04012010"));
        WorkerFalso.responderCom(503, "{}");

        assertThat(reclassificacao.reclassificarPendentes()).isZero();

        assertThat(WorkerFalso.chamadas()).isOne();
    }
}
