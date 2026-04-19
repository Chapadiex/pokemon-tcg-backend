package com.pokemon.tcg.loader.xy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO de un ataque dentro de una carta Pokémon
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class XyAttackDto {
    private String name;
    private List<String> cost;       // ej: ["Grass", "Colorless", "Colorless"]
    private int convertedEnergyCost;
    private String damage;           // "60", "60+", "60×", "" — ver patrones en FASE1_PARSER_SET_XY.md
    private String text;             // efecto del ataque — puede ser vacío o null
}
