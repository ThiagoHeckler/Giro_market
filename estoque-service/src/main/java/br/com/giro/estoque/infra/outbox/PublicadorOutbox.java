package br.com.giro.estoque.infra.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Lê a outbox e entrega os eventos. Entrega é at-least-once: se o commit falhar depois do envio,
 * o evento sai de novo e a inbox do destino descarta a duplicata.
 */
@Component
public class PublicadorOutbox {

    private static final Logger log = LoggerFactory.getLogger(PublicadorOutbox.class);

    private final OutboxEventRepository eventos;
    private final TransporteHttp transporte;
    private final OutboxProperties propriedades;
    private final Clock relogio;

    public PublicadorOutbox(OutboxEventRepository eventos, TransporteHttp transporte,
                            OutboxProperties propriedades, Clock relogio) {
        this.eventos = eventos;
        this.transporte = transporte;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    /** @return quantos eventos foram confirmados pelo destino nesta rodada */
    @Transactional
    public int publicarProntos() {
        var agora = relogio.instant();
        int enviados = 0;
        for (var evento : eventos.travarProntosParaEnvio(agora, propriedades.tamanhoLote())) {
            try {
                transporte.enviar(evento);
                evento.marcarEnviado(relogio.instant());
                enviados++;
            } catch (FalhaEnvioException e) {
                var espera = backoff(evento.getTentativas() + 1);
                evento.agendarNovaTentativa(e.getMessage(), agora.plus(espera));
                log.warn("Falha ao publicar eventId={} tipo={} tentativa={} novaTentativaEm={}: {}",
                        evento.getId(), evento.getTipo(), evento.getTentativas(), espera, e.getMessage());
            }
        }
        if (enviados > 0) {
            log.info("Outbox publicou {} evento(s)", enviados);
        }
        return enviados;
    }

    /** 1s, 2s, 4s, ... até o teto configurado. */
    Duration backoff(int tentativa) {
        var exponencial = Duration.ofSeconds(1L << Math.min(tentativa - 1, 20));
        return exponencial.compareTo(propriedades.backoffMaximo()) > 0 ? propriedades.backoffMaximo() : exponencial;
    }
}
