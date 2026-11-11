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
@Constraint(validatedBy = DddValidator.class)
public @interface DddValidation {
    String message() default "DDD inválido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
