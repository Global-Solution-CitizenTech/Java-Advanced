package fiap.com.br.zelafix.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DddValidatorTest {

    private DddValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DddValidator();
    }

    @Test
    @DisplayName("Deve aceitar null e string vazia ou em branco (delega obrigatoriedade para @NotBlank)")
    void deveAceitarNullOuBlank() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
        assertTrue(validator.isValid("   ", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11", "12", "19", "21", "27", "31", "35", "41", "48", "51", "55",
            "61", "67", "71", "79", "81", "85", "91", "99"
    })
    @DisplayName("Deve validar com sucesso DDDs válidos da Anatel")
    void deveValidarDddsValidos(String ddd) {
        assertTrue(validator.isValid(ddd, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00", "01", "20", "23", "25", "26", "29", "30", "36", "39", "50", "52", "70", "72", "76", "78", "80", "90",
            "1", "111", "AA", "1a", " 0 "
    })
    @DisplayName("Deve rejeitar DDDs inexistentes ou fora da tabela oficial da Anatel")
    void deveRejeitarDddsInvalidos(String ddd) {
        assertFalse(validator.isValid(ddd, null));
    }
}
