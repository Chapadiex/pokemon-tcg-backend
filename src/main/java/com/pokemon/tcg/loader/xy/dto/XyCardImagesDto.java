package com.pokemon.tcg.loader.xy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

// URLs de imagen de la carta (small para UI lista, large para modal de detalle)
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class XyCardImagesDto {
    private String small;
    private String large;
}
