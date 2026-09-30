package br.com.giro.estoque.infra.persistencia;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Leituras do painel do estoque. SQL direto porque são projeções com junções que as entidades
 * não modelam (referenciam o produto pelo SKU, sem associação JPA). Só leitura: nada aqui muda estado.
 */
@Component
@Transactional(readOnly = true)
public class ConsultasPainel {

    /** Horizonte do KPI de lotes perto do vencimento. */
    public static final int DIAS_ALERTA_VALIDADE = 30;

    private final JdbcClient jdbc;
    private final Clock relogio;

    public ConsultasPainel(JdbcClient jdbc, Clock relogio) {
        this.jdbc = jdbc;
        this.relogio = relogio;
    }

    /**
     * {@code skusSemSaldo} conta saldo expedível: SKU cujo saldo está zerado ou só em lotes vencidos,
     * que a expedição FEFO não envia. {@code lotesVencendo} são lotes com saldo que vencem nos próximos
     * {@value #DIAS_ALERTA_VALIDADE} dias.
     */
    public record Resumo(long skusCadastrados, long skusSemSaldo, long demandasAbertas, long lotesVencendo) {
    }

    public record DemandaAberta(long id, String sku, String descricao, int qtdSolicitada, Instant registradoEm) {
    }

    /** {@code entregueEm} nulo enquanto a {@code ReposicaoEnviada} ainda não chegou ao mercado. */
    public record ReposicaoRecente(UUID eventId, String sku, String descricao, int qtd, String codigoLote,
                                   Instant expedidoEm, Instant entregueEm) {
    }

    public Resumo resumo() {
        var hoje = LocalDate.now(relogio);
        return jdbc.sql("""
                        SELECT (SELECT count(*) FROM produto_estoque)                      AS skus,
                               (SELECT count(*) FROM produto_estoque p
                                 WHERE NOT EXISTS (SELECT 1 FROM lote l
                                                    WHERE l.sku = p.sku AND l.quantidade_disponivel > 0
                                                      AND (l.validade IS NULL OR l.validade >= :hoje))) AS sem_saldo,
                               (SELECT count(*) FROM demanda_reprimida WHERE NOT atendida) AS demandas,
                               (SELECT count(*) FROM lote
                                 WHERE quantidade_disponivel > 0
                                   AND validade BETWEEN :hoje AND :limite)                 AS vencendo
                        """)
                .param("hoje", hoje)
                .param("limite", hoje.plusDays(DIAS_ALERTA_VALIDADE))
                .query((linha, _) -> new Resumo(linha.getLong("skus"), linha.getLong("sem_saldo"),
                        linha.getLong("demandas"), linha.getLong("vencendo")))
                .single();
    }

    /** Demandas reprimidas em aberto, das mais antigas para as mais novas (a ordem em que serão atendidas). */
    public List<DemandaAberta> demandasAbertas() {
        return jdbc.sql("""
                        SELECT d.id, d.sku, p.descricao, d.qtd_solicitada, d.registrado_em
                        FROM demanda_reprimida d
                        JOIN produto_estoque p ON p.sku = d.sku
                        WHERE NOT d.atendida
                        ORDER BY d.registrado_em, d.id
                        """)
                .query((linha, _) -> new DemandaAberta(linha.getLong("id"), linha.getString("sku"),
                        linha.getString("descricao"), linha.getInt("qtd_solicitada"),
                        linha.getTimestamp("registrado_em").toInstant()))
                .list();
    }

    /** Envios mais recentes primeiro, com o status de entrega do evento na outbox. */
    public List<ReposicaoRecente> reposicoesRecentes(int limite) {
        return jdbc.sql("""
                        SELECT r.event_id, r.sku, p.descricao, r.qtd, l.codigo_lote, r.expedido_em, o.enviado_em
                        FROM reposicao_expedida r
                        JOIN produto_estoque p ON p.sku = r.sku
                        JOIN lote l ON l.id = r.lote_id
                        LEFT JOIN outbox_event o ON o.id = r.event_id
                        ORDER BY r.expedido_em DESC, r.id DESC
                        LIMIT :limite
                        """)
                .param("limite", limite)
                .query((linha, _) -> {
                    var entregue = linha.getTimestamp("enviado_em");
                    return new ReposicaoRecente(linha.getObject("event_id", UUID.class), linha.getString("sku"),
                            linha.getString("descricao"), linha.getInt("qtd"), linha.getString("codigo_lote"),
                            linha.getTimestamp("expedido_em").toInstant(),
                            entregue == null ? null : entregue.toInstant());
                })
                .list();
    }
}
