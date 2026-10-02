package br.com.giro.mercado.web;

import br.com.giro.mercado.domain.EstoqueInsuficienteException;
import br.com.giro.mercado.domain.PagamentoRecusadoException;
import br.com.giro.mercado.domain.PedidoNaoEncontradoException;
import br.com.giro.mercado.domain.ProdutoJaCadastradoException;
import br.com.giro.mercado.domain.ProdutoNaoEncontradoException;
import br.com.giro.mercado.domain.SkuDesconhecidoNoEstoqueException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros de domínio viram ProblemDetail (RFC 9457) com o status HTTP que o cliente consegue tratar. */
@RestControllerAdvice
public class TratamentoDeErros {

    /** O front usa a propriedade {@code sku} para apontar qual item do carrinho acabou. */
    @ExceptionHandler(EstoqueInsuficienteException.class)
    ProblemDetail estoqueInsuficiente(EstoqueInsuficienteException e) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problema.setTitle("Estoque insuficiente");
        problema.setProperty("sku", e.getSku());
        return problema;
    }

    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    ProblemDetail produtoNaoEncontrado(ProdutoNaoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    ProblemDetail pedidoNaoEncontrado(PedidoNaoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(PagamentoRecusadoException.class)
    ProblemDetail pagamentoRecusado(PagamentoRecusadoException e) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problema.setTitle("Pagamento recusado");
        return problema;
    }

    @ExceptionHandler(ProdutoJaCadastradoException.class)
    ProblemDetail produtoJaCadastrado(ProdutoJaCadastradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(SkuDesconhecidoNoEstoqueException.class)
    ProblemDetail skuDesconhecido(SkuDesconhecidoNoEstoqueException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail argumentoInvalido(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Cadastro concorrente do mesmo SKU. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflito(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "conflito de cadastro; tente novamente");
    }
}
