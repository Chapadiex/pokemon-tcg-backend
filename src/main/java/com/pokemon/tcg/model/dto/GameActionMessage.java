package com.pokemon.tcg.model.dto;

import lombok.Data;

import java.util.Map;

@Data
public class GameActionMessage {
    /** DRAW_CARD | PLACE_POKEMON | ATTACH_ENERGY | EVOLVE | PLAY_TRAINER | RETREAT | ATTACK | END_TURN */
    private String type;
    private Map<String, Object> data;
}
