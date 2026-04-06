package com.pokemon.tcg.model.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupPhaseState {
    private int player1MulliganCount;
    private int player2MulliganCount;
    private boolean player1CanDrawExtra;
    private boolean player2CanDrawExtra;
    private SetupStep currentStep;
    private boolean player1PlacedActive;
    private boolean player2PlacedActive;
    private boolean player1ReadyToStart;
    private boolean player2ReadyToStart;

    public static SetupPhaseState initial() {
        return SetupPhaseState.builder()
            .currentStep(SetupStep.MULLIGAN_CHECK)
            .build();
    }

    public enum SetupStep {
        MULLIGAN_CHECK,
        WAITING_EXTRA_DRAW,
        INITIAL_PLACEMENT,
        COIN_FLIP,
        REVEALING,
        COMPLETE
    }
}
