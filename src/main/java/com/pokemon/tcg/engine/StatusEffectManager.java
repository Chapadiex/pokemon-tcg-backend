package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.BetweenTurnsResult;
import com.pokemon.tcg.engine.model.CoinFlipper;
import com.pokemon.tcg.engine.model.GameEvent;
import com.pokemon.tcg.engine.model.GameEventType;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.model.enums.RotationCondition;
import com.pokemon.tcg.model.enums.StatusCondition;
import com.pokemon.tcg.model.game.PokemonInPlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Component
public class StatusEffectManager {

    /** Aplicar una condición respetando las reglas de compatibilidad del TCG */
    public void applyStatus(PokemonInPlay pokemon, StatusCondition status) {
        switch (status) {
            // Condiciones de rotación — mutuamente excluyentes entre sí
            case ASLEEP    -> pokemon.setRotationCondition(RotationCondition.ASLEEP);
            case CONFUSED  -> pokemon.setRotationCondition(RotationCondition.CONFUSED);
            case PARALYZED -> pokemon.setRotationCondition(RotationCondition.PARALYZED);
            // Marcadores — pueden coexistir con rotación y entre sí
            case POISONED  -> pokemon.setPoisoned(true);
            case BURNED    -> pokemon.setBurned(true);
        }
    }

    /** Quitar una condición específica */
    public void removeStatus(PokemonInPlay pokemon, StatusCondition status) {
        switch (status) {
            case ASLEEP, CONFUSED, PARALYZED -> {
                if (pokemon.getRotationCondition() != null
                        && pokemon.getRotationCondition().name().equals(status.name())) {
                    pokemon.setRotationCondition(null);
                }
            }
            case POISONED -> pokemon.setPoisoned(false);
            case BURNED   -> pokemon.setBurned(false);
        }
    }

    /** Quitar todas las condiciones de rotación (al retirarse a Banca) */
    public void removeStatus(PokemonInPlay pokemon) {
        pokemon.setRotationCondition(null);
        // POISONED/BURNED se mantienen al retirarse — solo se curan con carta específica o al evolucionar
    }

    /** Quitar TODAS las condiciones (al evolucionar) */
    public void removeAllConditions(PokemonInPlay pokemon) {
        pokemon.setRotationCondition(null);
        pokemon.setPoisoned(false);
        pokemon.setBurned(false);
    }

    /**
     * Proceso entre turnos (ORDEN: POISONED → BURNED → ASLEEP → PARALYZED).
     * Ambos marcadores se procesan independientemente de la condición de rotación.
     */
    public BetweenTurnsResult processBetweenTurns(GameStateSnapshot state, CoinFlipper coinFlipper) {
        List<GameEvent> events = new ArrayList<>();

        List<PokemonInPlay> activePokemon = new ArrayList<>();
        if (state.getP1ActivePokemon() != null) activePokemon.add(state.getP1ActivePokemon());
        if (state.getP2ActivePokemon() != null) activePokemon.add(state.getP2ActivePokemon());

        for (PokemonInPlay pokemon : activePokemon) {

            // 1. ENVENENADO — 1 contador, sin moneda
            if (pokemon.isPoisoned()) {
                pokemon.setDamageCounters((pokemon.getDamageCounters() != null ? pokemon.getDamageCounters() : 0) + 1);
                events.add(new GameEvent(GameEventType.DAMAGE_DEALT,
                    Map.of("pokemon", pokemon.getName(), "damage", 10, "cause", "POISONED")));
            }

            // 2. QUEMADO — lanzar moneda; cruz = 2 contadores
            if (pokemon.isBurned()) {
                boolean tails = coinFlipper.flip();
                events.add(new GameEvent(GameEventType.COIN_FLIP,
                    Map.of("pokemon", pokemon.getName(), "tails", tails)));
                if (tails) {
                    pokemon.setDamageCounters((pokemon.getDamageCounters() != null ? pokemon.getDamageCounters() : 0) + 2);
                    events.add(new GameEvent(GameEventType.DAMAGE_DEALT,
                        Map.of("pokemon", pokemon.getName(), "damage", 20, "cause", "BURNED")));
                }
            }

            // 3. DORMIDO — cara despierta
            if (pokemon.getRotationCondition() == RotationCondition.ASLEEP) {
                boolean tails = coinFlipper.flip();
                events.add(new GameEvent(GameEventType.COIN_FLIP,
                    Map.of("pokemon", pokemon.getName(), "tails", tails)));
                if (!tails) {
                    pokemon.setRotationCondition(null);
                    events.add(new GameEvent(GameEventType.STATUS_CURED,
                        Map.of("pokemon", pokemon.getName(), "condition", "ASLEEP")));
                }
            }

            // 4. PARALIZADO — se cura automáticamente al fin del turno
            if (pokemon.getRotationCondition() == RotationCondition.PARALYZED) {
                pokemon.setRotationCondition(null);
                events.add(new GameEvent(GameEventType.STATUS_CURED,
                    Map.of("pokemon", pokemon.getName(), "condition", "PARALYZED")));
            }
        }

        return new BetweenTurnsResult(events);
    }

    /** Confusión al atacar: retorna true si el ataque se CANCELA (salió cruz) */
    public boolean checkConfusionCancelsAttack(PokemonInPlay pokemon, CoinFlipper coinFlipper) {
        if (pokemon.getRotationCondition() != RotationCondition.CONFUSED) return false;
        boolean tails = coinFlipper.flip();
        if (tails) {
            pokemon.setDamageCounters((pokemon.getDamageCounters() != null ? pokemon.getDamageCounters() : 0) + 3);
            return true;
        }
        return false;
    }

    public boolean canAttack(PokemonInPlay pokemon) {
        return pokemon.canAttack();
    }

    public boolean canRetreat(PokemonInPlay pokemon) {
        return pokemon.canRetreat();
    }
}
