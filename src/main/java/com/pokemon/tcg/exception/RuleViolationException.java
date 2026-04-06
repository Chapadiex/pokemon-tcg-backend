package com.pokemon.tcg.exception;

public class RuleViolationException extends RuntimeException {

    public RuleViolationException(String message) {
        super(message);
    }
}
