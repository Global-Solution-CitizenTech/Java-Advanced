package fiap.com.br.zelafix.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ValidationErrorDetail(String campo, String mensagem) {
        public ValidationErrorDetail(FieldError error) {
            this(error.getField(), error.getDefaultMessage());
        }

        public ValidationErrorDetail(ObjectError error) {
            this(extrairCampo(error), error.getDefaultMessage());
        }

        private static String extrairCampo(ObjectError error) {
            return error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
        }
    }

    public record ApiErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path
    ) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<ValidationErrorDetail>>> handleValidation(
            MethodArgumentNotValidException exception) {
        log.warn("Falha de validação de entrada detectada: {} erro(s)", exception.getErrorCount());

        List<ValidationErrorDetail> errors = exception.getAllErrors().stream()
                .map(ValidationErrorDetail::new)
                .toList();

        return ResponseEntity.badRequest().body(Map.of("erros", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, List<ValidationErrorDetail>>> handleConstraintViolation(
            ConstraintViolationException exception) {
        log.warn("Falha de validação de constraint: {}", exception.getMessage());
        List<ValidationErrorDetail> errors = exception.getConstraintViolations().stream()
                .map(cv -> new ValidationErrorDetail(
                        cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : "parametro",
                        cv.getMessage()
                ))
                .toList();
        return ResponseEntity.badRequest().body(Map.of("erros", errors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception, HttpServletRequest request) {
        log.warn("Recurso não encontrado: {}", exception.getMessage());
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({BusinessException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            Exception exception, HttpServletRequest request) {
        log.warn("Violação de regra de negócio: {}", exception.getMessage());
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException exception, HttpServletRequest request) {
        log.warn("Falha de autenticação para a requisição {}", request.getRequestURI());
        return error(HttpStatus.UNAUTHORIZED, "Credenciais inválidas: e-mail ou senha incorretos.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        log.warn("Acesso negado para a requisição {}", request.getRequestURI());
        return error(HttpStatus.FORBIDDEN, "Você não possui permissão para acessar este recurso.", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        log.warn("JSON com formato ou tipos incorretos: {}", exception.getMessage());
        String mensagem = "Erro na leitura dos dados. Verifique os tipos informados no JSON.";

        Throwable cause = exception.getCause();
        if (cause instanceof JsonMappingException jme && !jme.getPath().isEmpty()) {
            String field = jme.getPath().get(0).getFieldName();
            mensagem = String.format("Valor inválido para o campo '%s'. Verifique o tipo ou formato informado.", field);
        }

        return error(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        log.warn("Parâmetro com tipo inválido detectado: {}", exception.getMessage());
        String tipoEsperado = exception.getRequiredType() != null ? exception.getRequiredType().getSimpleName() : "compatível";
        String mensagem = String.format("O valor '%s' é inválido para o parâmetro '%s'. Esperava-se um tipo %s.",
                exception.getValue(), exception.getName(), tipoEsperado);
        return error(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiErrorResponse> handlePropertyReferenceException(
            PropertyReferenceException exception, HttpServletRequest request) {
        log.warn("Campo de ordenação inválido detectado: {}", exception.getMessage());
        String mensagem = String.format("Propriedade de ordenação '%s' é inválida para a entidade.", exception.getPropertyName());
        return error(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        log.warn("Parâmetro obrigatório ausente: {}", exception.getParameterName());
        String mensagem = String.format("O parâmetro obrigatório '%s' não foi informado.", exception.getParameterName());
        return error(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        log.warn("Método HTTP não suportado: {}", exception.getMessage());
        String mensagem = String.format("Método HTTP '%s' não é suportado para esta rota.", exception.getMethod());
        return error(HttpStatus.METHOD_NOT_ALLOWED, mensagem, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        log.warn("Content-Type não suportado: {}", exception.getMessage());
        String mensagem = String.format("Content-Type '%s' não é suportado para esta requisição.", exception.getContentType());
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, mensagem, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        log.error("Erro de consistência/integridade no banco de dados", exception);
        String msg = exception.getMostSpecificCause() != null ? exception.getMostSpecificCause().getMessage() : "";
        if (msg != null && (msg.contains("ORA-00001") || msg.toLowerCase().contains("unique") || msg.toLowerCase().contains("duplicate"))) {
            return error(HttpStatus.CONFLICT,
                    "Registro duplicado. Verifique se o e-mail ou outro dado único já está cadastrado.", request);
        }
        if (msg != null && msg.contains("ORA-12899")) {
            return error(HttpStatus.BAD_REQUEST,
                    "Tamanho de texto excede o limite máximo permitido pela coluna no banco de dados.", request);
        }
        return error(HttpStatus.BAD_REQUEST,
                "Erro de integridade de dados. Verifique os valores e chaves únicas.", request);
    }

    @ExceptionHandler({ConcurrencyFailureException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiErrorResponse> handleConcurrency(
            Exception exception, HttpServletRequest request) {
        log.warn("Conflito de concorrência detectado: {}", exception.getMessage());
        return error(HttpStatus.CONFLICT,
                "Conflito de concorrência detectado. Operação simultânea em andamento, tente novamente em instantes.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception, HttpServletRequest request) {
        log.warn("Recurso estático ou rota não encontrada: {}", request.getRequestURI());
        return error(HttpStatus.NOT_FOUND, "Recurso não encontrado.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception exception, HttpServletRequest request) {
        log.error("Erro inesperado capturado no handler global", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado no servidor.", request);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status, String message, HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }
}
