package br.com.giro.mercado.infra.persistencia;

import br.com.giro.mercado.domain.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    List<Reserva> findByPedidoId(UUID pedidoId);

    /** Pedidos com reserva ativa vencida, mais antigos primeiro (usa o índice ix_reserva_ativa_expira). */
    @Query(value = """
            SELECT pedido_id FROM reserva
            WHERE status = 'ATIVA' AND expira_em <= :agora
            GROUP BY pedido_id
            ORDER BY min(expira_em)
            LIMIT :limite
            """, nativeQuery = true)
    List<UUID> pedidosComReservaVencida(@Param("agora") Instant agora, @Param("limite") int limite);
}
