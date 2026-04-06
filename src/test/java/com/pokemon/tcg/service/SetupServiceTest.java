package com.pokemon.tcg.service;

import com.pokemon.tcg.engine.InitialPlacementValidator;
import com.pokemon.tcg.engine.MulliganProcessor;
import com.pokemon.tcg.engine.RuleValidator;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.model.entity.CardInDeck;
import com.pokemon.tcg.model.entity.Deck;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.entity.Player;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.PokemonInPlay;
import com.pokemon.tcg.model.game.SetupPhaseState;
import com.pokemon.tcg.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetupServiceTest {
    @Mock
    private GameRepository gameRepository;
    @Mock
    private CardService cardService;
    @Mock
    private GameStateMapper gameStateMapper;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private SetupService setupService;

    @BeforeEach
    void setUp() {
        setupService = new SetupService(
            gameRepository,
            cardService,
            new MulliganProcessor(new RuleValidator(), new Random(1)),
            new InitialPlacementValidator(),
            gameStateMapper,
            messagingTemplate,
            new Random() {
                @Override
                public boolean nextBoolean() {
                    return true;
                }
            }
        );
    }

    @Test
    void initializeSetupWhenBothHandsHaveBasicsMovesToInitialPlacement() {
        Game game = buildGame();
        when(gameRepository.findById(99L)).thenReturn(Optional.of(game));
        when(cardService.preloadDeck(any())).thenReturn(basicDeck("p1-"), basicDeck("p2-"));

        setupService.initializeSetup(99L);

        ArgumentCaptor<GameStateSnapshot> captor = ArgumentCaptor.forClass(GameStateSnapshot.class);
        verify(gameStateMapper, atLeastOnce()).saveSnapshot(eq(game), captor.capture());
        GameStateSnapshot lastSnapshot = captor.getAllValues().get(captor.getAllValues().size() - 1);

        assertThat(lastSnapshot.getStatus()).isEqualTo(GameStatus.SETUP);
        assertThat(lastSnapshot.getSetupState().getCurrentStep()).isEqualTo(SetupPhaseState.SetupStep.INITIAL_PLACEMENT);
        assertThat(lastSnapshot.getP1Hand()).hasSize(7);
        assertThat(lastSnapshot.getP1Prizes()).hasSize(6);
        verify(gameRepository).save(game);
    }

    @Test
    void playerReadyToStartOnlyFinishesWhenBothPlayersAreReady() {
        Game game = buildGame();
        GameStateSnapshot snapshot = placementSnapshot(false, false);
        when(gameRepository.findById(99L)).thenReturn(Optional.of(game));
        when(gameStateMapper.loadSnapshot(game)).thenReturn(snapshot);

        setupService.playerReadyToStart(99L, 1L);

        assertThat(snapshot.getSetupState().isPlayer1ReadyToStart()).isTrue();
        assertThat(snapshot.getStatus()).isEqualTo(GameStatus.SETUP);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/game/99"), any(Map.class));
    }

    @Test
    void playerReadyToStartWhenBothPlayersReadyTransitionsToActive() {
        Game game = buildGame();
        GameStateSnapshot snapshot = placementSnapshot(true, false);
        when(gameRepository.findById(99L)).thenReturn(Optional.of(game));
        when(gameStateMapper.loadSnapshot(game)).thenReturn(snapshot);

        setupService.playerReadyToStart(99L, 2L);

        assertThat(snapshot.getStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(snapshot.getCurrentTurnPlayerId()).isEqualTo(1L);
        assertThat(snapshot.getTurnNumber()).isEqualTo(1);
        assertThat(snapshot.getSetupState().getCurrentStep()).isEqualTo(SetupPhaseState.SetupStep.COMPLETE);
        assertThat(game.getStartedAt()).isNotNull();
        verify(gameRepository).save(game);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/game/99"), any(Map.class));
    }

    private Game buildGame() {
        Player player1 = Player.builder().id(1L).username("red").build();
        Player player2 = Player.builder().id(2L).username("blue").build();
        Deck deck1 = Deck.builder().id(11L).player(player1).name("Deck One").cards(deckCards()).build();
        Deck deck2 = Deck.builder().id(12L).player(player2).name("Deck Two").cards(deckCards()).build();
        return Game.builder()
            .id(99L)
            .player1(player1)
            .player2(player2)
            .player1Deck(deck1)
            .player2Deck(deck2)
            .status(GameStatus.WAITING)
            .turnNumber(0)
            .isFirstTurn(true)
            .build();
    }

    private List<CardInDeck> deckCards() {
        List<CardInDeck> cards = new ArrayList<>();
        cards.add(CardInDeck.builder().cardId("basic-a").quantity(30).build());
        cards.add(CardInDeck.builder().cardId("basic-b").quantity(30).build());
        return cards;
    }

    private List<CardData> basicDeck(String prefix) {
        List<CardData> cards = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            cards.add(CardData.builder()
                .id(prefix + i)
                .name(prefix + i)
                .supertype("Pokémon")
                .subtypes(List.of("Basic"))
                .hp(60)
                .types(List.of("Water"))
                .attacks(List.of())
                .build());
        }
        return cards;
    }

    private GameStateSnapshot placementSnapshot(boolean p1Ready, boolean p2Ready) {
        SetupPhaseState setupState = SetupPhaseState.builder()
            .currentStep(SetupPhaseState.SetupStep.INITIAL_PLACEMENT)
            .player1ReadyToStart(p1Ready)
            .player2ReadyToStart(p2Ready)
            .player1PlacedActive(true)
            .player2PlacedActive(true)
            .build();

        return GameStateSnapshot.builder()
            .gameId(99L)
            .player1Id(1L)
            .player2Id(2L)
            .status(GameStatus.SETUP)
            .turnNumber(0)
            .isFirstTurn(true)
            .p1Hand(new ArrayList<>())
            .p1Deck(new ArrayList<>())
            .p1Prizes(new ArrayList<>())
            .p1Discard(new ArrayList<>())
            .p1Bench(new ArrayList<>())
            .p1ActivePokemon(PokemonInPlay.fromCard(CardData.builder().id("p1-active").name("p1-active").supertype("Pokémon").subtypes(List.of("Basic")).hp(60).types(List.of("Fire")).attacks(List.of()).build()))
            .p2Hand(new ArrayList<>())
            .p2Deck(new ArrayList<>())
            .p2Prizes(new ArrayList<>())
            .p2Discard(new ArrayList<>())
            .p2Bench(new ArrayList<>())
            .p2ActivePokemon(PokemonInPlay.fromCard(CardData.builder().id("p2-active").name("p2-active").supertype("Pokémon").subtypes(List.of("Basic")).hp(60).types(List.of("Water")).attacks(List.of()).build()))
            .setupState(setupState)
            .build();
    }
}