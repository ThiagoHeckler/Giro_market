package br.com.giro.estoque.infra.persistencia;

import br.com.giro.estoque.domain.Lote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoteRepository extends JpaRepository<Lote, Long> {
}
