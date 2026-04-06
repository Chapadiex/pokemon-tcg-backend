package com.pokemon.tcg.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    record ErrorResponse(String code, String message) {}

    @ExceptionHandler(InvalidMoveException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMove(InvalidMoveException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("INVALID_MOVE", ex.getMessage()));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(GameNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(RuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleRuleViolation(RuleViolationException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("RULE_VIOLATION", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.internalServerError()
            .body(new ErrorResponse("INTERNAL_ERROR", ex.getMessage()));
    }
}
