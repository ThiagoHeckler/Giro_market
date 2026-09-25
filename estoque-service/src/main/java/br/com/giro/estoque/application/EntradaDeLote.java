package br.com.giro.estoque.application;

import br.com.giro.estoque.domain.ProdutoEstoque;
import br.com.giro.estoque.infra.persistencia.DemandaReprimidaRepository;
import br.com.giro.estoque.infra.persistencia.LoteRepository;
import br.com.giro.estoque.infra.persistencia.ProdutoEstoqueRepository;
import br.com.giro.estoque.infra.tags.ClassificadorTags;
import br.com.giro.estoque.infra.tags.ClassificadorTags.Classificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Optional;

/**
 * Entrada de lote no almoxarifado. Classifica tags quando o produto ainda não tem (fora da transação,
 * com timeout curto — worker fora do ar não impede a entrada) e atende a demanda reprimida do SKU
 * na mesma transação que credita o saldo.
 */
@Service
public class EntradaDeLote {

    private static final Logger log = LoggerFactory.getLogger(EntradaDeLote.class);

    private final ProdutoEstoqueRepository produtos;
    private final LoteRepository lotes;
    private final DemandaReprimidaRepository demandas;
    private final Expedicao expedicao;
    private final ClassificadorTags classificador;
    private final TransactionTemplate transacao;
    private final Clock relogio;

    public EntradaDeLote(ProdutoEstoqueRepository produtos, LoteRepository lotes,
                         DemandaReprimidaRepository demandas, Expedicao expedicao,
                         ClassificadorTags classificador, TransactionTemplate transacao, Clock relogio) {
        this.produtos = produtos;
        this.lotes = lotes;
        this.demandas = demandas;
        this.expedicao = expedicao;
        this.classificador = classificador;
        this.transacao = transacao;
        this.relogio = relogio;
    }

    public LoteRegistrado registrar(EntradaLote entrada) {
        var existente = produtos.findById(entrada.sku());
        if (existente.isEmpty() && (vazio(entrada.descricao()) || vazio(entrada.ncm()))) {
            throw new IllegalArgumentException("SKU novo exige descricao e ncm: " + entrada.sku());
        }

        // HTTP antes da transação: um worker lento nunca segura a trava do produto.
        Optional<Classificacao> classificacao = existente.map(p -> p.getTags().isEmpty()).orElse(true)
                ? classificador.classificar(entrada.sku(),
                        existente.map(ProdutoEstoque::getDescricao).orElse(entrada.descricao()))
                : Optional.empty();

        return transacao.execute(_ -> registrarNaTransacao(entrada, classificacao));
    }

    private LoteRegistrado registrarNaTransacao(EntradaLote entrada, Optional<Classificacao> classificacao) {
        var produto = produtos.buscarParaAtualizar(entrada.sku())
                .orElseGet(() -> produtos.save(new ProdutoEstoque(entrada.sku(), entrada.descricao(), entrada.ncm())));
        if (produto.getTags().isEmpty()) {
            classificacao.ifPresent(c -> produto.classificar(c.tags(), c.categoria()));
        }

        var lote = lotes.save(produto.receberLote(entrada.codigoLote(), entrada.quantidade(), entrada.validade(),
                relogio.instant()));
        int atendidas = atenderDemandaReprimida(produto);

        log.info("Lote registrado sku={} lote={} qtd={} saldo={} classificado={} demandasAtendidas={}",
                produto.getSku(), lote.getCodigoLote(), lote.getQuantidade(), produto.getSaldoDisponivel(),
                produto.getTags().isPresent(), atendidas);
        return new LoteRegistrado(lote.getId(), produto.getSku(), produto.getSaldoDisponivel(),
                produto.getTags().isPresent(), atendidas);
    }

    /** Responde as solicitações que ficaram sem saldo, na ordem em que chegaram, enquanto houver lote. */
    private int atenderDemandaReprimida(ProdutoEstoque produto) {
        int atendidas = 0;
        for (var demanda : demandas.findBySkuAndAtendidaFalseOrderByRegistradoEmAsc(produto.getSku())) {
            var envio = expedicao.expedirDoProximoLote(produto, demanda.getSolicitacaoOriginal(),
                    demanda.getQtdSolicitada());
            if (envio.isEmpty()) {
                break;
            }
            demanda.marcarAtendida();
            atendidas++;
            log.info("Demanda reprimida atendida sku={} solicitacao={} qtd={}/{} lote={}", produto.getSku(),
                    demanda.getSolicitacaoOriginal(), envio.get().qtd(), demanda.getQtdSolicitada(), envio.get().lote());
        }
        return atendidas;
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
