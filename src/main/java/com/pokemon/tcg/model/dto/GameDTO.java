package com.pokemon.tcg.model.dto;

import com.pokemon.tcg.model.entity.Game;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameDTO {
    private Long id;
    private String status;
    private PlayerDTO player1;
    private PlayerDTO player2;
    private Long currentTurnPlayerId;
    private Integer turnNumber;
    private Long winnerId;
    private String victoryCondition;
    private String createdAt;

    public static GameDTO from(Game game) {
        return GameDTO.builder()
            .id(game.getId())
            .status(game.getStatus() != null ? game.getStatus().name() : null)
            .player1(game.getPlayer1() != null ? PlayerDTO.from(game.getPlayer1()) : null)
            .player2(game.getPlayer2() != null ? PlayerDTO.from(game.getPlayer2()) : null)
            .currentTurnPlayerId(game.getCurrentTurnPlayerId())
            .turnNumber(game.getTurnNumber())
            .winnerId(game.getWinner() != null ? game.getWinner().getId() : null)
            .victoryCondition(game.getVictoryCondition() != null ? game.getVictoryCondition().name() : null)
            .createdAt(game.getCreatedAt() != null ? game.getCreatedAt().toString() : null)
            .build();
    }
}
