package com.pokemon.tcg.loader.xy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO raíz del archivo setXY_completo.json
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class XySetFileDto {
    private XyExpansionDto expansion;
    private int totalCards;

    @JsonProperty("data")
    private List<XyCardDto> cards;
}
