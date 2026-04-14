package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.dto.SetupActionMessage;
import com.pokemon.tcg.service.SetupService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

/**
 * Maneja las acciones de la fase de setup (mulligan + colocación inicial)
 * a través de WebSocket STOMP.
 *
 * Destino: /app/game/{gameId}/setup
 */
@Controller
@Transactional
public class SetupController {

    private final SetupService setupService;

    public SetupController(SetupService setupService) {
        this.setupService = setupService;
    }

    @MessageMapping("/game/{gameId}/setup")
    public void handleSetupAction(@DestinationVariable Long gameId,
                                   @Payload SetupActionMessage action,
                                   Principal principal) {
        Long playerId = Long.parseLong(principal.getName());

        switch (action.getType()) {
            case "PLACE_POKEMON"      -> setupService.placePokemonDuringSetup(
                    gameId, playerId,
                    (String) action.getData().get("cardId"),
                    (String) action.getData().get("position"));
            case "READY_TO_START"     -> setupService.playerReadyToStart(gameId, playerId);
            case "ACCEPT_EXTRA_DRAW"  -> setupService.applyExtraDraw(gameId, playerId);
            case "DECLINE_EXTRA_DRAW" -> setupService.declineExtraDraw(gameId, playerId);
            default -> throw new IllegalArgumentException(
                    "Acción de setup no soportada: " + action.getType());
        }
    }
}
