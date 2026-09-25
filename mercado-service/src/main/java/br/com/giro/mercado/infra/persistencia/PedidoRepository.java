package br.com.giro.mercado.infra.persistencia;

import br.com.giro.mercado.domain.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
}
