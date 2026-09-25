package br.com.giro.estoque.infra.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

    /**
     * Registra o evento se ainda não foi visto. Retorna 0 para duplicata.
     * Entregas concorrentes do mesmo evento esperam no índice da PK: só uma insere.
     */
    @Modifying
    @Query(value = """
            INSERT INTO inbox_event (event_id, processado_em)
            VALUES (:eventId, :processadoEm)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int registrarSeNovo(@Param("eventId") UUID eventId, @Param("processadoEm") Instant processadoEm);
}
