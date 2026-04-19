package com.pokemon.tcg.loader.xy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

// DTO de una carta individual del set XY
// Todos los campos opcionales son null-safe (no lanza NPE si el campo no viene)
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class XyCardDto {
    private String id;
    private String name;
    private String supertype;
    private List<String> subtypes;
    private String hp;                              // String en el JSON ("180"), null en Trainers/Energy
    private List<String> types;                     // null en Trainers/Energy
    private String evolvesFrom;                     // OPCIONAL
    private List<String> evolvesTo;                 // OPCIONAL
    private List<String> rules;                     // OPCIONAL — EX rule / Trainer text / Special Energy text
    private List<XyAttackDto> attacks;              // OPCIONAL — Trainers y Energy no tienen
    private List<XyAbilityDto> abilities;           // OPCIONAL — solo 12 Pokémon en XY
    private List<XyWeaknessResistanceDto> weaknesses;
    private List<XyWeaknessResistanceDto> resistances;  // OPCIONAL
    private List<String> retreatCost;               // OPCIONAL
    private int convertedRetreatCost;
    private String number;
    private XyCardImagesDto images;
}
