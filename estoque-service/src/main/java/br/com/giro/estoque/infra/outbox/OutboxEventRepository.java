package br.com.giro.estoque.infra.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Trava um lote de eventos prontos para envio. SKIP LOCKED deixa vários publicadores
     * (réplicas ou execuções sobrepostas) trabalharem em paralelo sem enviar o mesmo evento.
     */
    @Query(value = """
            SELECT * FROM outbox_event
            WHERE status = 'PENDING' AND proxima_tentativa_em <= :agora
            ORDER BY criado_em
            LIMIT :limite
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> travarProntosParaEnvio(@Param("agora") Instant agora, @Param("limite") int limite);
}
