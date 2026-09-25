package br.com.giro.mercado.infra.persistencia;

import br.com.giro.mercado.domain.ProdutoVitrine;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProdutoVitrineRepository extends JpaRepository<ProdutoVitrine, String> {

    /** SELECT ... FOR UPDATE: serializa checkouts e reposições do mesmo SKU. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProdutoVitrine p where p.sku = :sku")
    Optional<ProdutoVitrine> buscarParaAtualizar(@Param("sku") String sku);
}
