package com.empresa.controleservico.exception;

import com.empresa.controleservico.controller.ApiRestController;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Converte exceções da API auxiliar em respostas RFC 9457 no formato Problem Details.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ApiRestController.class)
@Slf4j
public class ApiExceptionHandler {

    /**
     * Oculta detalhes de conversão e devolve um erro de entrada estável ao cliente.
     *
     * @param ex falha de argumento ou conversão
     * @return Problem Details HTTP 400
     */
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail handleBadRequest(Exception ex) {
        log.warn("Requisição inválida na API: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Parâmetros inválidos para a requisição.");
    }

    /**
     * Converte a ausência de uma entidade em resposta HTTP 404.
     *
     * @param ex falha com a identificação lógica do recurso
     * @return Problem Details HTTP 404
     */
    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail handleNotFound(EntityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Registra falhas imprevistas sem expor a exceção na resposta JSON.
     *
     * @param ex falha inesperada
     * @return Problem Details HTTP 500 com mensagem genérica
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro inesperado na API", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisição.");
    }
}
