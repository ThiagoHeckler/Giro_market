package br.com.giro.estoque.infra.persistencia;

import br.com.giro.estoque.domain.ProdutoEstoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProdutoEstoqueRepository extends JpaRepository<ProdutoEstoque, String> {

    /** SELECT ... FOR UPDATE: serializa expedições e entradas de lote do mesmo SKU. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProdutoEstoque p where p.sku = :sku")
    Optional<ProdutoEstoque> buscarParaAtualizar(@Param("sku") String sku);
}
