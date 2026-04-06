package com.pokemon.tcg.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ActionResultDTO {
    private boolean     success;
    private String      errorMessage;
    private Object      updatedGameState;   // GameStateDTO serializado
    private List<Map<String,Object>> events;
    private boolean     gameOver;
    private Long        winnerId;
    private String      victoryCondition;
}
