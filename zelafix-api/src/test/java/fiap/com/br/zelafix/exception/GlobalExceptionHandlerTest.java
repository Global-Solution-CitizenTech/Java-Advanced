package fiap.com.br.zelafix.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentNotValidException e retornar 400 com lista de campos")
    void deveTratarMethodArgumentNotValidException() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("usuarioRequest", "email", "E-mail inválido");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        when(bindingResult.getErrorCount()).thenReturn(1);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, List<GlobalExceptionHandler.ValidationErrorDetail>>> response =
                exceptionHandler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("email", response.getBody().get("erros").getFirst().campo());
        assertEquals("E-mail inválido", response.getBody().get("erros").getFirst().mensagem());
    }

    @Test
    @DisplayName("Deve tratar ConstraintViolationException e retornar 400 com detalhes")
    void deveTratarConstraintViolationException() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("idChamado");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("deve ser maior que zero");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<Map<String, List<GlobalExceptionHandler.ValidationErrorDetail>>> response =
                exceptionHandler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("idChamado", response.getBody().get("erros").getFirst().campo());
    }

    @Test
    @DisplayName("Deve tratar ResourceNotFoundException e retornar 404")
    void deveTratarResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Chamado não encontrado");

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleResourceNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Chamado não encontrado", response.getBody().message());
        assertEquals("/api/test", response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar BusinessException e retornar 400")
    void deveTratarBusinessException() {
        BusinessException ex = new BusinessException("Não é possível rebaixar único admin");

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleBusinessException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertEquals("Não é possível rebaixar único admin", response.getBody().message());
    }

    @Test
    @DisplayName("Deve tratar AuthenticationException e retornar 401")
    void deveTratarAuthenticationException() {
        BadCredentialsException ex = new BadCredentialsException("Senha incorreta");

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleAuthentication(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertTrue(response.getBody().message().contains("Credenciais inválidas"));
    }

    @Test
    @DisplayName("Deve tratar AccessDeniedException e retornar 403")
    void deveTratarAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().status());
    }

    @Test
    @DisplayName("Deve tratar HttpMessageNotReadableException e retornar 400")
    void deveTratarHttpMessageNotReadableException() {
        HttpInputMessage inputMessage = new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(new byte[0]);
            }
            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", inputMessage);

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleHttpMessageNotReadable(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentTypeMismatchException e retornar 400")
    void deveTratarMethodArgumentTypeMismatchException() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", null, null
        );

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleMethodArgumentTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().message().contains("abc"));
        assertTrue(response.getBody().message().contains("id"));
    }

    @Test
    @DisplayName("Deve tratar MissingServletRequestParameterException e retornar 400")
    void deveTratarMissingServletRequestParameterException() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("email", "String");

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleMissingParameter(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().message().contains("email"));
    }

    @Test
    @DisplayName("Deve tratar HttpRequestMethodNotSupportedException e retornar 405")
    void deveTratarHttpRequestMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("PATCH");

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(405, response.getBody().status());
    }

    @Test
    @DisplayName("Deve tratar HttpMediaTypeNotSupportedException e retornar 415")
    void deveTratarHttpMediaTypeNotSupportedException() {
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_XML, List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleMediaTypeNotSupported(ex, request);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertEquals(415, response.getBody().status());
    }

    @Test
    @DisplayName("Deve tratar DataIntegrityViolationException com ORA-00001 (Unique constraint) e retornar 409 Conflict")
    void deveTratarDataIntegrityViolationComChaveDuplicada() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Violation", new RuntimeException("ORA-00001: unique constraint violated"));

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleDataIntegrityViolation(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertTrue(response.getBody().message().contains("Registro duplicado"));
    }

    @Test
    @DisplayName("Deve tratar DataIntegrityViolationException com ORA-12899 (Tamanho de coluna) e retornar 400 Bad Request")
    void deveTratarDataIntegrityViolationComTamanhoExcedido() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Violation", new RuntimeException("ORA-12899: value too large for column"));

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleDataIntegrityViolation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertTrue(response.getBody().message().contains("limite máximo permitido"));
    }

    @Test
    @DisplayName("Deve tratar NoResourceFoundException e retornar 404")
    void deveTratarNoResourceFoundException() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleNoResourceFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
    }

    @Test
    @DisplayName("Deve capturar exceção genérica e retornar 500 com mensagem amigável")
    void deveTratarGenericException() {
        Exception ex = new NullPointerException("Erro inesperado de ponteiro");

        ResponseEntity<GlobalExceptionHandler.ApiErrorResponse> response =
                exceptionHandler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertTrue(response.getBody().message().contains("erro interno"));
    }
}
