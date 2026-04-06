package com.pokemon.tcg.model.game;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnergyAttached {
    private String cardId;  // ID de la carta de Energía
    private String type;    // "Fire" | "Water" | "Colorless" | etc.
    private String name;    // "Fire Energy"
}
