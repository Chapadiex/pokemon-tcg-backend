package com.pokemon.tcg.loader.xy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

// URLs de símbolo y logo del set
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class XyExpansionImagesDto {
    private String symbol;
    private String logo;
}
