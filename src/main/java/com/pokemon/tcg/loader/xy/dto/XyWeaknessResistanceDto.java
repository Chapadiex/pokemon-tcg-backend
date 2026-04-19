package com.pokemon.tcg.loader.xy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO reutilizado para debilidades y resistencias
// Debilidad: value = "×2" | Resistencia: value = "-20"
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class XyWeaknessResistanceDto {
    private String type;   // tipo de Pokémon: "Fire", "Water", "Fighting", etc.
    private String value;  // "×2" o "-20"
}
