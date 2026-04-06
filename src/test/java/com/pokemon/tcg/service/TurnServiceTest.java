package com.pokemon.tcg.service;

import com.pokemon.tcg.engine.GameEngine;
import com.pokemon.tcg.engine.TrainerCardEffectProcessor;
import com.pokemon.tcg.engine.model.ActionResult;
import com.pokemon.tcg.engine.model.GameEvent;
import com.pokemon.tcg.engine.model.GameEventType;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.exception.InvalidMoveException;
import com.pokemon.tcg.model.dto.GameActionMessage;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.entity.Player;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.model.enums.VictoryCondition;
import com.pokemon.tcg.repository.ActionRepository;
import com.pokemon.tcg.repository.GameRepository;
import com.pokemon.tcg.repository.TurnRepository;
import com.pokemon.tcg.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private TurnRepository turnRepository;
    @Mock private ActionRepository actionRepository;
    @Mock private GameStateMapper gameStateMapper;
    @Mock private GameEngine gameEngine;
    @Mock private TrainerCardEffectProcessor trainerProcessor;
    @Mock private SimpMessagingTemplate messaging;
    @Mock private JsonUtil jsonUtil;

    private TurnService turnService;

    private static final Long GAME_ID   = 1L;
    private static final Long PLAYER1   = 10L;
    private static final Long PLAYER2   = 20L;

    @BeforeEach
    void setUp() {
        turnService = new TurnService(gameRepository, turnRepository, actionRepository,
            gameStateMapper, gameEngine, trainerProcessor, messaging, jsonUtil);
    }

    // ── Guardianes de estado ──────────────────────────────────────────────

    @Test
    void falla_si_partida_no_activa() {
        Game game = gameWith(GameStatus.SETUP, PLAYER1);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> turnService.executeAction(GAME_ID, PLAYER1, action("DRAW_CARD")))
            .isInstanceOf(InvalidMoveException.class)
            .hasMessageContaining("activa");
    }

    @Test
    void falla_si_no_es_turno_del_jugador() {
        Game game = gameWith(GameStatus.ACTIVE, PLAYER1); // turno de P1
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        // P2 intenta jugar cuando es turno de P1
        assertThatThrownBy(() -> turnService.executeAction(GAME_ID, PLAYER2, action("DRAW_CARD")))
            .isInstanceOf(InvalidMoveException.class)
            .hasMessageContaining("turno");
    }

    // ── Delegación al engine ──────────────────────────────────────────────

    @Test
    void draw_card_delega_al_engine_y_hace_broadcast() {
        Game game = gameWith(GameStatus.ACTIVE, PLAYER1);
        GameStateSnapshot snapshot = emptySnapshot();
        ActionResult engineResult = successResult(snapshot);

        com.pokemon.tcg.model.entity.Turn savedTurn = com.pokemon.tcg.model.entity.Turn.builder()
            .id(1L).game(game).turnNumber(1).playerId(PLAYER1).build();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gameStateMapper.loadSnapshot(game)).thenReturn(snapshot);
        when(gameEngine.drawCard(snapshot)).thenReturn(engineResult);
        when(turnRepository.findByGameIdOrderByTurnNumberAsc(GAME_ID)).thenReturn(List.of());
        when(turnRepository.save(any())).thenReturn(savedTurn);
        when(jsonUtil.toJson(any())).thenReturn("{}");

        ActionResult result = turnService.executeAction(GAME_ID, PLAYER1, action("DRAW_CARD"));

        assertThat(result.isSuccess()).isTrue();
        verify(gameEngine).drawCard(snapshot);
        verify(gameStateMapper).saveSnapshot(eq(game), eq(snapshot));
        // broadcastStateUpdate ahora envía a cada jugador individualmente
        verify(messaging, atLeastOnce()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void attack_delega_al_engine_con_indice() {
        Game game = gameWith(GameStatus.ACTIVE, PLAYER1);
        GameStateSnapshot snapshot = emptySnapshot();
        ActionResult engineResult = successResult(snapshot);

        com.pokemon.tcg.model.entity.Turn savedTurn = com.pokemon.tcg.model.entity.Turn.builder()
            .id(1L).game(game).turnNumber(1).playerId(PLAYER1).build();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gameStateMapper.loadSnapshot(game)).thenReturn(snapshot);
        when(gameEngine.attack(snapshot, 1)).thenReturn(engineResult);
        when(turnRepository.findByGameIdOrderByTurnNumberAsc(GAME_ID)).thenReturn(List.of());
        when(turnRepository.save(any())).thenReturn(savedTurn);
        when(jsonUtil.toJson(any())).thenReturn("{}");

        GameActionMessage msg = action("ATTACK");
        msg.setData(Map.of("attackIndex", 1));
        turnService.executeAction(GAME_ID, PLAYER1, msg);

        verify(gameEngine).attack(snapshot, 1);
    }

    @Test
    void place_pokemon_delega_al_engine() {
        Game game = gameWith(GameStatus.ACTIVE, PLAYER1);
        GameStateSnapshot snapshot = emptySnapshot();
        ActionResult engineResult = successResult(snapshot);

        com.pokemon.tcg.model.entity.Turn savedTurn = com.pokemon.tcg.model.entity.Turn.builder()
            .id(1L).game(game).turnNumber(1).playerId(PLAYER1).build();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gameStateMapper.loadSnapshot(game)).thenReturn(snapshot);
        when(gameEngine.placePokemon(snapshot, "card-1", "BENCH")).thenReturn(engineResult);
        when(turnRepository.findByGameIdOrderByTurnNumberAsc(GAME_ID)).thenReturn(List.of());
        when(turnRepository.save(any())).thenReturn(savedTurn);
        when(jsonUtil.toJson(any())).thenReturn("{}");

        GameActionMessage msg = action("PLACE_POKEMON");
        msg.setData(Map.of("cardId", "card-1", "position", "BENCH"));
        turnService.executeAction(GAME_ID, PLAYER1, msg);

        verify(gameEngine).placePokemon(snapshot, "card-1", "BENCH");
    }

    // ── Acción inválida ───────────────────────────────────────────────────

    @Test
    void accion_invalida_hace_broadcast_de_error_y_no_persiste() {
        Game game = gameWith(GameStatus.ACTIVE, PLAYER1);
        GameStateSnapshot snapshot = emptySnapshot();
        ActionResult failResult = ActionResult.fail("sin energía", snapshot);

        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gameStateMapper.loadSnapshot(game)).thenReturn(snapshot);
        when(gameEngine.drawCard(snapshot)).thenReturn(failResult);

        ActionResult result = turnService.executeAction(GAME_ID, PLAYER1, action("DRAW_CARD"));

        assertThat(result.isSuccess()).isFalse();
        // No debe persistir el estado
        verify(gameStateMapper, org.mockito.Mockito.never()).saveSnapshot(any(), any());
        // Debe notificar al jugador
        verify(messaging).convertAndSendToUser(anyString(), anyString(), any());
    }

    // ── Fin de juego ──────────────────────────────────────────────────────

    @Test
    void game_over_actualiza_la_entidad_game() {
        Game game = gameWith(GameStatus.ACTIVE, PLAYER1);
        game.setPlayer1(player(PLAYER1, "ash"));
        game.setPlayer2(player(PLAYER2, "misty"));

        GameStateSnapshot snapshot = emptySnapshot();
        ActionResult gameOverResult = ActionResult.builder()
            .success(true).gameOver(true).winnerId(PLAYER1)
            .victoryCondition(VictoryCondition.PRIZES)
            .updatedState(snapshot).events(new ArrayList<>()).build();

        com.pokemon.tcg.model.entity.Turn savedTurn = com.pokemon.tcg.model.entity.Turn.builder()
            .id(1L).game(game).turnNumber(1).playerId(PLAYER1).build();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gameStateMapper.loadSnapshot(game)).thenReturn(snapshot);
        when(gameEngine.drawCard(snapshot)).thenReturn(gameOverResult);
        when(turnRepository.findByGameIdOrderByTurnNumberAsc(GAME_ID)).thenReturn(List.of());
        when(turnRepository.save(any())).thenReturn(savedTurn);
        when(jsonUtil.toJson(any())).thenReturn("{}");
        when(gameRepository.save(any())).thenReturn(game);

        turnService.executeAction(GAME_ID, PLAYER1, action("DRAW_CARD"));

        verify(gameRepository).save(any(Game.class));
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getVictoryCondition()).isEqualTo(VictoryCondition.PRIZES);
        assertThat(game.getWinner().getId()).isEqualTo(PLAYER1);
    }

    // ── Tipo de acción desconocido ────────────────────────────────────────

    @Test
    void tipo_accion_desconocido_lanza_exception() {
        Game game = gameWith(GameStatus.ACTIVE, PLAYER1);
        GameStateSnapshot snapshot = emptySnapshot();

        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gameStateMapper.loadSnapshot(game)).thenReturn(snapshot);

        assertThatThrownBy(() -> turnService.executeAction(GAME_ID, PLAYER1, action("UNKNOWN_ACTION")))
            .isInstanceOf(InvalidMoveException.class)
            .hasMessageContaining("desconocido");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Game gameWith(GameStatus status, Long currentTurnPlayerId) {
        Game game = new Game();
        game.setId(GAME_ID);
        game.setStatus(status);
        game.setCurrentTurnPlayerId(currentTurnPlayerId);
        game.setTurnNumber(1);
        game.setPlayer1(player(PLAYER1, "ash"));
        game.setPlayer2(player(PLAYER2, "misty"));
        return game;
    }

    private Player player(Long id, String name) {
        Player p = new Player();
        p.setId(id);
        p.setUsername(name);
        return p;
    }

    private GameStateSnapshot emptySnapshot() {
        return GameStateSnapshot.builder()
            .gameId(GAME_ID)
            .player1Id(PLAYER1).player2Id(PLAYER2).currentTurnPlayerId(PLAYER1)
            .turnNumber(1).isFirstTurn(false).status(GameStatus.ACTIVE)
            .p1Deck(new ArrayList<>()).p1Hand(new ArrayList<>())
            .p1Prizes(new ArrayList<>()).p1Discard(new ArrayList<>()).p1Bench(new ArrayList<>())
            .p2Deck(new ArrayList<>()).p2Hand(new ArrayList<>())
            .p2Prizes(new ArrayList<>()).p2Discard(new ArrayList<>()).p2Bench(new ArrayList<>())
            .energyAttachedThisTurn(false).supporterPlayedThisTurn(false)
            .stadiumPlayedThisTurn(false).retreatUsedThisTurn(false)
            .build();
    }

    private ActionResult successResult(GameStateSnapshot snapshot) {
        return ActionResult.builder()
            .success(true).gameOver(false)
            .updatedState(snapshot)
            .events(List.of(new GameEvent(GameEventType.CARD_DRAWN, Map.of())))
            .build();
    }

    private GameActionMessage action(String type) {
        GameActionMessage msg = new GameActionMessage();
        msg.setType(type);
        msg.setData(Map.of());
        return msg;
    }
}
