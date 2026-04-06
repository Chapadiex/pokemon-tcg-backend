package com.pokemon.tcg.controller;

import com.pokemon.tcg.engine.KnockoutReplacementHandler;
import com.pokemon.tcg.engine.model.ActionResult;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.exception.InvalidMoveException;
import com.pokemon.tcg.model.dto.GameActionMessage;
import com.pokemon.tcg.model.dto.SetupActionMessage;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.game.PokemonInPlay;
import com.pokemon.tcg.repository.GameRepository;
import com.pokemon.tcg.service.GameStateMapper;
import com.pokemon.tcg.service.SetupService;
import com.pokemon.tcg.service.TurnService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@Transactional
public class WebSocketController {

    private final SetupService setupService;
    private final TurnService turnService;
    private final KnockoutReplacementHandler replacementHandler;
    private final GameRepository gameRepository;
    private final GameStateMapper gameStateMapper;
    private final SimpMessagingTemplate messaging;

    public WebSocketController(SetupService setupService, TurnService turnService,
                                KnockoutReplacementHandler replacementHandler,
                                GameRepository gameRepository,
                                GameStateMapper gameStateMapper,
                                SimpMessagingTemplate messaging) {
        this.setupService        = setupService;
        this.turnService         = turnService;
        this.replacementHandler  = replacementHandler;
        this.gameRepository      = gameRepository;
        this.gameStateMapper     = gameStateMapper;
        this.messaging           = messaging;
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
     * Acciones de la fase de setup (mulligan + colocación inicial).
     * Destino: /app/game/{gameId}/setup
     */
    @MessageMapping("/game/{gameId}/setup")
    public void handleSetupAction(@DestinationVariable Long gameId,
                                   @Payload SetupActionMessage action,
                                   Principal principal) {
        Long playerId = Long.parseLong(principal.getName());

        switch (action.getType()) {
            case "PLACE_POKEMON"     -> setupService.placePokemonDuringSetup(
                gameId, playerId,
                (String) action.getData().get("cardId"),
                (String) action.getData().get("position"));
            case "READY_TO_START"    -> setupService.playerReadyToStart(gameId, playerId);
            case "ACCEPT_EXTRA_DRAW" -> setupService.applyExtraDraw(gameId, playerId);
            case "DECLINE_EXTRA_DRAW"-> setupService.declineExtraDraw(gameId, playerId);
            default -> throw new IllegalArgumentException("Acción de setup no soportada: " + action.getType());
        }
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
        GameStateSnapshot snapshot = gameStateMapper.loadSnapshot(game);

        if (!playerId.equals(snapshot.getCurrentTurnPlayerId())) {
            throw new InvalidMoveException("No es tu turno para elegir objetivo");
        }

        List<PokemonInPlay> opponentBench = snapshot.getOpponentId().equals(snapshot.getPlayer1Id())
            ? snapshot.getP1Bench() : snapshot.getP2Bench();

        if (benchIndex >= opponentBench.size()) {
            throw new InvalidMoveException("Índice de Banca inválido");
        }

        com.pokemon.tcg.engine.model.PendingBenchDamage pending =
            snapshot.getPendingBenchDamage().remove(0);
        PokemonInPlay target = opponentBench.get(benchIndex);
        target.setDamageCounters(target.getDamageCounters() + pending.getDamageCounters());

        gameStateMapper.saveSnapshot(game, snapshot);
        messaging.convertAndSend("/topic/game/" + gameId,
            Map.of("type", "BENCH_DAMAGE_APPLIED",
                   "target", target.getName(),
                   "damage", pending.getDamageCounters() * 10));
    }
}
