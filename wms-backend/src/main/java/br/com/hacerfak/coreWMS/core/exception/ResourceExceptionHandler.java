package br.com.hacerfak.coreWMS.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice // Intercepta todos os Controllers
public class ResourceExceptionHandler {

    // Erro 404 - Recurso não encontrado (ex: Produto ID 999)
    @ExceptionHandler(EntityNotFoundException.class) // Você precisará criar essa exceção ou usar RuntimeException
    public ResponseEntity<StandardError> entityNotFound(RuntimeException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso não encontrado", e.getMessage(), request);
    }

    // Erro 400 - Regra de Negócio (ex: Estoque insuficiente, Lote vencido)
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<StandardError> businessException(RuntimeException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Erro de Regra de Negócio", e.getMessage(), request);
    }

    // Erro 403/401 - Segurança
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<StandardError> badCredentials(BadCredentialsException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Credenciais Inválidas", "Usuário ou senha incorretos", request);
    }

    // Fallback - Qualquer outro erro não tratado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> generalError(Exception e, HttpServletRequest request) {
        e.printStackTrace(); // Loga no servidor para debug
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro Interno",
                "Ocorreu um erro inesperado. Contate o suporte.", request);
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
