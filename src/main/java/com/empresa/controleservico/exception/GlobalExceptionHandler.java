package com.empresa.controleservico.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.ServletRequestBindingException;

/**
 * Converte exceções das rotas MVC em páginas de erro com status HTTP adequado.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * @param ex exceção com a identificação do recurso ausente
     * @param model modelo da página de erro
     * @return página HTTP 404 para entidades inexistentes
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(EntityNotFoundException ex, Model model) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        model.addAttribute("errorTitle",   "Recurso não encontrado");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("pageTitle",    "Erro 404");
        return "error/not-found";
    }

    /**
     * @param ex violação de uma regra de entrada
     * @param model modelo da página de erro
     * @return página HTTP 400 para regras de entrada inválidas
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(IllegalArgumentException ex, Model model) {
        log.warn("Requisição inválida: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Requisição inválida");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("pageTitle", "Erro 400");
        return "error/general";
    }

    /**
     * @param ex falha de binding ou conversão de parâmetro
     * @param model modelo da página de erro
     * @return página HTTP 400 para parâmetros ausentes ou incompatíveis
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ServletRequestBindingException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidParameters(Exception ex, Model model) {
        log.warn("Parâmetros inválidos: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Requisição inválida");
        model.addAttribute("errorMessage", "Verifique os parâmetros informados e tente novamente.");
        model.addAttribute("pageTitle", "Erro 400");
        return "error/general";
    }

    /**
     * @param ex violação de FK, unicidade ou outra constraint
     * @param model modelo da página de erro
     * @return página HTTP 409 para violações de integridade persistente
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleConflict(DataIntegrityViolationException ex, Model model) {
        log.warn("Operação viola a integridade dos dados", ex);
        model.addAttribute("errorTitle", "Operação não permitida");
        model.addAttribute("errorMessage", "O registro está em uso ou conflita com dados existentes.");
        model.addAttribute("pageTitle", "Erro 409");
        return "error/general";
    }

    /**
     * @param ex falha não tratada por handlers mais específicos
     * @param model modelo da página de erro
     * @return página HTTP 500 sem exposição de detalhes internos
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Erro inesperado: {}", ex.getMessage(), ex);
        model.addAttribute("errorTitle",   "Erro interno");
        model.addAttribute("errorMessage", "Ocorreu um erro inesperado. Tente novamente.");
        model.addAttribute("pageTitle",    "Erro");
        return "error/general";
    }
}
