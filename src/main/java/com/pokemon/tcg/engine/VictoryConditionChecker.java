package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.VictoryResult;
import com.pokemon.tcg.model.enums.VictoryCondition;
import com.pokemon.tcg.model.game.CardData;

import java.util.List;

@org.springframework.stereotype.Component
public class VictoryConditionChecker {

    public VictoryResult check(GameStateSnapshot state) {
        if (state.getP1Prizes() != null && state.getP1Prizes().isEmpty()) {
            return VictoryResult.win(state.getPlayer1Id(), VictoryCondition.PRIZES);
        }
        if (state.getP2Prizes() != null && state.getP2Prizes().isEmpty()) {
            return VictoryResult.win(state.getPlayer2Id(), VictoryCondition.PRIZES);
        }

        boolean p1NoPokemon = state.getP1ActivePokemon() == null && (state.getP1Bench() == null || state.getP1Bench().isEmpty());
        boolean p2NoPokemon = state.getP2ActivePokemon() == null && (state.getP2Bench() == null || state.getP2Bench().isEmpty());

        if (p1NoPokemon && p2NoPokemon) {
            return VictoryResult.suddenDeath();
        }
        if (p1NoPokemon) {
            return VictoryResult.win(state.getPlayer2Id(), VictoryCondition.NO_POKEMON);
        }
        if (p2NoPokemon) {
            return VictoryResult.win(state.getPlayer1Id(), VictoryCondition.NO_POKEMON);
        }
        return VictoryResult.noWinner();
    }

    public VictoryResult checkDeckOut(GameStateSnapshot state, Long playerId) {
        List<CardData> deck = playerId.equals(state.getPlayer1Id()) ? state.getP1Deck() : state.getP2Deck();
        if (deck == null || deck.isEmpty()) {
            Long opponent = playerId.equals(state.getPlayer1Id()) ? state.getPlayer2Id() : state.getPlayer1Id();
            return VictoryResult.win(opponent, VictoryCondition.DECK_OUT);
        }
        return VictoryResult.noWinner();
    }
}
