package com.pokemon.tcg.engine.model;

import com.pokemon.tcg.model.enums.VictoryCondition;
import com.pokemon.tcg.model.game.PokemonInPlay;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KOReplacementResult {
    private boolean      waitingForPlayer;
    private boolean      autoResolved;
    private boolean      gameOver;
    private Long         ownerId;
    private Long         winnerId;
    private VictoryCondition victoryCondition;
    private PokemonInPlay autoChosenPokemon;

    public static KOReplacementResult waitingForPlayer(Long ownerId) {
        return new KOReplacementResult(true, false, false, ownerId, null, null, null);
    }

    public static KOReplacementResult autoResolved(PokemonInPlay p) {
        return new KOReplacementResult(false, true, false, null, null, null, p);
    }

    public static KOReplacementResult gameOver(Long winnerId, VictoryCondition condition) {
        return new KOReplacementResult(false, false, true, null, winnerId, condition, null);
    }
}
