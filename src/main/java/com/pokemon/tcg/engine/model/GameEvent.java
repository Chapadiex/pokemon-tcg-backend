package com.pokemon.tcg.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class GameEvent {
    private GameEventType type;
    private Map<String, Object> data;
}
