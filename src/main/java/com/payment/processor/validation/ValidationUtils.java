package com.payment.processor.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationUtils {
    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);
    private static final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = validatorFactory.getValidator();

    public static <T> ValidationResult validate(T object) {
        if (object == null) {
            return ValidationResult.error("Request cannot be null");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(object);
        
        if (violations.isEmpty()) {
            return ValidationResult.success();
        }
        
        String errorMessage = violations.stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));
            
        logger.debug("Validation failed with errors: {}", errorMessage);
        return ValidationResult.error(errorMessage);
    }

    public static void close() {
        validatorFactory.close();
    }
} 