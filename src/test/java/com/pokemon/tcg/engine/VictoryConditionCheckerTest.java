package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.VictoryResult;
import com.pokemon.tcg.model.enums.VictoryCondition;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.PokemonInPlay;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VictoryConditionCheckerTest {
    private final VictoryConditionChecker checker = new VictoryConditionChecker();

    @Test
    void gana_por_premios() {
        VictoryResult result = checker.check(baseState(new ArrayList<>(), new ArrayList<>(List.of(prize()))));
        assertThat(result.isGameOver()).isTrue();
        assertThat(result.getCondition()).isEqualTo(VictoryCondition.PRIZES);
    }

    @Test
    void gana_por_no_pokemon() {
        GameStateSnapshot state = baseState(new ArrayList<>(List.of(prize())), new ArrayList<>(List.of(prize())));
        state.setP2ActivePokemon(null);
        state.setP2Bench(new ArrayList<>());
        VictoryResult result = checker.check(state);
        assertThat(result.getCondition()).isEqualTo(VictoryCondition.NO_POKEMON);
    }

    @Test
    void sudden_death_si_ambos_sin_pokemon() {
        GameStateSnapshot state = baseState(new ArrayList<>(List.of(prize())), new ArrayList<>(List.of(prize())));
        state.setP1ActivePokemon(null);
        state.setP2ActivePokemon(null);
        state.setP1Bench(new ArrayList<>());
        state.setP2Bench(new ArrayList<>());
        assertThat(checker.check(state).isSuddenDeath()).isTrue();
    }

    @Test
    void deck_out_da_ganador_al_oponente() {
        GameStateSnapshot state = baseState(new ArrayList<>(List.of(prize())), new ArrayList<>(List.of(prize())));
        state.setP1Deck(new ArrayList<>());
        assertThat(checker.checkDeckOut(state, 1L).getCondition()).isEqualTo(VictoryCondition.DECK_OUT);
    }

    @Test
    void no_hay_ganador_si_todo_sigue() {
        assertThat(checker.check(baseState(new ArrayList<>(List.of(prize())), new ArrayList<>(List.of(prize())))).isGameOver()).isFalse();
    }

    private GameStateSnapshot baseState(List<CardData> p1Prizes, List<CardData> p2Prizes) {
        return GameStateSnapshot.builder()
            .player1Id(1L)
            .player2Id(2L)
            .p1Prizes(p1Prizes)
            .p2Prizes(p2Prizes)
            .p1Deck(new ArrayList<>(List.of(CardData.builder().id("c1").build())))
            .p2Deck(new ArrayList<>(List.of(CardData.builder().id("c2").build())))
            .p1ActivePokemon(PokemonInPlay.builder().name("P1").build())
            .p2ActivePokemon(PokemonInPlay.builder().name("P2").build())
            .p1Bench(new ArrayList<>())
            .p2Bench(new ArrayList<>())
            .build();
    }

    private CardData prize() {
        return CardData.builder().id("prize").build();
    }
}
