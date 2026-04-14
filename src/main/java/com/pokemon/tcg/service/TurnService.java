package com.pokemon.tcg.service;

import com.pokemon.tcg.engine.GameEngine;
import com.pokemon.tcg.engine.TrainerCardEffectProcessor;
import com.pokemon.tcg.engine.model.ActionResult;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.exception.GameNotFoundException;
import com.pokemon.tcg.exception.InvalidMoveException;
import com.pokemon.tcg.model.dto.GameActionMessage;
import com.pokemon.tcg.model.dto.GameStateViewDTO;
import com.pokemon.tcg.model.entity.Action;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.entity.Turn;
import com.pokemon.tcg.model.enums.ActionType;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.repository.ActionRepository;
import com.pokemon.tcg.repository.GameRepository;
import com.pokemon.tcg.repository.TurnRepository;
import com.pokemon.tcg.util.CardSelectionUtil;
import com.pokemon.tcg.util.JsonUtil;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TurnService {

    private final GameRepository gameRepository;
    private final TurnRepository turnRepository;
    private final ActionRepository actionRepository;
    private final GameStateMapper gameStateMapper;
    private final GameEngine gameEngine;
    private final TrainerCardEffectProcessor trainerProcessor;
    private final SimpMessagingTemplate messaging;
    private final JsonUtil jsonUtil;

    public TurnService(GameRepository gameRepository, TurnRepository turnRepository,
                       ActionRepository actionRepository, GameStateMapper gameStateMapper,
                       GameEngine gameEngine, TrainerCardEffectProcessor trainerProcessor,
                       SimpMessagingTemplate messaging, JsonUtil jsonUtil) {
        this.gameRepository = gameRepository;
        this.turnRepository = turnRepository;
        this.actionRepository = actionRepository;
        this.gameStateMapper = gameStateMapper;
        this.gameEngine = gameEngine;
        this.trainerProcessor = trainerProcessor;
        this.messaging = messaging;
        this.jsonUtil = jsonUtil;
    }

    public ActionResult executeAction(Long gameId, Long playerId, GameActionMessage action) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameNotFoundException(gameId));

        if (game.getStatus() != GameStatus.ACTIVE) {
            throw new InvalidMoveException("La partida no está activa (estado: " + game.getStatus() + ")");
        }
        if (!playerId.equals(game.getCurrentTurnPlayerId())) {
            throw new InvalidMoveException("No es tu turno");
        }

        GameStateSnapshot snapshot = gameStateMapper.loadSnapshot(game);
        Integer handIndex = CardSelectionUtil.extractHandIndex(action.getData());

        ActionResult result = switch (action.getType()) {
            case "DRAW_CARD"     -> gameEngine.drawCard(snapshot);
            case "PLACE_POKEMON" -> gameEngine.placePokemon(snapshot,
                (String) action.getData().get("cardId"),
                (String) action.getData().get("position"),
                handIndex);
            case "ATTACH_ENERGY" -> gameEngine.attachEnergy(snapshot,
                (String) action.getData().get("cardId"),
                (String) action.getData().get("targetPosition"),
                handIndex);
            case "EVOLVE"        -> gameEngine.evolve(snapshot,
                (String) action.getData().get("evolutionCardId"),
                (String) action.getData().get("targetPosition"),
                handIndex);
            case "PLAY_TRAINER"  -> processTrainer(snapshot,
                (String) action.getData().get("cardId"), handIndex, action.getData());
            case "RETREAT"       -> gameEngine.retreat(snapshot,
                castList(action.getData().get("energyIdsToDiscard")),
                ((Number) action.getData().get("newActiveIndex")).intValue());
            case "ATTACK"        -> gameEngine.attack(snapshot,
                ((Number) action.getData().get("attackIndex")).intValue());
            case "END_TURN"      -> gameEngine.endTurn(snapshot);
            default -> throw new InvalidMoveException("Tipo de acción desconocido: " + action.getType());
        };

        if (!result.isSuccess()) {
            broadcastError(gameId, playerId, result.getErrorMessage());
            return result;
        }

        // Persistir estado actualizado
        gameStateMapper.saveSnapshot(game, result.getUpdatedState());

        // Registrar historial
        logAction(game, playerId, action, result);

        // Actualizar Game si terminó
        if (result.isGameOver()) {
            game.setStatus(GameStatus.FINISHED);
            game.setFinishedAt(LocalDateTime.now());
            game.setVictoryCondition(result.getVictoryCondition());
            if (result.getWinnerId() != null) {
                boolean p1Wins = result.getWinnerId().equals(game.getPlayer1().getId());
                game.setWinner(p1Wins ? game.getPlayer1() : game.getPlayer2());
            }
            gameRepository.save(game);
        }

        broadcastStateUpdate(gameId, result, game);
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ActionResult processTrainer(GameStateSnapshot snapshot, String cardId, Integer handIndex, Map<String, Object> data) {
        CardData card = CardSelectionUtil.findInHand(snapshot.getCurrentPlayerHand(), cardId, handIndex);
        if (card == null) {
            throw new InvalidMoveException("Carta Entrenador no encontrada en mano: " + cardId);
        }
        return trainerProcessor.process(card, snapshot, data, handIndex);
    }

    private void logAction(Game game, Long playerId, GameActionMessage action, ActionResult result) {
        Turn currentTurn = turnRepository.findByGameIdOrderByTurnNumberAsc(game.getId())
            .stream()
            .filter(t -> t.getEndedAt() == null)
            .findFirst()
            .orElseGet(() -> turnRepository.save(Turn.builder()
                .game(game)
                .turnNumber(game.getTurnNumber())
                .playerId(playerId)
                .build()));

        ActionType actionType;
        try {
            actionType = ActionType.valueOf(action.getType());
        } catch (IllegalArgumentException e) {
            actionType = ActionType.BETWEEN_TURNS;
        }

        actionRepository.save(Action.builder()
            .turn(currentTurn)
            .actionType(actionType)
            .actionData(jsonUtil.toJson(action.getData()))
            .isValid(result.isSuccess())
            .validationError(result.isSuccess() ? null : result.getErrorMessage())
            .build());

        // Cerrar el turno al atacar o pasar turno
        if ("ATTACK".equals(action.getType()) || "END_TURN".equals(action.getType())) {
            currentTurn.setEndedAt(LocalDateTime.now());
            turnRepository.save(currentTurn);
        }
    }

    private void broadcastStateUpdate(Long gameId, ActionResult result, Game game) {
        GameStateSnapshot snapshot = result.getUpdatedState();
        String type = result.isGameOver() ? "GAME_OVER" : "STATE_UPDATE";

        // Enviar vista personalizada a cada jugador (mano del oponente oculta)
        if (game.getPlayer1() != null) {
            GameStateViewDTO viewP1 = GameStateViewDTO.projectFor(snapshot, game.getPlayer1().getId());
            messaging.convertAndSendToUser(
                String.valueOf(game.getPlayer1().getId()),
                "/queue/game/" + gameId,
                Map.of("type", type, "data", viewP1,
                       "events", result.getEvents(),
                       "gameOver", result.isGameOver(),
                       "winnerId", result.getWinnerId() != null ? result.getWinnerId() : "",
                       "victoryCondition", result.getVictoryCondition() != null
                           ? result.getVictoryCondition().name() : ""));
        }
        if (game.getPlayer2() != null) {
            GameStateViewDTO viewP2 = GameStateViewDTO.projectFor(snapshot, game.getPlayer2().getId());
            messaging.convertAndSendToUser(
                String.valueOf(game.getPlayer2().getId()),
                "/queue/game/" + gameId,
                Map.of("type", type, "data", viewP2,
                       "events", result.getEvents(),
                       "gameOver", result.isGameOver(),
                       "winnerId", result.getWinnerId() != null ? result.getWinnerId() : "",
                       "victoryCondition", result.getVictoryCondition() != null
                           ? result.getVictoryCondition().name() : ""));
        }
    }

    private void broadcastError(Long gameId, Long playerId, String message) {
        messaging.convertAndSendToUser(String.valueOf(playerId),
            "/queue/game/" + gameId + "/error",
            Map.of("type", "ACTION_INVALID", "error", message));
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object obj) {
        return obj instanceof List ? (List<String>) obj : List.of();
    }
}
