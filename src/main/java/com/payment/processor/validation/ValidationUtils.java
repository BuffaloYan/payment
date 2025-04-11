package com.payment.processor.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidationUtils {
    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    public static <T> ValidationResult validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        
        if (violations.isEmpty()) {
            return ValidationResult.success();
        }
        
        String errorMessage = violations.stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));
            
        return ValidationResult.error(errorMessage);
    }

    public static void close() {
        factory.close();
    }
} 