package com.pokemon.tcg.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingBenchDamage {
    private int  damageCounters;
    private Long targetPlayerId;
}
