package br.com.giro.estoque.web;

import br.com.giro.estoque.infra.persistencia.ProdutoEstoqueRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de produto do almoxarifado; o mercado usa no cadastro para herdar a classificação atual. */
@RestController
@RequestMapping("/produtos")
public class ProdutoEstoqueController {

    private final ProdutoEstoqueRepository produtos;

    public ProdutoEstoqueController(ProdutoEstoqueRepository produtos) {
        this.produtos = produtos;
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ProdutoEstoqueResposta> buscar(@PathVariable String sku) {
        return ResponseEntity.of(produtos.findById(sku).map(ProdutoEstoqueResposta::de));
    }
}
