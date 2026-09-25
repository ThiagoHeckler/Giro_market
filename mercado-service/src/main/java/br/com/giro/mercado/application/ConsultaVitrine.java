package br.com.giro.mercado.application;

import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ConsultaVitrine {

    private final ProdutoVitrineRepository produtos;

    public ConsultaVitrine(ProdutoVitrineRepository produtos) {
        this.produtos = produtos;
    }

    public List<ProdutoVitrine> listar(String categoria, String busca) {
        return produtos.buscarNaVitrine(vazioComoNulo(categoria), vazioComoNulo(busca));
    }

    public Optional<ProdutoVitrine> buscar(String sku) {
        return produtos.findById(sku);
    }

    private static String vazioComoNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
