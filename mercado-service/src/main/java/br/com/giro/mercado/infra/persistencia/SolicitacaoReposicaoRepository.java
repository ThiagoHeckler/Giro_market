package br.com.giro.mercado.infra.persistencia;

import br.com.giro.mercado.domain.SolicitacaoReposicao;
import br.com.giro.mercado.domain.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoReposicaoRepository extends JpaRepository<SolicitacaoReposicao, Long> {

    boolean existsBySkuAndStatusIn(String sku, Collection<StatusSolicitacao> status);

    Optional<SolicitacaoReposicao> findByEventId(UUID eventId);
}
