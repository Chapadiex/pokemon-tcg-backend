package com.pokemon.tcg.controller;

import com.pokemon.tcg.engine.BenchDamageResolver;
import com.pokemon.tcg.engine.KnockoutReplacementHandler;
import com.pokemon.tcg.engine.model.ActionResult;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.model.dto.GameActionMessage;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.repository.GameRepository;
import com.pokemon.tcg.service.GameStateMapper;
import com.pokemon.tcg.service.TurnService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Map;

/**
 * Maneja las acciones de turno durante el juego activo y las acciones
 * de reemplazo (KO y dano a Banca) a traves de WebSocket STOMP.
 *
 * Las acciones de la fase de setup se delegan a {@link SetupController}.
 */
@Controller
@Transactional
public class WebSocketController {
    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private final TurnService turnService;
    private final KnockoutReplacementHandler replacementHandler;
    private final BenchDamageResolver benchDamageResolver;
    private final GameRepository gameRepository;
    private final GameStateMapper gameStateMapper;
    private final SimpMessagingTemplate messaging;

    public WebSocketController(TurnService turnService,
                                KnockoutReplacementHandler replacementHandler,
                                BenchDamageResolver benchDamageResolver,
                                GameRepository gameRepository,
                                GameStateMapper gameStateMapper,
                                SimpMessagingTemplate messaging) {
        this.turnService          = turnService;
        this.replacementHandler   = replacementHandler;
        this.benchDamageResolver  = benchDamageResolver;
        this.gameRepository       = gameRepository;
        this.gameStateMapper      = gameStateMapper;
        this.messaging            = messaging;
    }

    /**
     * Acciones de turno durante el juego activo.
     * Destino: /app/game/{gameId}/action
     */
    @MessageMapping("/game/{gameId}/action")
    public void handleAction(@DestinationVariable Long gameId,
                             @Payload GameActionMessage action,
                             Principal principal) {
        Long playerId = Long.parseLong(principal.getName());
        ActionResult result = turnService.executeAction(gameId, playerId, action);
        // El broadcast lo hace TurnService internamente
    }

    /**
     * El jugador elige su Pokemon de reemplazo tras un KO.
     * Destino: /app/game/{gameId}/replacement
     */
    @MessageMapping("/game/{gameId}/replacement")
    public void handleReplacement(@DestinationVariable Long gameId,
                                   @Payload Map<String, Object> payload,
                                   Principal principal) {
        Long playerId   = Long.parseLong(principal.getName());
        int  benchIndex = ((Number) payload.get("benchIndex")).intValue();
        replacementHandler.applyReplacement(gameId, playerId, benchIndex);
    }

    /**
     * El atacante elige el objetivo de dano a Banca.
     * Destino: /app/game/{gameId}/bench-damage-target
     */
    @MessageMapping("/game/{gameId}/bench-damage-target")
    public void handleBenchDamageTarget(@DestinationVariable Long gameId,
                                         @Payload Map<String, Object> payload,
                                         Principal principal) {
        Long playerId   = Long.parseLong(principal.getName());
        int  benchIndex = ((Number) payload.get("benchIndex")).intValue();
        Game game = gameRepository.findById(gameId).orElseThrow();
        benchDamageResolver.resolve(game, playerId, benchIndex);
    }

    /**
     * Un jugador que se reconecta pide el estado actual del juego.
     * Destino: /app/game/{gameId}/reconnect
     * El backend responde por topic y tambien por user-queue para mayor robustez.
     */
    @MessageMapping("/game/{gameId}/reconnect")
    public void handleReconnect(@DestinationVariable Long gameId, Principal principal) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) return;

        try {
            GameStateSnapshot snapshot = gameStateMapper.loadSnapshot(game);
            if (game.getStatus() == GameStatus.SETUP) {
                Map<String, Object> payload = Map.of("type", "SETUP_STATE_UPDATE", "data", snapshot);
                messaging.convertAndSend("/topic/game/" + gameId, payload);
                if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
                    messaging.convertAndSendToUser(principal.getName(), "/queue/game/" + gameId, payload);
                }
            } else if (game.getStatus() == GameStatus.ACTIVE) {
                Long firstPlayerId = game.getCurrentTurnPlayerId();
                Map<String, Object> payload = firstPlayerId != null
                    ? Map.of("type", "GAME_READY", "data", Map.of("firstPlayerId", firstPlayerId, "initialState", snapshot))
                    : Map.of("type", "GAME_READY", "data", Map.of("initialState", snapshot));
                messaging.convertAndSend("/topic/game/" + gameId, payload);
                if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
                    messaging.convertAndSendToUser(principal.getName(), "/queue/game/" + gameId, payload);
                }
            }
        } catch (Exception ignored) {
            log.warn("No se pudo enviar snapshot en reconnect para gameId={} principal={}.",
                gameId,
                principal != null ? principal.getName() : "null",
                ignored);
        }
    }
}
