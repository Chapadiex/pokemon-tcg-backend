package com.pokemon.tcg.engine.model;

import com.pokemon.tcg.model.enums.VictoryCondition;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ActionResult {
    private final boolean success;
    private final String errorMessage;
    private final GameStateSnapshot updatedState;
    private final List<GameEvent> events;
    private final boolean gameOver;
    private final Long winnerId;
    private final VictoryCondition victoryCondition;

    public static ActionResult fail(String message, GameStateSnapshot state) {
        return ActionResult.builder()
            .success(false)
            .errorMessage(message)
            .updatedState(state)
            .events(List.of())
            .gameOver(false)
            .build();
    }
}
