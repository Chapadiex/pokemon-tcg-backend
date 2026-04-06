package com.pokemon.tcg.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MulliganResult {
    private boolean bothReady;
    private boolean continueLoop;
    private boolean p1CanDrawExtra;
    private boolean p2CanDrawExtra;
    @Builder.Default
    private List<GameEvent> events = new ArrayList<>();

    public static MulliganResult bothReady() {
        return MulliganResult.builder()
            .bothReady(true)
            .continueLoop(false)
            .events(new ArrayList<>())
            .build();
    }

    public static MulliganResult continueLoop(List<GameEvent> events, boolean p1Extra, boolean p2Extra) {
        return MulliganResult.builder()
            .bothReady(false)
            .continueLoop(true)
            .p1CanDrawExtra(p1Extra)
            .p2CanDrawExtra(p2Extra)
            .events(events)
            .build();
    }
}
