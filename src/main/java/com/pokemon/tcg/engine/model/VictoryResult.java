package com.pokemon.tcg.engine.model;

import com.pokemon.tcg.model.enums.VictoryCondition;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VictoryResult {
    private boolean gameOver;
    private Long winnerId;
    private VictoryCondition condition;
    private boolean suddenDeath;

    public static VictoryResult win(Long winnerId, VictoryCondition condition) {
        return VictoryResult.builder()
            .gameOver(true)
            .winnerId(winnerId)
            .condition(condition)
            .build();
    }

    public static VictoryResult noWinner() {
        return VictoryResult.builder().gameOver(false).build();
    }

    public static VictoryResult suddenDeath() {
        return VictoryResult.builder()
            .gameOver(true)
            .suddenDeath(true)
            .condition(VictoryCondition.SUDDEN_DEATH)
            .build();
    }
}
