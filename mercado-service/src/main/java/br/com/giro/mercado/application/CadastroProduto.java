package br.com.giro.mercado.application;

import br.com.giro.mercado.domain.ProdutoJaCadastradoException;
import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.infra.estoque.CatalogoEstoque;
import br.com.giro.mercado.infra.estoque.CatalogoEstoque.ProdutoNoEstoque;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * Coloca um produto na vitrine. Entra com a prateleira vazia: o próprio gatilho min/max pede o
 * primeiro lote ao estoque. A classificação atual do estoque é herdada aqui — o ProdutoClassificado
 * que chegou antes do cadastro foi descartado por falta de produto.
 */
@Service
public class CadastroProduto {

    private static final Logger log = LoggerFactory.getLogger(CadastroProduto.class);

    private final ProdutoVitrineRepository produtos;
    private final CatalogoEstoque catalogoEstoque;
    private final GatilhoReposicao gatilho;
    private final TransactionTemplate transacao;
    private final Clock relogio;

    public CadastroProduto(ProdutoVitrineRepository produtos, CatalogoEstoque catalogoEstoque,
                           GatilhoReposicao gatilho, TransactionTemplate transacao, Clock relogio) {
        this.produtos = produtos;
        this.catalogoEstoque = catalogoEstoque;
        this.gatilho = gatilho;
        this.transacao = transacao;
        this.relogio = relogio;
    }

    public ProdutoVitrine cadastrar(NovoProduto novo) {
        if (produtos.existsById(novo.sku())) {
            throw new ProdutoJaCadastradoException(novo.sku());
        }
        // HTTP antes da transação; SKU desconhecido no estoque interrompe aqui.
        var noEstoque = catalogoEstoque.consultar(novo.sku());

        return transacao.execute(_ -> {
            var produto = new ProdutoVitrine(novo.sku(), novo.nome(), novo.preco(), 0,
                    novo.estoqueMinimo(), novo.estoqueIdeal());
            // classificadoEm = agora: um ProdutoClassificado mais antigo que chegue depois não sobrescreve.
            noEstoque.filter(ProdutoNoEstoque::classificado)
                    .ifPresent(e -> produto.classificar(e.tags(), e.categoria(), relogio.instant()));
            produtos.save(produto);
            gatilho.avaliar(produto);
            log.info("Produto cadastrado na vitrine sku={} minimo={} ideal={} classificado={}", produto.getSku(),
                    produto.getEstoqueMinimo(), produto.getEstoqueIdeal(), produto.getCategoria().isPresent());
            return produto;
        });
    }
}
