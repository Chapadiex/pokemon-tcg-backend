package com.pokemon.tcg.loader.xy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de una habilidad Pokémon
// En el set XY todos los abilities tienen type = "Ability"
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class XyAbilityDto {
    private String name;
    private String text;
    private String type;  // siempre "Ability" en XY
}
