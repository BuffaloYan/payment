package com.payment.processor.validation;

import lombok.Getter;

@Getter
public class ValidationResult {
    private final boolean valid;
    private final String errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult error(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }

    public boolean isValid() {
        return valid;
    }
} 