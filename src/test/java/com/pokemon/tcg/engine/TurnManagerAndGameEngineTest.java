package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.ActionResult;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.KnockoutResult;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.EnergyAttached;
import com.pokemon.tcg.model.game.PokemonInPlay;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TurnManagerAndGameEngineTest {
    private final RuleValidator validator = new RuleValidator();
    private final DamageCalculator damageCalculator = new DamageCalculator();
    private final StatusEffectManager statusEffectManager = new StatusEffectManager();
    private final VictoryConditionChecker victoryChecker = new VictoryConditionChecker();
    private final TurnManager turnManager = new TurnManager(validator, damageCalculator, statusEffectManager, victoryChecker);

    @Test
    void start_turn_roba_carta() {
        GameStateSnapshot state = state();
        state.setIsFirstTurn(false);
        state.setTurnNumber(1);
        ActionResult result = turnManager.startTurn(state, () -> false);
        assertThat(result.isSuccess()).isTrue();
        assertThat(state.getP1Hand()).hasSize(1);
        assertThat(result.getEvents()).hasSize(1);
    }

    @Test
    void start_turn_detecta_deck_out() {
        GameStateSnapshot state = state();
        state.setIsFirstTurn(false);
        state.setTurnNumber(1);
        state.setP1Deck(new ArrayList<>());
        ActionResult result = turnManager.startTurn(state, () -> false);
        assertThat(result.isGameOver()).isTrue();
        assertThat(result.getWinnerId()).isEqualTo(2L);
    }

    @Test
    void reset_y_switch_turn_actualizan_estado() {
        GameStateSnapshot state = state();
        state.getP1ActivePokemon().setTurnsSinceInPlay(0);
        state.getP1ActivePokemon().setEvolvedThisTurn(true);
        turnManager.resetTurnFlags(state);
        turnManager.switchTurn(state);
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(2L);
        assertThat(state.getTurnNumber()).isEqualTo(1);
        assertThat(state.getP1ActivePokemon().getTurnsSinceInPlay()).isEqualTo(1);
        assertThat(state.getP1ActivePokemon().getEvolvedThisTurn()).isFalse();
    }

    @Test
    void knockout_mueve_cartas_y_toma_premio() {
        GameStateSnapshot state = state();
        PokemonInPlay knocked = state.getP2ActivePokemon();
        knocked.setAttachedEnergies(List.of(EnergyAttached.builder().cardId("energy-1").name("Fire Energy").build()));
        KnockoutResult result = turnManager.processKnockout(knocked, false, state);
        assertThat(state.getP2Discard()).hasSize(2);
        assertThat(state.getP1Prizes()).hasSize(0);
        assertThat(result.getEvents()).isNotEmpty();
    }

    @Test
    void game_engine_ejecuta_ataque() {
        GameEngine engine = new GameEngine(validator, damageCalculator, statusEffectManager, victoryChecker, new com.pokemon.tcg.engine.AttackEffectParser(statusEffectManager));
        GameStateSnapshot state = state();
        state.setIsFirstTurn(false);
        state.getP1ActivePokemon().setAttacks(List.of(com.pokemon.tcg.model.game.AttackData.builder().name("Scratch").damage("30").cost(List.of("Fire")).build()));
        state.getP1ActivePokemon().setAttachedEnergies(List.of(EnergyAttached.builder().type("Fire").build()));
        ActionResult result = engine.executeAttack(state, 0, () -> false);
        assertThat(result.isSuccess()).isTrue();
        assertThat(state.getP2ActivePokemon().getDamageCounters()).isEqualTo(3);
    }

    @Test
    void game_engine_falla_si_confusion_cancela() {
        GameEngine engine = new GameEngine(validator, damageCalculator, statusEffectManager, victoryChecker, new com.pokemon.tcg.engine.AttackEffectParser(statusEffectManager));
        GameStateSnapshot state = state();
        state.setIsFirstTurn(false);
        state.getP1ActivePokemon().setRotationCondition(com.pokemon.tcg.model.enums.RotationCondition.CONFUSED);
        state.getP1ActivePokemon().setAttacks(List.of(com.pokemon.tcg.model.game.AttackData.builder().name("Scratch").damage("30").cost(List.of("Fire")).build()));
        state.getP1ActivePokemon().setAttachedEnergies(List.of(EnergyAttached.builder().type("Fire").build()));
        ActionResult result = engine.executeAttack(state, 0, () -> true);
        // Confusion cancels the attack but the action itself is valid (success=true)
        assertThat(result.isSuccess()).isTrue();
        assertThat(state.getP1ActivePokemon().getDamageCounters()).isEqualTo(3);
    }

    private GameStateSnapshot state() {
        PokemonInPlay p1 = PokemonInPlay.builder().cardId("p1").name("Charmander").hp(60).damageCounters(0).attachedEnergies(new ArrayList<>()).attacks(new ArrayList<>()).turnsSinceInPlay(0).evolvedThisTurn(false).build();
        PokemonInPlay p2 = PokemonInPlay.builder().cardId("p2").name("Squirtle").hp(60).damageCounters(0).attachedEnergies(new ArrayList<>()).attacks(new ArrayList<>()).turnsSinceInPlay(0).evolvedThisTurn(false).build();
        return GameStateSnapshot.builder()
            .player1Id(1L)
            .player2Id(2L)
            .currentTurnPlayerId(1L)
            .turnNumber(0)
            .isFirstTurn(true)
            .status(GameStatus.ACTIVE)
            .p1Deck(new ArrayList<>(List.of(CardData.builder().id("draw-1").build())))
            .p2Deck(new ArrayList<>(List.of(CardData.builder().id("draw-2").build())))
            .p1Hand(new ArrayList<>())
            .p2Hand(new ArrayList<>())
            .p1Prizes(new ArrayList<>(List.of(CardData.builder().id("prize-1").build())))
            .p2Prizes(new ArrayList<>(List.of(CardData.builder().id("prize-2").build())))
            .p1Discard(new ArrayList<>())
            .p2Discard(new ArrayList<>())
            .p1ActivePokemon(p1)
            .p2ActivePokemon(p2)
            .p1Bench(new ArrayList<>())
            .p2Bench(new ArrayList<>())
            .energyAttachedThisTurn(false)
            .supporterPlayedThisTurn(false)
            .stadiumPlayedThisTurn(false)
            .retreatUsedThisTurn(false)
            .build();
    }
}
