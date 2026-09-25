package br.com.giro.estoque.infra.persistencia;

import br.com.giro.estoque.domain.Lote;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    /**
     * FEFO: com saldo e não vencidos, validade mais próxima primeiro; sem validade por último.
     * Chamar com a linha do produto já travada — é ela que serializa o acesso aos lotes do SKU.
     */
    @Query("""
            select l from Lote l
            where l.sku = :sku and l.quantidadeDisponivel > 0
              and (l.validade is null or l.validade >= :hoje)
            order by l.validade asc nulls last, l.recebidoEm asc, l.id asc
            """)
    List<Lote> expediveis(@Param("sku") String sku, @Param("hoje") LocalDate hoje, Limit limite);

    default Optional<Lote> proximoParaExpedir(String sku, LocalDate hoje) {
        return expediveis(sku, hoje, Limit.of(1)).stream().findFirst();
    }
}
