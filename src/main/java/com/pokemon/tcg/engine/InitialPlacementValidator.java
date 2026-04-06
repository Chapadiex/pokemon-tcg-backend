package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.ValidationResult;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.PokemonInPlay;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InitialPlacementValidator {

    public ValidationResult validatePlaceDuringSetup(CardData card, String position, GameStateSnapshot state, boolean isPlayer1) {
        if (card == null || !card.isBasicPokemon()) {
            return ValidationResult.fail("Solo se pueden colocar Pokemon Basicos durante la colocacion inicial");
        }

        List<CardData> hand = isPlayer1 ? state.getP1Hand() : state.getP2Hand();
        boolean inHand = hand != null && hand.stream().anyMatch(candidate -> candidate.getId().equals(card.getId()));
        if (!inHand) {
            return ValidationResult.fail("La carta no esta en tu mano");
        }

        if ("ACTIVE".equals(position)) {
            PokemonInPlay active = isPlayer1 ? state.getP1ActivePokemon() : state.getP2ActivePokemon();
            if (active != null) {
                return ValidationResult.fail("Ya tienes un Pokemon Activo colocado");
            }
            return ValidationResult.ok();
        }

        if ("BENCH".equals(position)) {
            List<PokemonInPlay> bench = isPlayer1 ? state.getP1Bench() : state.getP2Bench();
            if (bench != null && bench.size() >= 5) {
                return ValidationResult.fail("La Banca esta llena (maximo 5 Pokemon)");
            }
            return ValidationResult.ok();
        }

        return ValidationResult.fail("Posicion invalida: " + position);
    }

    public ValidationResult validateReadyToStart(GameStateSnapshot state, boolean isPlayer1) {
        PokemonInPlay active = isPlayer1 ? state.getP1ActivePokemon() : state.getP2ActivePokemon();
        if (active == null) {
            return ValidationResult.fail("Debes colocar al menos 1 Pokemon Activo antes de comenzar");
        }
        return ValidationResult.ok();
    }
}
