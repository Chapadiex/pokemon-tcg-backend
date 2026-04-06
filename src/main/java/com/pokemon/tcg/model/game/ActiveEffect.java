package com.pokemon.tcg.model.game;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveEffect {
    private String  type;            // "DAMAGE_MODIFIER" | "BLOCK_ATTACK" | "PREVENT_STATUS"
    private Integer value;           // +20, -30, etc.
    private Integer turnsRemaining;  // cuántos turnos dura
    private String  description;     // texto legible del efecto
    private String  sourceAttack;    // nombre del ataque que lo generó
}
