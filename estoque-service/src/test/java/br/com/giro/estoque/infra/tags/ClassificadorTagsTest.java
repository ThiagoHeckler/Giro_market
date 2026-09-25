package br.com.giro.estoque.infra.tags;

import br.com.giro.estoque.IntegracaoTest;
import br.com.giro.estoque.WorkerFalso;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Formato da chamada ao tag-worker: é o contrato com o serviço Python (POST /classificar). */
class ClassificadorTagsTest extends IntegracaoTest {

    @Autowired
    ClassificadorTags classificador;

    @Test
    void enviaSkuEDescricaoComoJson() {
        classificador.classificar("7894900011517", "COCA COLA 2L PET");

        assertThat(WorkerFalso.ultimoContentType()).startsWith("application/json");
        assertThat(json.readTree(WorkerFalso.ultimoCorpoRecebido()))
                .isEqualTo(json.readTree("{\"sku\":\"7894900011517\",\"descricao\":\"COCA COLA 2L PET\"}"));
    }

    @Test
    void respostaSemTagsEhTratadaComoIndisponivel() {
        WorkerFalso.responderCom(200, "{\"tags\":[],\"categoria\":\"bebidas\"}");

        assertThat(classificador.classificar("7894900011517", "COCA COLA 2L PET")).isEmpty();
    }
}
