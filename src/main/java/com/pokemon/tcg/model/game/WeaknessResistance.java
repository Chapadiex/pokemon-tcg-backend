package com.pokemon.tcg.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeaknessResistance {
    private String type;
    private String value;

    public int computeModifier(int baseDamage) {
        if (value == null) {
            return 0;
        }

        if (value.startsWith("\u00d7")) {
            int multiplier = Integer.parseInt(value.substring(1));
            return baseDamage * (multiplier - 1);
        }

        if (value.startsWith("+")) {
            return Integer.parseInt(value.substring(1));
        }

        if (value.startsWith("-")) {
            return -Integer.parseInt(value.substring(1));
        }

        return 0;
    }
}
