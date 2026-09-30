package br.com.giro.estoque.web;

import br.com.giro.estoque.infra.persistencia.ConsultasPainel;
import br.com.giro.estoque.infra.persistencia.ConsultasPainel.DemandaAberta;
import br.com.giro.estoque.infra.persistencia.ConsultasPainel.ReposicaoRecente;
import br.com.giro.estoque.infra.persistencia.ConsultasPainel.Resumo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Leituras do painel do estoque (estoque-web). Só dados do próprio estoque — nada do mercado. */
@RestController
@RequestMapping("/painel")
public class PainelController {

    private static final int LIMITE_MAXIMO_REPOSICOES = 100;

    private final ConsultasPainel consultas;

    public PainelController(ConsultasPainel consultas) {
        this.consultas = consultas;
    }

    @GetMapping("/resumo")
    public Resumo resumo() {
        return consultas.resumo();
    }

    @GetMapping("/demandas")
    public List<DemandaAberta> demandas() {
        return consultas.demandasAbertas();
    }

    @GetMapping("/reposicoes")
    public List<ReposicaoRecente> reposicoes(@RequestParam(defaultValue = "20") int limite) {
        return consultas.reposicoesRecentes(Math.clamp(limite, 1, LIMITE_MAXIMO_REPOSICOES));
    }
}
