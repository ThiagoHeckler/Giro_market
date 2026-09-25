package br.com.giro.estoque.application;

import br.com.giro.estoque.infra.persistencia.ProdutoEstoqueRepository;
import br.com.giro.estoque.infra.tags.ClassificadorTags;
import br.com.giro.estoque.infra.tags.TagsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Reprocessa produtos que entraram sem tags porque o worker estava fora do ar. */
@Service
public class ReclassificacaoTags {

    private static final Logger log = LoggerFactory.getLogger(ReclassificacaoTags.class);

    private final ProdutoEstoqueRepository produtos;
    private final ClassificadorTags classificador;
    private final ClassificacaoDeProduto classificacaoDeProduto;
    private final TransactionTemplate transacao;
    private final TagsProperties propriedades;

    public ReclassificacaoTags(ProdutoEstoqueRepository produtos, ClassificadorTags classificador,
                               ClassificacaoDeProduto classificacaoDeProduto, TransactionTemplate transacao,
                               TagsProperties propriedades) {
        this.produtos = produtos;
        this.classificador = classificador;
        this.classificacaoDeProduto = classificacaoDeProduto;
        this.transacao = transacao;
        this.propriedades = propriedades;
    }

    /** @return quantos produtos ganharam tags nesta rodada */
    public int reclassificarPendentes() {
        int classificados = 0;
        for (var pendente : produtos.semTags(Limit.of(propriedades.loteReclassificacao()))) {
            var classificacao = classificador.classificar(pendente.getSku(), pendente.getDescricao());
            if (classificacao.isEmpty()) {
                // Worker ainda fora: não adianta insistir nos outros agora, cada um custaria um timeout.
                break;
            }
            var c = classificacao.get();
            boolean aplicou = Boolean.TRUE.equals(transacao.execute(_ -> produtos.buscarParaAtualizar(pendente.getSku())
                    .filter(p -> p.getTags().isEmpty())
                    .map(p -> {
                        classificacaoDeProduto.aplicar(p, c);
                        return true;
                    })
                    .orElse(false)));
            if (aplicou) {
                classificados++;
            }
        }
        if (classificados > 0) {
            log.info("Reclassificação de tags: {} produto(s) classificado(s)", classificados);
        }
        return classificados;
    }
}
