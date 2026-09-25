package br.com.giro.estoque.infra.persistencia;

import br.com.giro.estoque.domain.DemandaReprimida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandaReprimidaRepository extends JpaRepository<DemandaReprimida, Long> {

    /** Demandas em aberto do SKU, das mais antigas para as mais novas. */
    List<DemandaReprimida> findBySkuAndAtendidaFalseOrderByRegistradoEmAsc(String sku);
}
