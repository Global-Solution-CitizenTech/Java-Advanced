package fiap.com.br.zelafix.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Constraint(validatedBy = EnumValidator.class)
public @interface EnumValidation {
    Class<? extends Enum<?>> enumClass();

    String message() default "Enum inserido incorretamente!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
