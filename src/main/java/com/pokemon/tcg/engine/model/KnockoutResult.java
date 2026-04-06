package com.pokemon.tcg.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class KnockoutResult {
    private List<GameEvent> events;
    private VictoryResult victoryResult;
}
