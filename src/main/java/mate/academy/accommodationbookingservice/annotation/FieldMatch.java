package mate.academy.accommodationbookingservice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// Check filed identity per chosen filed
// NULL is valid

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Constraint(validatedBy = FieldMatchImpl.class)
public @interface FieldMatch {
    String first();

    String second();

    String message() default "Values mismatch";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
