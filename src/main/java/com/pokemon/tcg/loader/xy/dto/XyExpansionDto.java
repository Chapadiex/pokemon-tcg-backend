package com.pokemon.tcg.loader.xy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de la sección "expansion" del JSON
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class XyExpansionDto {
    private String id;
    private String name;
    private String series;
    private int total;
    private String releaseDate;
    private String ptcgoCode;
    private XyExpansionImagesDto images;
}
