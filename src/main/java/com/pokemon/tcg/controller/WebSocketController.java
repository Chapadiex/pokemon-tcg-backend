package com.pokemon.tcg.controller;

import com.pokemon.tcg.engine.BenchDamageResolver;
import com.pokemon.tcg.engine.KnockoutReplacementHandler;
import com.pokemon.tcg.engine.model.ActionResult;
import com.pokemon.tcg.model.dto.GameActionMessage;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.repository.GameRepository;
import com.pokemon.tcg.service.TurnService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Map;

/**
 * Maneja las acciones de turno durante el juego activo y las acciones
 * de reemplazo (KO y daño a Banca) a través de WebSocket STOMP.
 *
 * Las acciones de la fase de setup se delegan a {@link SetupController}.
 */
@Controller
@Transactional
public class WebSocketController {

    private final TurnService turnService;
    private final KnockoutReplacementHandler replacementHandler;
    private final BenchDamageResolver benchDamageResolver;
    private final GameRepository gameRepository;

    public WebSocketController(TurnService turnService,
                                KnockoutReplacementHandler replacementHandler,
                                BenchDamageResolver benchDamageResolver,
                                GameRepository gameRepository) {
        this.turnService          = turnService;
        this.replacementHandler   = replacementHandler;
        this.benchDamageResolver  = benchDamageResolver;
        this.gameRepository       = gameRepository;
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
     * El jugador elige su Pokémon de reemplazo tras un KO.
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
     * El atacante elige el objetivo de daño a Banca.
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
}
