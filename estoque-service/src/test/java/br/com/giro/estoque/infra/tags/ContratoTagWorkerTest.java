package br.com.giro.estoque.infra.tags;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato Java ↔ Python contra o tag-worker de verdade, construído do Dockerfile do repositório.
 * Sem GROQ_API_KEY o worker usa o classificador por palavras-chave: resposta determinística, sem rede.
 * Servidores falsos não pegam incompatibilidades de protocolo — este teste pega.
 */
@Testcontainers
class ContratoTagWorkerTest {

    @Container
    static final GenericContainer<?> WORKER = new GenericContainer<>(
            new ImageFromDockerfile("giro/tag-worker-teste", false)
                    // Só o que o Dockerfile copia: o .venv local não entra no contexto de build.
                    .withFileFromPath("Dockerfile", Path.of("../tag-worker/Dockerfile"))
                    .withFileFromPath("pyproject.toml", Path.of("../tag-worker/pyproject.toml"))
                    .withFileFromPath("uv.lock", Path.of("../tag-worker/uv.lock"))
                    .withFileFromPath("app", Path.of("../tag-worker/app")))
            .withEnv("GROQ_API_KEY", "")
            .withExposedPorts(8000)
            .waitingFor(Wait.forHttp("/saude").forStatusCode(200));

    private static ClassificadorTags classificador() {
        var url = URI.create("http://%s:%d".formatted(WORKER.getHost(), WORKER.getMappedPort(8000)));
        return new ClassificadorTags(new TagsProperties(url, Duration.ofSeconds(2), false, Duration.ofMinutes(5), 20));
    }

    @Test
    void workerRealClassificaOQueOEstoqueEnvia() {
        var classificacao = classificador().classificar("7894900011517", "COCA COLA 2L PET");

        assertThat(classificacao).hasValueSatisfying(c -> {
            assertThat(c.categoria()).isEqualTo("bebidas");
            assertThat(c.tags()).isEqualTo(List.of("coca", "cola", "2l", "pet"));
        });
    }
}
