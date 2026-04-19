package com.pokemon.tcg.loader.xy;

import com.pokemon.tcg.loader.xy.dto.XyAbilityDto;
import com.pokemon.tcg.loader.xy.dto.XyAttackDto;
import com.pokemon.tcg.loader.xy.dto.XyCardDto;
import com.pokemon.tcg.loader.xy.dto.XyCardImagesDto;
import com.pokemon.tcg.loader.xy.dto.XyExpansionDto;
import com.pokemon.tcg.loader.xy.dto.XyWeaknessResistanceDto;
import com.pokemon.tcg.model.game.AbilityData;
import com.pokemon.tcg.model.game.AttackData;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.CardImages;
import com.pokemon.tcg.model.game.SetData;
import com.pokemon.tcg.model.game.WeaknessResistance;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// Convierte XyCardDto → CardData (modelo de dominio)
// Se adapta al modelo existente: no agrega campos de precio, rareza ni artista
@Component
public class XyCardMapper {

    // Punto de entrada principal — recibe también la expansión para poblar SetData
    public CardData toCardData(XyCardDto dto, XyExpansionDto expansion) {
        // El campo "rules" del JSON tiene significado diferente según tipo:
        // - Pokémon EX/MEGA: regla de 2 premios al ser KO
        // - Trainer: efecto completo de la carta
        // - Energy especial: descripción del efecto
        // Se mapea rules[0] al campo "text" del modelo de dominio
        String text = null;
        if (dto.getRules() != null && !dto.getRules().isEmpty()) {
            text = dto.getRules().get(0);
        }

        SetData setData = null;
        if (expansion != null) {
            setData = new SetData(expansion.getId(), expansion.getName(), expansion.getSeries());
        }

        return CardData.builder()
            .id(dto.getId())
            .name(dto.getName())
            .supertype(dto.getSupertype())
            .subtypes(dto.getSubtypes())
            .hp(parseHp(dto.getHp()))
            .types(dto.getTypes())
            .evolvesFrom(dto.getEvolvesFrom())
            .attacks(mapAttacks(dto.getAttacks()))
            .abilities(mapAbilities(dto.getAbilities()))
            .weaknesses(mapWeaknessResistance(dto.getWeaknesses()))
            .resistances(mapWeaknessResistance(dto.getResistances()))
            .retreatCost(dto.getRetreatCost())
            .convertedRetreatCost(dto.getConvertedRetreatCost())
            .text(text)
            .images(mapImages(dto.getImages()))
            .set(setData)
            .build();
    }

    // hp viene como String ("160") en el JSON — null en Trainers y Energy
    private Integer parseHp(String hp) {
        if (hp == null || hp.isBlank()) return null;
        try {
            return Integer.parseInt(hp.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<AttackData> mapAttacks(List<XyAttackDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream()
            .map(d -> AttackData.builder()
                .name(d.getName())
                .cost(d.getCost())
                .convertedEnergyCost(d.getConvertedEnergyCost())
                .damage(d.getDamage())   // se preserva "60", "60+", "60×" — AttackData ya maneja la lógica
                .text(d.getText())
                .build())
            .collect(Collectors.toList());
    }

    private List<AbilityData> mapAbilities(List<XyAbilityDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream()
            .map(d -> AbilityData.builder()
                .name(d.getName())
                .text(d.getText())
                .type(d.getType())
                .build())
            .collect(Collectors.toList());
    }

    private List<WeaknessResistance> mapWeaknessResistance(List<XyWeaknessResistanceDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream()
            .map(d -> new WeaknessResistance(d.getType(), d.getValue()))
            .collect(Collectors.toList());
    }

    private CardImages mapImages(XyCardImagesDto dto) {
        if (dto == null) return null;
        return new CardImages(dto.getSmall(), dto.getLarge());
    }
}
