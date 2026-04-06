package com.pokemon.tcg.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ValidationResult {
    private final boolean valid;
    private final String errorMessage;
    private final List<String> warnings;

    public static ValidationResult ok() {
        return ValidationResult.builder()
            .valid(true)
            .warnings(new ArrayList<>())
            .build();
    }

    public static ValidationResult fail(String message) {
        return ValidationResult.builder()
            .valid(false)
            .errorMessage(message)
            .warnings(new ArrayList<>())
            .build();
    }
}
