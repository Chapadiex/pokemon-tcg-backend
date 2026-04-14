package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.ActionResult;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.model.enums.StatusCondition;
import com.pokemon.tcg.model.game.AttackData;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.EnergyAttached;
import com.pokemon.tcg.model.game.PokemonInPlay;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EngineCoverageTest {
    private final RuleValidator validator = new RuleValidator();
    private final StatusEffectManager statusFx = new StatusEffectManager();
    private final GameEngine engine = new GameEngine(new RuleValidator(), new DamageCalculator(), statusFx, new VictoryConditionChecker(), new AttackEffectParser(statusFx));

    @Test
    void validator_cubre_hand_place_attach_trainer_retreat_y_attack_paths() {
        GameStateSnapshot state = state();

        assertThat(validator.handHasBasicPokemon(List.of(basicPokemon("b1", "Charmander")))).isTrue();
        assertThat(validator.handHasBasicPokemon(List.of(trainer("t1", "Potion", "Item")))).isFalse();

        assertThat(validator.validatePlacePokemon(trainer("t2", "Potion", "Item"), "BENCH", state).isValid()).isFalse();
        CardData stage1 = CardData.builder().id("s1").name("Charmeleon").supertype("Pok\u00e9mon").subtypes(List.of("Stage 1")).build();
        assertThat(validator.validatePlacePokemon(stage1, "BENCH", state).isValid()).isFalse();

        state.setP1Bench(new ArrayList<>(List.of(mon("b1"), mon("b2"), mon("b3"), mon("b4"), mon("b5"))));
        CardData basic = basicPokemon("b6", "Bulbasaur");
        state.getP1Hand().add(basic);
        assertThat(validator.validatePlacePokemon(basic, "BENCH", state).isValid()).isFalse();

        state = state();
        assertThat(validator.validatePlacePokemon(basicPokemon("missing", "Squirtle"), "BENCH", state).isValid()).isFalse();

        assertThat(validator.validateAttachEnergy(trainer("x", "Potion", "Item"), mon("target"), state).isValid()).isFalse();
        state.setEnergyAttachedThisTurn(true);
        assertThat(validator.validateAttachEnergy(basicEnergy("e1", "Fire Energy"), mon("target"), state).isValid()).isFalse();
        state.setEnergyAttachedThisTurn(false);
        assertThat(validator.validateAttachEnergy(basicEnergy("e1", "Fire Energy"), null, state).isValid()).isFalse();

        CardData stadium = trainer("stadium", "Viridian Forest", "Stadium");
        state.setStadiumPlayedThisTurn(true);
        assertThat(validator.validatePlayTrainer(stadium, state).isValid()).isFalse();
        state.setStadiumPlayedThisTurn(false);
        assertThat(validator.validatePlayTrainer(basicEnergy("not-trainer", "Fire Energy"), state).isValid()).isFalse();

        PokemonInPlay active = mon("Active");
        active.setRotationCondition(com.pokemon.tcg.model.enums.RotationCondition.PARALYZED);
        assertThat(validator.validateRetreat(active, List.of(EnergyAttached.builder().type("Fire").build()), state).isValid()).isFalse();
        active.setRotationCondition(null);
        active.setRetreatCost(2);
        assertThat(validator.validateRetreat(active, List.of(EnergyAttached.builder().type("Fire").build()), state).isValid()).isFalse();

        state.setIsFirstTurn(true);
        state.setCurrentTurnPlayerId(1L);
        assertThat(validator.validateAttack(monWithAttack(), 0, state).isValid()).isFalse();
        state.setIsFirstTurn(false);
        assertThat(validator.validateAttack(null, 0, state).isValid()).isFalse();
        PokemonInPlay noAttacks = mon("NoAttacks");
        noAttacks.setAttachedEnergies(List.of(EnergyAttached.builder().type("Fire").build()));
        noAttacks.setAttacks(new ArrayList<>());
        assertThat(validator.validateAttack(noAttacks, 0, state).isValid()).isFalse();

        PokemonInPlay colorlessAttacker = mon("Colorless");
        colorlessAttacker.setAttachedEnergies(List.of(
            EnergyAttached.builder().type("Fire").build(),
            EnergyAttached.builder().type("Water").build()
        ));
        colorlessAttacker.setAttacks(List.of(AttackData.builder().name("Colorless Hit").cost(List.of("Colorless", "Colorless")).build()));
        assertThat(validator.validateAttack(colorlessAttacker, 0, state).isValid()).isTrue();

        PokemonInPlay specificEnergyAttacker = mon("Specific");
        specificEnergyAttacker.setAttachedEnergies(List.of(EnergyAttached.builder().type("Fire").build()));
        specificEnergyAttacker.setAttacks(List.of(AttackData.builder().name("Water Hit").cost(List.of("Water")).build()));
        assertThat(validator.validateAttack(specificEnergyAttacker, 0, state).isValid()).isFalse();
    }

    @Test
    void validator_cubre_evolution_paths_restantes() {
        GameStateSnapshot state = state();
        CardData evo = CardData.builder().id("charmeleon").name("Charmeleon").supertype("Pok\u00e9mon").subtypes(List.of("Stage 1")).evolvesFrom("Charmander").build();
        state.setIsFirstTurn(false);
        assertThat(validator.validateEvolution(mon("Charmander"), evo, state).isValid()).isFalse();
    }

    @Test
    void game_engine_cubre_start_end_invalid_and_game_over() {
        GameStateSnapshot state = state();
        ActionResult firstTurn = engine.startTurn(state, () -> false);
        assertThat(firstTurn.getEvents()).isEmpty();

        engine.endTurn(state);
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(2L);

        GameStateSnapshot invalidAttackState = state();
        invalidAttackState.setIsFirstTurn(false);
        invalidAttackState.getP1ActivePokemon().setAttachedEnergies(new ArrayList<>());
        ActionResult invalid = engine.executeAttack(invalidAttackState, 0, () -> false);
        assertThat(invalid.isSuccess()).isFalse();

        GameStateSnapshot knockoutState = state();
        knockoutState.setIsFirstTurn(false);
        knockoutState.getP2Prizes().clear();
        knockoutState.getP1ActivePokemon().setAttacks(List.of(AttackData.builder().name("Hit").damage("100").cost(List.of("Fire")).build()));
        knockoutState.getP1ActivePokemon().setAttachedEnergies(List.of(EnergyAttached.builder().type("Fire").build()));
        ActionResult gameOver = engine.executeAttack(knockoutState, 0, () -> false);
        assertThat(gameOver.isGameOver()).isTrue();
        assertThat(gameOver.getEvents().stream().anyMatch(event -> event.getType().name().equals("GAME_OVER"))).isTrue();
    }

    private GameStateSnapshot state() {
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
            .p1ActivePokemon(monWithAttack())
            .p2ActivePokemon(mon("Opponent"))
            .p1Bench(new ArrayList<>(List.of(mon("Bench"))))
            .p2Bench(new ArrayList<>())
            .energyAttachedThisTurn(false)
            .supporterPlayedThisTurn(false)
            .stadiumPlayedThisTurn(false)
            .retreatUsedThisTurn(false)
            .build();
    }

    private CardData basicPokemon(String id, String name) {
        return CardData.builder().id(id).name(name).supertype("Pok\u00e9mon").subtypes(List.of("Basic")).build();
    }

    private CardData basicEnergy(String id, String name) {
        return CardData.builder().id(id).name(name).supertype("Energy").subtypes(List.of("Basic")).build();
    }

    private CardData trainer(String id, String name, String subtype) {
        return CardData.builder().id(id).name(name).supertype("Trainer").subtypes(List.of(subtype)).build();
    }

    private PokemonInPlay mon(String name) {
        return PokemonInPlay.builder().cardId(name.toLowerCase()).name(name).hp(60).damageCounters(0).attachedEnergies(new ArrayList<>()).attacks(new ArrayList<>()).turnsSinceInPlay(1).evolvedThisTurn(false).build();
    }

    private PokemonInPlay monWithAttack() {
        PokemonInPlay pokemon = mon("Attacker");
        pokemon.setAttachedEnergies(List.of(EnergyAttached.builder().type("Fire").build()));
        pokemon.setAttacks(List.of(AttackData.builder().name("Scratch").damage("30").cost(List.of("Fire")).build()));
        return pokemon;
    }
}
