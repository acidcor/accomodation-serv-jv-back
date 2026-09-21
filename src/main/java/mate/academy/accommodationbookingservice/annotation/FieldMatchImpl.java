package mate.academy.accommodationbookingservice.annotation;

import java.lang.reflect.Field;
import java.util.Objects;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FieldMatchImpl implements ConstraintValidator<FieldMatch, Object> {
    private String firstFieldName;
    private String secondFieldName;

    @Override
    public void initialize(FieldMatch constraintAnnotation) {
        this.firstFieldName = constraintAnnotation.first();
        this.secondFieldName = constraintAnnotation.second();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            Field firstObj = value.getClass().getDeclaredField(firstFieldName);
            firstObj.setAccessible(true);
            Object firstValue = firstObj.get(value);

            Field secondObj = value.getClass().getDeclaredField(secondFieldName);
            secondObj.setAccessible(true);
            Object secondValue = secondObj.get(value);

            if (!Objects.equals(firstValue, secondValue)) {
                String message = String.format(
                        "'%s' and '%s' do not match",
                        firstObj.getName(),
                        secondObj.getName()
                );
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addConstraintViolation();
                return false;
            }
            return true;
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            return false;
        }
    }
}
