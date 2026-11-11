package fiap.com.br.zelafix.validation;

import fiap.com.br.zelafix.usuario.CargoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class EnumValidatorTest {

    private EnumValidator validator;

    @Mock
    private EnumValidation annotation;

    @BeforeEach
    void setUp() {
        validator = new EnumValidator();
        doReturn(CargoUsuario.class).when(annotation).enumClass();
        validator.initialize(annotation);
    }

    @Test
    @DisplayName("Deve aceitar null (delega obrigatoriedade para @NotNull/@NotBlank)")
    void deveAceitarNull() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    @DisplayName("Deve validar valores corretos do enum (case-insensitive e com trim)")
    void deveValidarValoresCorretos() {
        assertTrue(validator.isValid("CIDADAO", null));
        assertTrue(validator.isValid("GESTOR", null));
        assertTrue(validator.isValid("ADMIN", null));
        assertTrue(validator.isValid("  cidadao  ", null));
        assertTrue(validator.isValid("gestor", null));
    }

    @Test
    @DisplayName("Deve rejeitar valores inexistentes no enum")
    void deveRejeitarValoresInvalidos() {
        assertFalse(validator.isValid("INVALIDO", null));
        assertFalse(validator.isValid("MODERADOR", null));
        assertFalse(validator.isValid("", null));
        assertFalse(validator.isValid("   ", null));
    }
}
