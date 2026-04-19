package com.pokemon.tcg.engine.model;

import com.pokemon.tcg.model.enums.VictoryCondition;
import com.pokemon.tcg.model.game.PokemonInPlay;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

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
    private List<PokemonInPlay> benchOptions;

    public static KOReplacementResult waitingForPlayer(Long ownerId, List<PokemonInPlay> benchOptions) {
        return new KOReplacementResult(true, false, false, ownerId, null, null, null, benchOptions);
    }

    public static KOReplacementResult autoResolved(PokemonInPlay p) {
        return new KOReplacementResult(false, true, false, null, null, null, p, null);
    }

    public static KOReplacementResult gameOver(Long winnerId, VictoryCondition condition) {
        return new KOReplacementResult(false, false, true, null, winnerId, condition, null, null);
    }
}
