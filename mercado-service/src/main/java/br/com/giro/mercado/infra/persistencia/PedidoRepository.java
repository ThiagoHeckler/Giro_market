package br.com.giro.mercado.infra.persistencia;

import br.com.giro.mercado.domain.Pedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    /** SELECT ... FOR UPDATE: pagamento e expiração do mesmo pedido se serializam aqui. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> buscarParaAtualizar(@Param("id") UUID id);

    /**
     * Trava o pedido se ninguém o estiver usando. Ocupado (um pagamento em curso, outra réplica do
     * job): vazio, e a expiração tenta de novo na próxima rodada.
     */
    @Query(value = "SELECT * FROM pedido WHERE id = :id FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<Pedido> travarSeLivre(@Param("id") UUID id);
}
