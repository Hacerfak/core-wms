package br.com.hacerfak.coreWMS.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class ResourceExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<StandardError> entityNotFound(EntityNotFoundException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso não encontrado", e.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<StandardError> illegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<StandardError> badCredentials(BadCredentialsException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Acesso Negado", "Usuário ou senha inválidos", request);
    }

    // --- Tratamento para Validação de DTOs (@Valid) ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        HttpStatusCode status = HttpStatusCode.valueOf(422); // 422

        ValidationError err = new ValidationError(
                Instant.now(),
                status.value(),
                "Erro de Validação",
                "Verifique os campos obrigatórios",
                request.getRequestURI());

        for (FieldError x : e.getBindingResult().getFieldErrors()) {
            err.addError(x.getField(), x.getDefaultMessage());
        }

        return ResponseEntity.status(status).body(err);
    }

    // --- Tratamento de Integridade (FKs) ---
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> dataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Conflito de Dados",
                "Não é possível excluir ou alterar pois o registro possui vínculos ativos.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> generalError(Exception e, HttpServletRequest request) {
        e.printStackTrace();
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro Interno", e.getMessage(), request);
    }

    private ResponseEntity<StandardError> buildResponse(HttpStatus status, String error, String msg,
            HttpServletRequest request) {
        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                error,
                msg,
                request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }
}