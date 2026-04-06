package com.pokemon.tcg.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetData {
    private String id;      // "base1"
    private String name;    // "Base"
    private String series;  // "Base"
}
