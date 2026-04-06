package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.PokemonInPlay;
import com.pokemon.tcg.model.game.SetupPhaseState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class MulliganProcessorTest {
    private final RuleValidator ruleValidator = new RuleValidator();
    private final MulliganProcessor processor = new MulliganProcessor(ruleValidator, new Random(1));

    @Test
    void ambosConBasicoRetornaBothReady() {
        GameStateSnapshot state = stateWithHands(List.of(basicPokemon("a1")), List.of(basicPokemon("b1")));

        var result = processor.processMulliganRound(state);

        assertThat(result.isBothReady()).isTrue();
        assertThat(result.isContinueLoop()).isFalse();
        assertThat(state.getSetupState().getCurrentStep()).isEqualTo(SetupPhaseState.SetupStep.INITIAL_PLACEMENT);
    }

    @Test
    void player1SinBasicoPlayer2PuedeRobarExtra() {
        GameStateSnapshot state = stateWithHands(List.of(stage1Pokemon("a1")), List.of(basicPokemon("b1")));

        var result = processor.processMulliganRound(state);

        assertThat(result.isBothReady()).isFalse();
        assertThat(result.isP2CanDrawExtra()).isTrue();
        assertThat(result.isP1CanDrawExtra()).isFalse();
        assertThat(state.getSetupState().getPlayer1MulliganCount()).isEqualTo(1);
        assertThat(state.getSetupState().isPlayer2CanDrawExtra()).isTrue();
        assertThat(state.getP1Hand()).hasSize(7);
    }

    @Test
    void player2SinBasicoPlayer1PuedeRobarExtra() {
        GameStateSnapshot state = stateWithHands(List.of(basicPokemon("a1")), List.of(stage1Pokemon("b1")));

        var result = processor.processMulliganRound(state);

        assertThat(result.isP1CanDrawExtra()).isTrue();
        assertThat(state.getSetupState().getPlayer2MulliganCount()).isEqualTo(1);
    }

    @Test
    void ambosSinBasicoNadieRobaExtra() {
        GameStateSnapshot state = stateWithHands(List.of(stage1Pokemon("a1")), List.of(stage1Pokemon("b1")));
        int p1DeckBefore = state.getP1Deck().size();
        int p1HandBefore = state.getP1Hand().size();

        var result = processor.processMulliganRound(state);

        assertThat(result.isP1CanDrawExtra()).isFalse();
        assertThat(result.isP2CanDrawExtra()).isFalse();
        assertThat(result.isContinueLoop()).isTrue();
        assertThat(state.getSetupState().getPlayer1MulliganCount()).isEqualTo(1);
        assertThat(state.getSetupState().getPlayer2MulliganCount()).isEqualTo(1);
        assertThat(state.getP1Hand()).hasSize(7);
        assertThat(state.getP1Deck()).hasSize(p1DeckBefore + p1HandBefore - 7);
    }

    @Test
    void applyExtraDrawRobaUnaCartaAlJugadorCorrecto() {
        GameStateSnapshot state = stateWithHands(List.of(basicPokemon("a1")), List.of(stage1Pokemon("b1")));
        processor.processMulliganRound(state);
        int handBefore = state.getP1Hand().size();

        processor.applyExtraDraw(state, 1L);

        assertThat(state.getP1Hand()).hasSize(handBefore + 1);
        assertThat(state.getSetupState().isPlayer1CanDrawExtra()).isFalse();
        assertThat(state.getSetupState().getCurrentStep()).isEqualTo(SetupPhaseState.SetupStep.MULLIGAN_CHECK);
    }

    @Test
    void colocacionInicialValidaPokemonBasicoEnActivo() {
        InitialPlacementValidator validator = new InitialPlacementValidator();
        GameStateSnapshot state = emptyBaseState();
        CardData card = basicPokemon("c1");
        state.getP1Hand().add(card);

        var result = validator.validatePlaceDuringSetup(card, "ACTIVE", state, true);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void colocacionInicialFallaSiNoEsBasico() {
        InitialPlacementValidator validator = new InitialPlacementValidator();
        GameStateSnapshot state = emptyBaseState();
        CardData card = stage1Pokemon("c1");
        state.getP1Hand().add(card);

        var result = validator.validatePlaceDuringSetup(card, "ACTIVE", state, true);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("basico");
    }

    @Test
    void colocacionInicialFallaBancaLlena() {
        InitialPlacementValidator validator = new InitialPlacementValidator();
        GameStateSnapshot state = emptyBaseState();
        for (int i = 0; i < 5; i++) {
            state.getP1Bench().add(PokemonInPlay.fromCard(basicPokemon("bench-" + i)));
        }
        CardData card = basicPokemon("c1");
        state.getP1Hand().add(card);

        var result = validator.validatePlaceDuringSetup(card, "BENCH", state, true);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("llena");
    }

    private GameStateSnapshot stateWithHands(List<CardData> p1Hand, List<CardData> p2Hand) {
        GameStateSnapshot state = emptyBaseState();
        state.setP1Hand(new ArrayList<>(p1Hand));
        state.setP2Hand(new ArrayList<>(p2Hand));
        state.setP1Deck(deckWithBasics("p1-deck-", 8));
        state.setP2Deck(deckWithBasics("p2-deck-", 8));
        return state;
    }

    private GameStateSnapshot emptyBaseState() {
        return GameStateSnapshot.builder()
            .player1Id(1L)
            .player2Id(2L)
            .status(com.pokemon.tcg.model.enums.GameStatus.SETUP)
            .p1Deck(new ArrayList<>())
            .p1Hand(new ArrayList<>())
            .p1Prizes(new ArrayList<>())
            .p1Discard(new ArrayList<>())
            .p1Bench(new ArrayList<>())
            .p2Deck(new ArrayList<>())
            .p2Hand(new ArrayList<>())
            .p2Prizes(new ArrayList<>())
            .p2Discard(new ArrayList<>())
            .p2Bench(new ArrayList<>())
            .setupState(SetupPhaseState.initial())
            .build();
    }

    private List<CardData> deckWithBasics(String prefix, int count) {
        List<CardData> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cards.add(basicPokemon(prefix + i));
        }
        return cards;
    }

    private CardData basicPokemon(String id) {
        return CardData.builder().id(id).name(id).supertype("Pokémon").subtypes(List.of("Basic")).hp(60).types(List.of("Fire")).attacks(List.of()).build();
    }

    private CardData stage1Pokemon(String id) {
        return CardData.builder().id(id).name(id).supertype("Pokémon").subtypes(List.of("Stage 1")).hp(80).types(List.of("Fire")).attacks(List.of()).build();
    }
}