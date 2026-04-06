package com.pokemon.tcg.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbilityData {
    private String name;  // "Energy Burn"
    private String text;  // descripción del efecto
    private String type;  // "Poké-Power" | "Poké-Body" | "Ability"
}
