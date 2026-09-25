package br.com.giro.mercado.infra.persistencia;

import br.com.giro.mercado.domain.ProdutoVitrine;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProdutoVitrineRepository extends JpaRepository<ProdutoVitrine, String> {

    /** SELECT ... FOR UPDATE: serializa checkouts e reposições do mesmo SKU. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProdutoVitrine p where p.sku = :sku")
    Optional<ProdutoVitrine> buscarParaAtualizar(@Param("sku") String sku);

    /** Vitrine com filtros opcionais; disponíveis primeiro, depois por nome. */
    @Query("""
            select p from ProdutoVitrine p
            where (:categoria is null or p.categoria = :categoria)
              and (:busca is null
                   or lower(p.nome) like lower(concat('%', :busca, '%'))
                   or p.sku = :busca)
            order by case when p.status = br.com.giro.mercado.domain.StatusVitrine.DISPONIVEL then 0 else 1 end,
                     p.nome
            """)
    List<ProdutoVitrine> buscarNaVitrine(@Param("categoria") String categoria, @Param("busca") String busca);
}
