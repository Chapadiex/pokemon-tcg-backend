package com.pokemon.tcg.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BetweenTurnsResult {
    private List<GameEvent> events;
}
