package com.pokemon.tcg.exception;

public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(Long gameId) {
        super("Game not found: " + gameId);
    }

    public GameNotFoundException(String message) {
        super(message);
    }
}
