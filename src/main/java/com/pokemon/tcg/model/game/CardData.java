package com.pokemon.tcg.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pokemon.tcg.config.HpDeserializer;
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
public class CardData {
    private String id;
    private String name;
    private String supertype;
    private List<String> subtypes;

    @JsonDeserialize(using = HpDeserializer.class)
    private Integer hp;

    private List<String> types;
    private String evolvesFrom;
    private List<AttackData> attacks;
    private List<AbilityData> abilities;
    private List<WeaknessResistance> weaknesses;
    private List<WeaknessResistance> resistances;
    private List<String> retreatCost;
    private Integer convertedRetreatCost;
    private String text;
    private CardImages images;
    private SetData set;

    public boolean isPokemon() {
        return "Pok\u00e9mon".equals(supertype);
    }

    public boolean isTrainer() {
        return "Trainer".equals(supertype);
    }

    public boolean isEnergy() {
        return "Energy".equals(supertype);
    }

    public boolean isBasicPokemon() {
        return isPokemon() && subtypes != null && subtypes.contains("Basic");
    }

    public boolean isStage1() {
        return isPokemon() && subtypes != null && subtypes.contains("Stage 1");
    }

    public boolean isStage2() {
        return isPokemon() && subtypes != null && subtypes.contains("Stage 2");
    }

    public boolean isEvolution() {
        return isStage1() || isStage2();
    }

    public boolean isBasicEnergy() {
        return isEnergy() && subtypes != null && subtypes.contains("Basic");
    }

    public boolean isSpecialEnergy() {
        return isEnergy() && subtypes != null && subtypes.contains("Special");
    }

    public boolean isItem() {
        return isTrainer() && subtypes != null && subtypes.contains("Item");
    }

    public boolean isSupporter() {
        return isTrainer() && subtypes != null && subtypes.contains("Supporter");
    }

    public boolean isStadium() {
        return isTrainer() && subtypes != null && subtypes.contains("Stadium");
    }

    public boolean isPokemonTool() {
        return isTrainer() && subtypes != null && subtypes.contains("Pok\u00e9mon Tool");
    }

    public String getImageUrl() {
        return images != null ? images.getSmall() : null;
    }

    public int getRetreatCostCount() {
        return retreatCost != null ? retreatCost.size() : 0;
    }

    public String getPrimaryType() {
        return types != null && !types.isEmpty() ? types.get(0) : "Colorless";
    }
}
