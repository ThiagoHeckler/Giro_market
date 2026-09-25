package br.com.giro.estoque.infra.persistencia;

import br.com.giro.estoque.domain.ProdutoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoEstoqueRepository extends JpaRepository<ProdutoEstoque, String> {
}
