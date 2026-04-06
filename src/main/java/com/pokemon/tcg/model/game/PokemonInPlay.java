package com.pokemon.tcg.model.game;

import com.pokemon.tcg.model.enums.RotationCondition;
import com.pokemon.tcg.model.enums.StatusCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PokemonInPlay {
    private String cardId;
    private String name;
    private String imageUrl;
    private Integer hp;
    private List<String> types;
    private List<AttackData> attacks;
    private List<AbilityData> abilities;
    private List<WeaknessResistance> weaknesses;
    private List<WeaknessResistance> resistances;
    private Integer retreatCost;
    private Integer damageCounters;
    private List<EnergyAttached> attachedEnergies;
    private CardData attachedTool;
    private List<CardData> evolutions;

    /** Condición de rotación — mutuamente excluyentes. null = ninguna. */
    private RotationCondition rotationCondition;

    /** Marcador de Envenenado — puede coexistir con rotationCondition e isBurned */
    private boolean poisoned;

    /** Marcador de Quemado — puede coexistir con rotationCondition e isPoisoned */
    private boolean burned;

    private Integer turnsSinceInPlay;
    private Boolean evolvedThisTurn;
    private List<ActiveEffect> activeEffects;

    public int getCurrentHp() {
        int baseHp = hp != null ? hp : 0;
        int counters = damageCounters != null ? damageCounters : 0;
        return Math.max(0, baseHp - counters * 10);
    }

    public boolean isKnockedOut() {
        return getCurrentHp() <= 0;
    }

    public boolean isAsleep()    { return rotationCondition == RotationCondition.ASLEEP; }
    public boolean isConfused()  { return rotationCondition == RotationCondition.CONFUSED; }
    public boolean isParalyzed() { return rotationCondition == RotationCondition.PARALYZED; }

    public boolean canAttack() {
        return rotationCondition != RotationCondition.ASLEEP
            && rotationCondition != RotationCondition.PARALYZED;
    }

    public boolean canRetreat() {
        return rotationCondition != RotationCondition.ASLEEP
            && rotationCondition != RotationCondition.PARALYZED;
    }

    public boolean hasAnyCondition() {
        return rotationCondition != null || poisoned || burned;
    }

    public int countEnergyOfType(String type) {
        if (attachedEnergies == null) {
            return 0;
        }

        return (int) attachedEnergies.stream().filter(energy -> type.equals(energy.getType())).count();
    }

    public int totalEnergies() {
        return attachedEnergies != null ? attachedEnergies.size() : 0;
    }

    public static PokemonInPlay fromCard(CardData card) {
        return PokemonInPlay.builder()
            .cardId(card.getId())
            .name(card.getName())
            .imageUrl(card.getImageUrl())
            .hp(card.getHp())
            .types(card.getTypes() != null ? new ArrayList<>(card.getTypes()) : new ArrayList<>())
            .attacks(card.getAttacks() != null ? new ArrayList<>(card.getAttacks()) : new ArrayList<>())
            .abilities(card.getAbilities() != null ? new ArrayList<>(card.getAbilities()) : new ArrayList<>())
            .weaknesses(card.getWeaknesses() != null ? new ArrayList<>(card.getWeaknesses()) : new ArrayList<>())
            .resistances(card.getResistances() != null ? new ArrayList<>(card.getResistances()) : new ArrayList<>())
            .retreatCost(card.getConvertedRetreatCost() != null ? card.getConvertedRetreatCost() : 0)
            .damageCounters(0)
            .attachedEnergies(new ArrayList<>())
            .evolutions(new ArrayList<>())
            .activeEffects(new ArrayList<>())
            .turnsSinceInPlay(0)
            .evolvedThisTurn(false)
            .build();
    }

    public static PokemonInPlay evolve(PokemonInPlay current, CardData evolutionCard) {
        List<CardData> newEvolutions = new ArrayList<>(current.getEvolutions());
        CardData baseCard = CardData.builder().id(current.getCardId()).name(current.getName()).build();
        newEvolutions.add(baseCard);

        PokemonInPlay evolved = fromCard(evolutionCard);
        evolved.setDamageCounters(current.getDamageCounters());
        evolved.setAttachedEnergies(current.getAttachedEnergies());
        evolved.setAttachedTool(current.getAttachedTool());
        evolved.setEvolutions(newEvolutions);
        evolved.setTurnsSinceInPlay(current.getTurnsSinceInPlay());
        evolved.setEvolvedThisTurn(true);
        evolved.setRotationCondition(null);
        evolved.setPoisoned(false);
        evolved.setBurned(false);
        evolved.setActiveEffects(new ArrayList<>());
        return evolved;
    }
}
