package com.pokemon.tcg.engine.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DamageResult {
    private int baseDamage;
    private int attackerModifier;
    private int weaknessModifier;
    private int resistanceModifier;
    private int finalDamage;
    private int damageCounters;
}
