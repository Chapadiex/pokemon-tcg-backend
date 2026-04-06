package com.pokemon.tcg.engine;

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

class RuleValidatorTest {
    private final RuleValidator validator = new RuleValidator();

    @Test
    void deck_exactamente_60_cartas() {
        assertThat(validator.validateDeckComposition(buildDeck(60, 4)).isValid()).isTrue();
        assertThat(validator.validateDeckComposition(buildDeck(59, 4)).isValid()).isFalse();
    }

    @Test
    void deck_mas_de_4_copias_falla() {
        assertThat(validator.validateDeckComposition(buildDeck(60, 5)).isValid()).isFalse();
    }

    @Test
    void deck_sin_pokemon_basico_falla() {
        List<CardData> cards = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            cards.add(trainer("trainer-" + i, "Trainer " + i, "Item"));
        }
        assertThat(validator.validateDeckComposition(cards).isValid()).isFalse();
    }

    @Test
    void deck_energia_basica_sin_limite() {
        List<CardData> cards = new ArrayList<>();
        cards.add(basicPokemon("basic-1", "Charmander"));
        for (int i = 0; i < 59; i++) {
            cards.add(basicEnergy("energy-" + i, "Fire Energy"));
        }
        assertThat(validator.validateDeckComposition(cards).isValid()).isTrue();
    }

    @Test
    void evolucion_primer_turno_falla() {
        GameStateSnapshot state = baseState();
        state.setIsFirstTurn(true);
        CardData evolution = evolutionCard();
        state.getP1Hand().add(evolution);
        assertThat(validator.validateEvolution(pokemonInPlay("Charmander", 1, false), evolution, state).isValid()).isFalse();
    }

    @Test
    void evolucion_pokemon_recien_colocado_falla() {
        GameStateSnapshot state = baseState();
        CardData evolution = evolutionCard();
        state.getP1Hand().add(evolution);
        assertThat(validator.validateEvolution(pokemonInPlay("Charmander", 0, false), evolution, state).getErrorMessage()).contains("debe esperar 1 turno");
    }

    @Test
    void evolucion_ya_evoluciono_este_turno_falla() {
        GameStateSnapshot state = baseState();
        CardData evolution = evolutionCard();
        state.getP1Hand().add(evolution);
        assertThat(validator.validateEvolution(pokemonInPlay("Charmander", 1, true), evolution, state).getErrorMessage()).contains("ya evoluciono este turno");
    }

    @Test
    void evolucion_nombre_incorrecto_falla() {
        GameStateSnapshot state = baseState();
        CardData evolution = CardData.builder().id("evo").name("Wartortle").supertype("Pokemon").evolvesFrom("Squirtle").build();
        state.getP1Hand().add(evolution);
        assertThat(validator.validateEvolution(pokemonInPlay("Charmander", 1, false), evolution, state).isValid()).isFalse();
    }

    @Test
    void ataque_sin_energia_suficiente_falla() {
        GameStateSnapshot state = baseState();
        PokemonInPlay attacker = pokemonInPlay("Charmander", 1, false);
        attacker.setAttachedEnergies(List.of(EnergyAttached.builder().type("Fire").build()));
        attacker.setAttacks(List.of(AttackData.builder().name("Big Fire").cost(List.of("Fire", "Fire")).build()));
        assertThat(validator.validateAttack(attacker, 0, state).isValid()).isFalse();
    }

    @Test
    void ataque_dormido_falla() {
        PokemonInPlay attacker = attackerWithAttack();
        attacker.setRotationCondition(com.pokemon.tcg.model.enums.RotationCondition.ASLEEP);
        assertThat(validator.validateAttack(attacker, 0, baseState()).isValid()).isFalse();
    }

    @Test
    void ataque_paralizado_falla() {
        PokemonInPlay attacker = attackerWithAttack();
        attacker.setRotationCondition(com.pokemon.tcg.model.enums.RotationCondition.PARALYZED);
        assertThat(validator.validateAttack(attacker, 0, baseState()).isValid()).isFalse();
    }

    @Test
    void retirada_dormido_falla() {
        PokemonInPlay active = pokemonInPlay("Charmander", 1, false);
        active.setRotationCondition(com.pokemon.tcg.model.enums.RotationCondition.ASLEEP);
        assertThat(validator.validateRetreat(active, List.of(EnergyAttached.builder().type("Fire").build()), baseState()).isValid()).isFalse();
    }

    @Test
    void retirada_ya_usada_este_turno_falla() {
        GameStateSnapshot state = baseState();
        state.setRetreatUsedThisTurn(true);
        assertThat(validator.validateRetreat(pokemonInPlay("Charmander", 1, false), List.of(), state).isValid()).isFalse();
    }

    @Test
    void retirada_sin_banca_falla() {
        GameStateSnapshot state = baseState();
        state.setP1Bench(new ArrayList<>());
        PokemonInPlay active = pokemonInPlay("Charmander", 1, false);
        active.setRetreatCost(1);
        assertThat(validator.validateRetreat(active, List.of(EnergyAttached.builder().type("Fire").build()), state).isValid()).isFalse();
    }

    @Test
    void colocar_pokemon_y_entrenador_y_energia_validan() {
        GameStateSnapshot state = baseState();
        CardData basic = basicPokemon("basic-x", "Bulbasaur");
        state.getP1Hand().add(basic);
        assertThat(validator.validatePlacePokemon(basic, "BENCH", state).isValid()).isTrue();

        CardData supporter = trainer("sup-1", "Professor Oak", "Supporter");
        state.setSupporterPlayedThisTurn(true);
        assertThat(validator.validatePlayTrainer(supporter, state).isValid()).isFalse();

        state.setEnergyAttachedThisTurn(false);
        assertThat(validator.validateAttachEnergy(basicEnergy("en", "Fire Energy"), pokemonInPlay("Charmander", 1, false), state).isValid()).isTrue();
    }

    private List<CardData> buildDeck(int size, int duplicateCount) {
        List<CardData> cards = new ArrayList<>();
        cards.add(basicPokemon("basic-1", "Charmander"));
        for (int i = 1; i < size - duplicateCount; i++) {
            cards.add(trainer("trainer-" + i, "Trainer " + i, "Item"));
        }
        for (int i = 0; i < duplicateCount; i++) {
            cards.add(trainer("dup-" + i, "Rare Candy", "Item"));
        }
        return cards;
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

    private CardData evolutionCard() {
        return CardData.builder().id("charmeleon").name("Charmeleon").supertype("Pok\u00e9mon").subtypes(List.of("Stage 1")).evolvesFrom("Charmander").build();
    }

    private PokemonInPlay pokemonInPlay(String name, int turns, boolean evolvedThisTurn) {
        PokemonInPlay pokemon = PokemonInPlay.builder()
            .cardId(name.toLowerCase())
            .name(name)
            .hp(60)
            .turnsSinceInPlay(turns)
            .evolvedThisTurn(evolvedThisTurn)
            .attachedEnergies(new ArrayList<>())
            .attacks(new ArrayList<>())
            .build();
        pokemon.setRetreatCost(1);
        pokemon.setDamageCounters(0);
        return pokemon;
    }

    private PokemonInPlay attackerWithAttack() {
        PokemonInPlay attacker = pokemonInPlay("Charmander", 1, false);
        attacker.setAttachedEnergies(List.of(EnergyAttached.builder().type("Fire").build()));
        attacker.setAttacks(List.of(AttackData.builder().name("Scratch").cost(List.of("Fire")).build()));
        return attacker;
    }

    private GameStateSnapshot baseState() {
        return GameStateSnapshot.builder()
            .player1Id(1L)
            .player2Id(2L)
            .currentTurnPlayerId(1L)
            .turnNumber(1)
            .isFirstTurn(false)
            .status(GameStatus.ACTIVE)
            .p1Hand(new ArrayList<>())
            .p2Hand(new ArrayList<>())
            .p1Bench(new ArrayList<>(List.of(pokemonInPlay("BenchMon", 1, false))))
            .p2Bench(new ArrayList<>())
            .p1Deck(new ArrayList<>())
            .p2Deck(new ArrayList<>())
            .p1Prizes(new ArrayList<>(List.of(CardData.builder().id("prize").build())))
            .p2Prizes(new ArrayList<>(List.of(CardData.builder().id("prize2").build())))
            .p1Discard(new ArrayList<>())
            .p2Discard(new ArrayList<>())
            .energyAttachedThisTurn(false)
            .supporterPlayedThisTurn(false)
            .stadiumPlayedThisTurn(false)
            .retreatUsedThisTurn(false)
            .build();
    }
}
