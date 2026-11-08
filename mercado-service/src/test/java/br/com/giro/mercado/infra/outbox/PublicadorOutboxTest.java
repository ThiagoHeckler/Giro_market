package br.com.giro.mercado.infra.outbox;

import br.com.giro.mercado.DestinoFalso;
import br.com.giro.mercado.IntegracaoTest;
import br.com.giro.mercado.application.contrato.ReposicaoSolicitada;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

class PublicadorOutboxTest extends IntegracaoTest {

    @Autowired
    PublicadorOutbox publicador;

    @Autowired
    Outbox outbox;

    @Autowired
    OutboxEventRepository eventos;

    @Autowired
    TransactionTemplate transacao;

    private ReposicaoSolicitada registraEvento() {
        var evento = new ReposicaoSolicitada(UUID.randomUUID(), "7894900011517", 16, Instant.now());
        transacao.executeWithoutResult(_ -> outbox.registrar(evento));
        return evento;
    }

    @Test
    void entregaOEventoEMarcaComoEnviado() {
        var evento = registraEvento();

        assertThat(publicador.publicarProntos()).isOne();

        assertThat(DestinoFalso.recebidos()).singleElement().satisfies(r -> {
            assertThat(r.tipo()).isEqualTo("ReposicaoSolicitada");
            assertThat(json.readTree(r.corpo()).get("eventId").asString()).isEqualTo(evento.eventId().toString());
        });
        var gravado = eventos.findById(evento.eventId()).orElseThrow();
        assertThat(gravado.getStatus()).isEqualTo(StatusOutbox.SENT);
        assertThat(gravado.getEnviadoEm()).isPresent();
    }

    @Test
    void falhaNoDestinoMantemPendenteEAdiaComBackoff() {
        var evento = registraEvento();
        DestinoFalso.responderCom(503);

        assertThat(publicador.publicarProntos()).isZero();
        assertThat(publicador.publicarProntos()).isZero();   // ainda dentro do backoff: nem tenta

        assertThat(DestinoFalso.recebidos()).hasSize(1);
        var gravado = eventos.findById(evento.eventId()).orElseThrow();
        assertThat(gravado.getStatus()).isEqualTo(StatusOutbox.PENDING);
        assertThat(gravado.getTentativas()).isOne();
        assertThat(gravado.getUltimoErro()).hasValueSatisfying(erro -> assertThat(erro).contains("503"));
        assertThat(gravado.getProximaTentativaEm()).isAfter(Instant.now());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 409, 422})
    void recusaDefinitivaVaiParaFailedSemReenvio(int status) {
        var evento = registraEvento();
        DestinoFalso.responderCom(status);

        assertThat(publicador.publicarProntos()).isZero();

        var gravado = eventos.findById(evento.eventId()).orElseThrow();
        assertThat(gravado.getStatus()).isEqualTo(StatusOutbox.FAILED);
        assertThat(gravado.getTentativas()).isOne();
        assertThat(gravado.getUltimoErro()).hasValueSatisfying(erro -> assertThat(erro).contains(String.valueOf(status)));
        assertThat(gravado.getEnviadoEm()).isEmpty();

        // Mesmo com o destino de volta e o prazo de backoff vencido, não sai de novo.
        DestinoFalso.responderCom(204);
        jdbc.update("UPDATE outbox_event SET proxima_tentativa_em = now() - interval '1 hour'");
        assertThat(publicador.publicarProntos()).isZero();
        assertThat(DestinoFalso.recebidos()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 408, 429})
    void quatroCentosTemporariosSeguemComBackoff(int status) {
        var evento = registraEvento();
        DestinoFalso.responderCom(status);

        assertThat(publicador.publicarProntos()).isZero();

        var gravado = eventos.findById(evento.eventId()).orElseThrow();
        assertThat(gravado.getStatus()).isEqualTo(StatusOutbox.PENDING);
        assertThat(gravado.getTentativas()).isOne();
        assertThat(gravado.getProximaTentativaEm()).isAfter(Instant.now());
    }

    @Test
    void eventoRejeitadoNaoTravaOsSeguintes() {
        var rejeitado = registraEvento();
        DestinoFalso.responderCom(400);
        publicador.publicarProntos();

        DestinoFalso.responderCom(204);
        var seguinte = registraEvento();
        assertThat(publicador.publicarProntos()).isOne();

        assertThat(eventos.findById(rejeitado.eventId()).orElseThrow().getStatus()).isEqualTo(StatusOutbox.FAILED);
        assertThat(eventos.findById(seguinte.eventId()).orElseThrow().getStatus()).isEqualTo(StatusOutbox.SENT);
    }

    @Test
    void eventoTravadoPorOutroPublicadorEhPulado() throws Exception {
        registraEvento();
        var travou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);

        // Outro publicador (outra réplica) segurando a linha.
        var outro = Thread.ofVirtual().start(() -> transacao.executeWithoutResult(_ -> {
            jdbc.queryForList("SELECT id FROM outbox_event FOR UPDATE");
            travou.countDown();
            try {
                liberar.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        travou.await();

        assertThat(publicador.publicarProntos()).isZero();   // SKIP LOCKED: não espera, não duplica
        assertThat(DestinoFalso.recebidos()).isEmpty();

        liberar.countDown();
        outro.join();
        assertThat(publicador.publicarProntos()).isOne();
    }

    @Test
    void backoffDobraAteOTeto() {
        assertThat(publicador.backoff(1)).isEqualTo(Duration.ofSeconds(1));
        assertThat(publicador.backoff(4)).isEqualTo(Duration.ofSeconds(8));
        assertThat(publicador.backoff(40)).isEqualTo(Duration.ofMinutes(5));
    }
}
