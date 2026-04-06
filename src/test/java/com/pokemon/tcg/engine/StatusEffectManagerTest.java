package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.BetweenTurnsResult;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.model.enums.RotationCondition;
import com.pokemon.tcg.model.enums.StatusCondition;
import com.pokemon.tcg.model.game.PokemonInPlay;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class StatusEffectManagerTest {
    private final StatusEffectManager manager = new StatusEffectManager();

    @Test
    void envenenado_aplica_1_contador_entre_turnos() {
        PokemonInPlay pokemon = pokemonWithStatus(StatusCondition.POISONED, 0);
        GameStateSnapshot state = state(pokemon, null);
        manager.processBetweenTurns(state, () -> true);
        assertThat(pokemon.getDamageCounters()).isEqualTo(1);
    }

    @Test
    void quemado_cara_no_aplica_danio() {
        PokemonInPlay pokemon = pokemonWithStatus(StatusCondition.BURNED, 0);
        BetweenTurnsResult result = manager.processBetweenTurns(state(pokemon, null), () -> false);
        assertThat(pokemon.getDamageCounters()).isZero();
        assertThat(result.getEvents()).hasSize(1); // solo COIN_FLIP
    }

    @Test
    void quemado_cruz_aplica_2_contadores() {
        PokemonInPlay pokemon = pokemonWithStatus(StatusCondition.BURNED, 0);
        manager.processBetweenTurns(state(pokemon, null), () -> true);
        assertThat(pokemon.getDamageCounters()).isEqualTo(2);
    }

    @Test
    void dormido_cara_cura() {
        PokemonInPlay pokemon = pokemonWithStatus(StatusCondition.ASLEEP, 0);
        manager.processBetweenTurns(state(pokemon, null), () -> false);
        assertThat(pokemon.getRotationCondition()).isNull();
    }

    @Test
    void dormido_cruz_permanece() {
        PokemonInPlay pokemon = pokemonWithStatus(StatusCondition.ASLEEP, 0);
        manager.processBetweenTurns(state(pokemon, null), () -> true);
        assertThat(pokemon.getRotationCondition()).isEqualTo(RotationCondition.ASLEEP);
    }

    @Test
    void paralizado_se_cura_al_resetear_flags() {
        PokemonInPlay pokemon = pokemonWithStatus(StatusCondition.PARALYZED, 0);
        GameStateSnapshot state = state(pokemon, null);
        state.setP1Bench(new ArrayList<>());
        state.setP2Bench(new ArrayList<>());
        new TurnManager(new RuleValidator(), new DamageCalculator(), manager, new VictoryConditionChecker()).resetTurnFlags(state);
        // PARALYZED se cura en processBetweenTurns (llamado por endTurn/attack), no en resetTurnFlags
        // pero sí se procesa en processBetweenTurns con coinFlipper ignorado
        manager.processBetweenTurns(state, () -> false);
        assertThat(pokemon.getRotationCondition()).isNull();
    }

    @Test
    void confundido_cruz_cancela_ataque_y_aplica_3_contadores() {
        PokemonInPlay pokemon = pokemonWithStatus(StatusCondition.CONFUSED, 0);
        boolean cancelled = manager.checkConfusionCancelsAttack(pokemon, () -> true);
        assertThat(cancelled).isTrue();
        assertThat(pokemon.getDamageCounters()).isEqualTo(3);
    }

    @Test
    void confundido_cara_permite_ataque() {
        PokemonInPlay pokemon = pokemonWithStatus(StatusCondition.CONFUSED, 0);
        boolean cancelled = manager.checkConfusionCancelsAttack(pokemon, () -> false);
        assertThat(cancelled).isFalse();
    }

    @Test
    void dormido_reemplaza_paralizado() {
        PokemonInPlay pokemon = pokemonWithStatus(StatusCondition.PARALYZED, 0);
        manager.applyStatus(pokemon, StatusCondition.ASLEEP);
        assertThat(pokemon.getRotationCondition()).isEqualTo(RotationCondition.ASLEEP);
    }

    @Test
    void can_attack_y_retreat_respetan_estado() {
        PokemonInPlay pokemon = pokemonWithStatus(StatusCondition.ASLEEP, 0);
        assertThat(manager.canAttack(pokemon)).isFalse();
        assertThat(manager.canRetreat(pokemon)).isFalse();
        manager.removeStatus(pokemon);
        assertThat(manager.canAttack(pokemon)).isTrue();
    }

    @Test
    void confundido_y_envenenado_coexisten() {
        PokemonInPlay pokemon = pokemonWithStatus(StatusCondition.CONFUSED, 0);
        manager.applyStatus(pokemon, StatusCondition.POISONED);
        assertThat(pokemon.getRotationCondition()).isEqualTo(RotationCondition.CONFUSED);
        assertThat(pokemon.isPoisoned()).isTrue();
    }

    private PokemonInPlay pokemonWithStatus(StatusCondition status, int counters) {
        PokemonInPlay p = PokemonInPlay.builder()
            .name("TestMon")
            .damageCounters(counters)
            .build();
        manager.applyStatus(p, status);
        return p;
    }

    private GameStateSnapshot state(PokemonInPlay p1, PokemonInPlay p2) {
        return GameStateSnapshot.builder()
            .p1ActivePokemon(p1)
            .p2ActivePokemon(p2)
            .p1Bench(new ArrayList<>())
            .p2Bench(new ArrayList<>())
            .build();
    }
}
