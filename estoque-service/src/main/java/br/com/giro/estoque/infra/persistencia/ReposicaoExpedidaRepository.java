package br.com.giro.estoque.infra.persistencia;

import br.com.giro.estoque.domain.ReposicaoExpedida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReposicaoExpedidaRepository extends JpaRepository<ReposicaoExpedida, Long> {

    /** Rastro de recall: envios que saíram de um lote, do primeiro ao último. */
    List<ReposicaoExpedida> findByLoteIdOrderByExpedidoEmAsc(Long loteId);
}
