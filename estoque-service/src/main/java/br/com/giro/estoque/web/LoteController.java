package br.com.giro.estoque.web;

import br.com.giro.estoque.application.EntradaDeLote;
import br.com.giro.estoque.application.LoteRegistrado;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lotes")
public class LoteController {

    private final EntradaDeLote entradaDeLote;

    public LoteController(EntradaDeLote entradaDeLote) {
        this.entradaDeLote = entradaDeLote;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoteRegistrado registrar(@Valid @RequestBody EntradaLoteRequest requisicao) {
        return entradaDeLote.registrar(requisicao.paraComando());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail entradaInvalida(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Código de lote repetido para o SKU, ou o mesmo SKU novo cadastrado em paralelo. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflito(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "lote já registrado para este SKU, ou o SKU foi cadastrado em paralelo; tente novamente");
    }
}
