package br.com.giro.estoque.infra.persistencia;

import br.com.giro.estoque.domain.DemandaReprimida;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandaReprimidaRepository extends JpaRepository<DemandaReprimida, Long> {
}
