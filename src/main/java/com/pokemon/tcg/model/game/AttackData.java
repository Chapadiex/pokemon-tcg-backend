package com.pokemon.tcg.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttackData {
    private String name;
    private List<String> cost;
    private Integer convertedEnergyCost;
    private String damage;
    private String text;

    public int getBaseDamageValue() {
        if (damage == null || damage.isBlank()) {
            return 0;
        }

        String number = damage.replaceAll("[^0-9]", "");
        return number.isBlank() ? 0 : Integer.parseInt(number);
    }

    public boolean hasVariableDamage() {
        return damage != null && (damage.contains("\u00d7") || damage.contains("+"));
    }

    public int getTotalEnergyCost() {
        return cost != null ? cost.size() : 0;
    }
}
