package com.pokemon.tcg.model.dto;

import com.pokemon.tcg.model.game.AttackData;
import com.pokemon.tcg.model.game.WeaknessResistance;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CardDTO {
    private String       id;
    private String       name;
    private String       supertype;
    private List<String> subtypes;
    private Integer      hp;
    private List<String> types;
    private String       evolvesFrom;
    private List<AttackData>          attacks;
    private List<WeaknessResistance>  weaknesses;
    private List<WeaknessResistance>  resistances;
    private Integer      convertedRetreatCost;
    private String       text;
    private String       imageUrl;     // extraído de images.small
    private String       setName;      // extraído de set.name

    public static CardDTO from(com.pokemon.tcg.model.game.CardData card) {
        return CardDTO.builder()
            .id(card.getId())
            .name(card.getName())
            .supertype(card.getSupertype())
            .subtypes(card.getSubtypes())
            .hp(card.getHp())
            .types(card.getTypes())
            .evolvesFrom(card.getEvolvesFrom())
            .attacks(card.getAttacks())
            .weaknesses(card.getWeaknesses())
            .resistances(card.getResistances())
            .convertedRetreatCost(card.getConvertedRetreatCost())
            .text(card.getText())
            .imageUrl(card.getImageUrl())
            .setName(card.getSet() != null ? card.getSet().getName() : null)
            .build();
    }
}
