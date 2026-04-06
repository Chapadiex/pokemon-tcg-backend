package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.DamageResult;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.model.game.AttackData;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.PokemonInPlay;

@org.springframework.stereotype.Component
public class DamageCalculator {

    public DamageResult calculateDamage(PokemonInPlay attacker, PokemonInPlay defender, AttackData attack, GameStateSnapshot state) {
        int baseDamage = parseBaseDamage(attack != null ? attack.getDamage() : null);
        int attackerModifier = calculateAttackerModifiers(attacker, state);
        int modifiedDamage = baseDamage + attackerModifier;
        int weaknessModifier = calculateWeakness(attacker, defender, modifiedDamage);
        int resistanceModifier = calculateResistance(attacker, defender);
        int finalDamage = Math.max(0, modifiedDamage + weaknessModifier + resistanceModifier);

        return DamageResult.builder()
            .baseDamage(baseDamage)
            .attackerModifier(attackerModifier)
            .weaknessModifier(weaknessModifier)
            .resistanceModifier(resistanceModifier)
            .finalDamage(finalDamage)
            .damageCounters(finalDamage / 10)
            .build();
    }

    private int parseBaseDamage(String damageStr) {
        if (damageStr == null || damageStr.isBlank()) {
            return 0;
        }
        String numeric = damageStr.replaceAll("[^0-9]", "");
        return numeric.isBlank() ? 0 : Integer.parseInt(numeric);
    }

    private int calculateAttackerModifiers(PokemonInPlay attacker, GameStateSnapshot state) {
        int modifier = 0;
        if (attacker != null && attacker.getAttachedTool() != null) {
            modifier += extractDamageModifierFromTool(attacker.getAttachedTool());
        }
        if (attacker != null && attacker.getActiveEffects() != null) {
            modifier += attacker.getActiveEffects().stream()
                .filter(effect -> "DAMAGE_MODIFIER".equals(effect.getType()))
                .mapToInt(effect -> effect.getValue() != null ? effect.getValue() : 0)
                .sum();
        }
        if (state != null && state.getStadiumCard() != null) {
            modifier += extractDamageModifierFromStadium(state.getStadiumCard(), attacker);
        }
        return modifier;
    }

    private int calculateWeakness(PokemonInPlay attacker, PokemonInPlay defender, int currentDamage) {
        if (attacker == null || defender == null || defender.getWeaknesses() == null || attacker.getTypes() == null) {
            return 0;
        }
        return defender.getWeaknesses().stream()
            .filter(weakness -> attacker.getTypes().contains(weakness.getType()))
            .mapToInt(weakness -> parseWeaknessValue(weakness.getValue(), currentDamage))
            .sum();
    }

    private int calculateResistance(PokemonInPlay attacker, PokemonInPlay defender) {
        if (attacker == null || defender == null || defender.getResistances() == null || attacker.getTypes() == null) {
            return 0;
        }
        return defender.getResistances().stream()
            .filter(resistance -> attacker.getTypes().contains(resistance.getType()))
            .mapToInt(resistance -> parseResistanceValue(resistance.getValue()))
            .sum();
    }

    private int parseWeaknessValue(String value, int currentDamage) {
        if (value == null) {
            return 0;
        }
        if (value.startsWith("\u00d7")) {
            return currentDamage * (Integer.parseInt(value.substring(1)) - 1);
        }
        if (value.startsWith("+")) {
            return Integer.parseInt(value.substring(1));
        }
        return 0;
    }

    private int parseResistanceValue(String value) {
        if (value == null) {
            return 0;
        }
        if (value.startsWith("-")) {
            return -Integer.parseInt(value.substring(1));
        }
        return 0;
    }

    private int extractDamageModifierFromTool(CardData tool) {
        return tool.getText() != null && tool.getText().contains("20 more damage") ? 20 : 0;
    }

    private int extractDamageModifierFromStadium(CardData stadium, PokemonInPlay attacker) {
        if (stadium.getText() == null || attacker == null) {
            return 0;
        }
        if (stadium.getText().contains("10 more damage") && attacker.getTypes() != null && attacker.getTypes().contains("Fire")) {
            return 10;
        }
        return 0;
    }
}
