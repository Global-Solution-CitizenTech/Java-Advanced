package fiap.com.br.zelafix.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DddValidator implements ConstraintValidator<DddValidation, String> {

    private static final Set<String> DDDS_VALIDOS = Set.of(
            // SP (11-19)
            "11", "12", "13", "14", "15", "16", "17", "18", "19",
            // RJ (21, 22, 24) e ES (27, 28)
            "21", "22", "24", "27", "28",
            // MG (31-35, 37, 38)
            "31", "32", "33", "34", "35", "37", "38",
            // PR (41-46) e SC (47-49)
            "41", "42", "43", "44", "45", "46", "47", "48", "49",
            // RS (51, 53, 54, 55)
            "51", "53", "54", "55",
            // DF, GO, TO, MT, MS, AC, RO (61-69)
            "61", "62", "63", "64", "65", "66", "67", "68", "69",
            // BA (71, 73, 74, 75, 77) e SE (79)
            "71", "73", "74", "75", "77", "79",
            // PE, AL, PB, RN, CE, PI (81-89)
            "81", "82", "83", "84", "85", "86", "87", "88", "89",
            // PA, AM, RR, AP, MA (91-99)
            "91", "92", "93", "94", "95", "96", "97", "98", "99"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return DDDS_VALIDOS.contains(value.trim());
    }
}
