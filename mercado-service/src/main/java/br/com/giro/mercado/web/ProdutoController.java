package br.com.giro.mercado.web;

import br.com.giro.mercado.application.CadastroProduto;
import br.com.giro.mercado.application.ConsultaVitrine;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ConsultaVitrine consulta;
    private final CadastroProduto cadastro;

    public ProdutoController(ConsultaVitrine consulta, CadastroProduto cadastro) {
        this.consulta = consulta;
        this.cadastro = cadastro;
    }

    @GetMapping
    public List<ProdutoResposta> listar(@RequestParam(required = false) String categoria,
                                        @RequestParam(required = false) String busca) {
        return consulta.listar(categoria, busca).stream().map(ProdutoResposta::de).toList();
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ProdutoResposta> buscar(@PathVariable String sku) {
        return ResponseEntity.of(consulta.buscar(sku).map(ProdutoResposta::de));
    }

    @PostMapping
    public ResponseEntity<ProdutoResposta> cadastrar(@Valid @RequestBody NovoProdutoRequest requisicao) {
        var produto = cadastro.cadastrar(requisicao.paraComando());
        return ResponseEntity.created(URI.create("/produtos/" + produto.getSku())).body(ProdutoResposta.de(produto));
    }
}
