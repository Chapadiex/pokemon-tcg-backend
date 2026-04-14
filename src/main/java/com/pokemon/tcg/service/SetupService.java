package com.pokemon.tcg.service;

import com.pokemon.tcg.engine.InitialPlacementValidator;
import com.pokemon.tcg.engine.MulliganProcessor;
import com.pokemon.tcg.engine.model.GameEvent;
import com.pokemon.tcg.engine.model.GameEventType;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.MulliganResult;
import com.pokemon.tcg.engine.model.ValidationResult;
import com.pokemon.tcg.exception.InvalidMoveException;
import com.pokemon.tcg.model.entity.CardInDeck;
import com.pokemon.tcg.model.entity.Deck;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.PokemonInPlay;
import com.pokemon.tcg.model.game.SetupPhaseState;
import com.pokemon.tcg.repository.GameRepository;
import com.pokemon.tcg.util.CardSelectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class SetupService {
    private final GameRepository gameRepository;
    private final CardService cardService;
    private final MulliganProcessor mulliganProcessor;
    private final InitialPlacementValidator placementValidator;
    private final GameStateMapper gameStateMapper;
    private final SimpMessagingTemplate messaging;
    private final Random random;

    @Autowired
    public SetupService(
        GameRepository gameRepository,
        CardService cardService,
        MulliganProcessor mulliganProcessor,
        InitialPlacementValidator placementValidator,
        GameStateMapper gameStateMapper,
        SimpMessagingTemplate messaging
    ) {
        this(gameRepository, cardService, mulliganProcessor, placementValidator, gameStateMapper, messaging, new Random());
    }

    SetupService(
        GameRepository gameRepository,
        CardService cardService,
        MulliganProcessor mulliganProcessor,
        InitialPlacementValidator placementValidator,
        GameStateMapper gameStateMapper,
        SimpMessagingTemplate messaging,
        Random random
    ) {
        this.gameRepository = gameRepository;
        this.cardService = cardService;
        this.mulliganProcessor = mulliganProcessor;
        this.placementValidator = placementValidator;
        this.gameStateMapper = gameStateMapper;
        this.messaging = messaging;
        this.random = random;
    }

    public void initializeSetup(Long gameId) {
        initializeSetupWithPrizeCount(gameId, 6);
    }

    public void initializeSetupWithPrizeCount(Long gameId, int prizeCount) {
        Game game = findGame(gameId);
        ensureSetupReady(game);

        List<CardData> deck1 = new ArrayList<>(buildFullDeck(game.getPlayer1Deck()));
        List<CardData> deck2 = new ArrayList<>(buildFullDeck(game.getPlayer2Deck()));
        Collections.shuffle(deck1, random);
        Collections.shuffle(deck2, random);

        List<CardData> hand1 = drawFromTop(deck1, 7);
        List<CardData> hand2 = drawFromTop(deck2, 7);
        List<CardData> prizes1 = drawFromTop(deck1, prizeCount);
        List<CardData> prizes2 = drawFromTop(deck2, prizeCount);

        GameStateSnapshot snapshot = GameStateSnapshot.builder()
            .gameId(gameId)
            .player1Id(game.getPlayer1().getId())
            .player2Id(game.getPlayer2().getId())
            .currentTurnPlayerId(null)
            .turnNumber(0)
            .isFirstTurn(true)
            .status(GameStatus.SETUP)
            .p1Deck(deck1)
            .p1Hand(hand1)
            .p1Prizes(prizes1)
            .p1Discard(new ArrayList<>())
            .p1Bench(new ArrayList<>())
            .p2Deck(deck2)
            .p2Hand(hand2)
            .p2Prizes(prizes2)
            .p2Discard(new ArrayList<>())
            .p2Bench(new ArrayList<>())
            .energyAttachedThisTurn(false)
            .supporterPlayedThisTurn(false)
            .stadiumPlayedThisTurn(false)
            .retreatUsedThisTurn(false)
            .setupState(SetupPhaseState.initial())
            .build();

        game.setStatus(GameStatus.SETUP);
        gameStateMapper.saveSnapshot(game, snapshot);
        gameRepository.save(game);
        processMulliganLoop(game, snapshot);
    }

    public void applyExtraDraw(Long gameId, Long playerId) {
        Game game = findGame(gameId);
        GameStateSnapshot snapshot = gameStateMapper.loadSnapshot(game);
        if (!canPlayerTakeExtraDraw(snapshot, playerId)) {
            throw new InvalidMoveException("No tienes una carta extra pendiente por mulligan");
        }

        mulliganProcessor.applyExtraDraw(snapshot, playerId);
        gameStateMapper.saveSnapshot(game, snapshot);
        processMulliganLoop(game, snapshot);
    }

    public void declineExtraDraw(Long gameId, Long playerId) {
        Game game = findGame(gameId);
        GameStateSnapshot snapshot = gameStateMapper.loadSnapshot(game);
        if (!canPlayerTakeExtraDraw(snapshot, playerId)) {
            throw new InvalidMoveException("No tienes una decision pendiente de mulligan");
        }

        mulliganProcessor.declineExtraDraw(snapshot, playerId);
        gameStateMapper.saveSnapshot(game, snapshot);
        processMulliganLoop(game, snapshot);
    }

    public void placePokemonDuringSetup(Long gameId, Long playerId, String cardId, String position) {
        Game game = findGame(gameId);
        GameStateSnapshot snapshot = gameStateMapper.loadSnapshot(game);
        ensurePlacementPhase(snapshot);

        boolean isPlayer1 = playerId.equals(game.getPlayer1().getId());
        List<CardData> hand = isPlayer1 ? snapshot.getP1Hand() : snapshot.getP2Hand();
        CardData card = hand.stream()
            .filter(candidate -> candidate.getId().equals(cardId))
            .findFirst()
            .orElseThrow(() -> new InvalidMoveException("Carta no encontrada en la mano"));

        ValidationResult validation = placementValidator.validatePlaceDuringSetup(card, position, snapshot, isPlayer1);
        if (!validation.isValid()) {
            throw new InvalidMoveException(validation.getErrorMessage());
        }

        boolean removed = CardSelectionUtil.removeFirstById(hand, cardId);
        if (!removed) {
            throw new InvalidMoveException("No se pudo remover la carta seleccionada durante setup");
        }
        PokemonInPlay pokemon = PokemonInPlay.fromCard(card);
        if ("ACTIVE".equals(position)) {
            if (isPlayer1) {
                snapshot.setP1ActivePokemon(pokemon);
                snapshot.getSetupState().setPlayer1PlacedActive(true);
            } else {
                snapshot.setP2ActivePokemon(pokemon);
                snapshot.getSetupState().setPlayer2PlacedActive(true);
            }
        } else {
            if (isPlayer1) {
                snapshot.getP1Bench().add(pokemon);
            } else {
                snapshot.getP2Bench().add(pokemon);
            }
        }

        gameStateMapper.saveSnapshot(game, snapshot);
        broadcastSetupAction(gameId, playerId, "PLACE_POKEMON", Map.of("cardId", cardId, "position", position));
        broadcastSetupState(gameId, snapshot);
    }

    public void playerReadyToStart(Long gameId, Long playerId) {
        Game game = findGame(gameId);
        GameStateSnapshot snapshot = gameStateMapper.loadSnapshot(game);
        ensurePlacementPhase(snapshot);

        boolean isPlayer1 = playerId.equals(game.getPlayer1().getId());
        ValidationResult validation = placementValidator.validateReadyToStart(snapshot, isPlayer1);
        if (!validation.isValid()) {
            throw new InvalidMoveException(validation.getErrorMessage());
        }

        if (isPlayer1) {
            snapshot.getSetupState().setPlayer1ReadyToStart(true);
        } else {
            snapshot.getSetupState().setPlayer2ReadyToStart(true);
        }

        gameStateMapper.saveSnapshot(game, snapshot);
        broadcastSetupAction(gameId, playerId, "READY_TO_START", Map.of());
        if (bothPlayersReady(snapshot)) {
            finishSetupAndStartGame(game, snapshot);
        } else {
            broadcastSetupState(gameId, snapshot);
        }
    }

    private void processMulliganLoop(Game game, GameStateSnapshot snapshot) {
        int iterations = 0;
        while (iterations++ < 20) {
            MulliganResult result = mulliganProcessor.processMulliganRound(snapshot);
            result.getEvents().forEach(event -> broadcastEvent(game.getId(), event));

            if (!result.getEvents().isEmpty()) {
                broadcastMulliganNeeded(game.getId(), result);
            }

            if (result.isBothReady()) {
                snapshot.getSetupState().setCurrentStep(SetupPhaseState.SetupStep.INITIAL_PLACEMENT);
                gameStateMapper.saveSnapshot(game, snapshot);
                broadcastSetupStep(game.getId(), SetupPhaseState.SetupStep.INITIAL_PLACEMENT.name(),
                    Map.of("p1HandCount", snapshot.getP1Hand().size(), "p2HandCount", snapshot.getP2Hand().size()));
                broadcastSetupState(game.getId(), snapshot);
                return;
            }

            gameStateMapper.saveSnapshot(game, snapshot);
            if (result.isP1CanDrawExtra() || result.isP2CanDrawExtra()) {
                broadcastExtraDrawOffer(game.getId(), snapshot);
                return;
            }
        }

        throw new IllegalStateException("Se excedio el maximo de iteraciones de mulligan");
    }

    private void finishSetupAndStartGame(Game game, GameStateSnapshot snapshot) {
        boolean player1Starts = random.nextBoolean();
        Long firstPlayerId = player1Starts ? game.getPlayer1().getId() : game.getPlayer2().getId();

        snapshot.getSetupState().setCurrentStep(SetupPhaseState.SetupStep.COMPLETE);
        snapshot.setCurrentTurnPlayerId(firstPlayerId);
        snapshot.setStatus(GameStatus.ACTIVE);
        snapshot.setTurnNumber(1);
        snapshot.setIsFirstTurn(true);

        game.setCurrentTurnPlayerId(firstPlayerId);
        game.setStatus(GameStatus.ACTIVE);
        game.setTurnNumber(1);
        game.setIsFirstTurn(true);
        game.setStartedAt(LocalDateTime.now());

        gameStateMapper.saveSnapshot(game, snapshot);
        gameRepository.save(game);
        broadcastGameReady(game.getId(), firstPlayerId, snapshot);
    }

    private boolean bothPlayersReady(GameStateSnapshot snapshot) {
        return snapshot.getSetupState().isPlayer1ReadyToStart() && snapshot.getSetupState().isPlayer2ReadyToStart();
    }

    private boolean canPlayerTakeExtraDraw(GameStateSnapshot snapshot, Long playerId) {
        return playerId.equals(snapshot.getPlayer1Id())
            ? snapshot.getSetupState().isPlayer1CanDrawExtra()
            : playerId.equals(snapshot.getPlayer2Id()) && snapshot.getSetupState().isPlayer2CanDrawExtra();
    }

    private void ensurePlacementPhase(GameStateSnapshot snapshot) {
        if (snapshot.getStatus() != GameStatus.SETUP || snapshot.getSetupState().getCurrentStep() != SetupPhaseState.SetupStep.INITIAL_PLACEMENT) {
            throw new InvalidMoveException("El juego no esta listo para la colocacion inicial");
        }
    }

    private void ensureSetupReady(Game game) {
        if (game.getPlayer2() == null || game.getPlayer2Deck() == null) {
            throw new InvalidMoveException("La partida necesita dos jugadores y dos mazos para iniciar el setup");
        }
    }

    private Game findGame(Long gameId) {
        return gameRepository.findById(gameId)
            .orElseThrow(() -> new IllegalArgumentException("Juego no encontrado: " + gameId));
    }

    /**
     * Builds the full 60-card deck properly: fetches unique cards in batch (efficient),
     * then replicates each card according to its quantity.
     * Fixes the bug where preloadDeck().distinct() was deduplicating and shrinking the deck.
     */
    private List<CardData> buildFullDeck(Deck deck) {
        List<CardInDeck> deckCards = deck.getCards();
        List<String> uniqueIds = deckCards.stream()
            .map(CardInDeck::getCardId)
            .distinct()
            .collect(Collectors.toList());

        Map<String, CardData> cardMap = cardService.getCardsByIdsBatch(uniqueIds);

        List<CardData> fullDeck = new ArrayList<>();
        for (CardInDeck cid : deckCards) {
            CardData card = cardMap.get(cid.getCardId());
            if (card != null) {
                for (int i = 0; i < cid.getQuantity(); i++) {
                    fullDeck.add(card);
                }
            }
        }
        return fullDeck;
    }

    private List<String> expandDeckIds(Deck deck) {
        List<String> ids = new ArrayList<>();
        for (CardInDeck card : deck.getCards()) {
            for (int i = 0; i < card.getQuantity(); i++) {
                ids.add(card.getCardId());
            }
        }
        return ids;
    }

    private List<CardData> drawFromTop(List<CardData> deck, int count) {
        List<CardData> drawn = new ArrayList<>();
        int actual = Math.min(count, deck.size());
        for (int i = 0; i < actual; i++) {
            drawn.add(deck.remove(0));
        }
        return drawn;
    }

    private void broadcastEvent(Long gameId, GameEvent event) {
        messaging.convertAndSend("/topic/game/" + gameId, Map.of("type", event.getType().name(), "data", event.getData()));
    }

    private void broadcastSetupStep(Long gameId, String step, Map<String, Object> data) {
        messaging.convertAndSend("/topic/game/" + gameId, Map.of("type", "SETUP_STEP", "step", step, "data", data));
    }

    private void broadcastMulliganNeeded(Long gameId, MulliganResult result) {
        messaging.convertAndSend("/topic/game/" + gameId,
            Map.of("type", GameEventType.MULLIGAN_NEEDED.name(),
                "data", Map.of(
                    "p1CanDraw", result.isP1CanDrawExtra(),
                    "p2CanDraw", result.isP2CanDrawExtra(),
                    "continueLoop", result.isContinueLoop())));
    }

    private void broadcastExtraDrawOffer(Long gameId, GameStateSnapshot snapshot) {
        messaging.convertAndSend("/topic/game/" + gameId,
            Map.of("type", "EXTRA_DRAW_OFFER",
                "data", Map.of(
                    "p1CanDraw", snapshot.getSetupState().isPlayer1CanDrawExtra(),
                    "p2CanDraw", snapshot.getSetupState().isPlayer2CanDrawExtra())));
    }

    private void broadcastSetupAction(Long gameId, Long playerId, String actionType, Map<String, Object> data) {
        messaging.convertAndSend("/topic/game/" + gameId,
            Map.of("type", GameEventType.SETUP_ACTION.name(),
                "data", Map.of("playerId", playerId, "action", actionType, "payload", data)));
    }

    private void broadcastSetupState(Long gameId, GameStateSnapshot snapshot) {
        messaging.convertAndSend("/topic/game/" + gameId,
            Map.of("type", "SETUP_STATE_UPDATE", "data", snapshot));
    }

    private void broadcastGameReady(Long gameId, Long firstPlayerId, GameStateSnapshot snapshot) {
        messaging.convertAndSend("/topic/game/" + gameId,
            Map.of("type", GameEventType.GAME_READY.name(),
                "data", Map.of("firstPlayerId", firstPlayerId, "initialState", snapshot)));
    }
}
