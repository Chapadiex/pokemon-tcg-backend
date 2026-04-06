package com.pokemon.tcg.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardImages {
    private String small;   // URL 245×342 (usar en tablero)
    private String large;   // URL alta resolución (usar en detalle)
}
