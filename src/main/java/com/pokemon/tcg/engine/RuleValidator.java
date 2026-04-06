package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.ValidationResult;
import com.pokemon.tcg.model.game.AttackData;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.EnergyAttached;
import com.pokemon.tcg.model.game.PokemonInPlay;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RuleValidator {

    public ValidationResult validateDeckComposition(List<CardData> cards) {
        if (cards == null || cards.size() != 60) {
            return ValidationResult.fail("El mazo debe tener exactamente 60 cartas (tiene " + (cards == null ? 0 : cards.size()) + ")");
        }

        Map<String, Long> counts = cards.stream()
            .collect(Collectors.groupingBy(CardData::getName, Collectors.counting()));

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            boolean basicEnergy = cards.stream()
                .filter(card -> entry.getKey().equals(card.getName()))
                .anyMatch(this::isBasicEnergy);

            if (!basicEnergy && entry.getValue() > 4) {
                return ValidationResult.fail("Hay mas de 4 copias de '" + entry.getKey() + "' (" + entry.getValue() + ")");
            }
        }

        boolean hasBasicPokemon = cards.stream().anyMatch(CardData::isBasicPokemon);
        if (!hasBasicPokemon) {
            return ValidationResult.fail("El mazo debe contener al menos 1 Pokemon Basico");
        }

        return ValidationResult.ok();
    }

    public boolean handHasBasicPokemon(List<CardData> hand) {
        return hand != null && hand.stream().anyMatch(CardData::isBasicPokemon);
    }

    public ValidationResult validatePlacePokemon(CardData card, String position, GameStateSnapshot state) {
        if (card == null || !card.isPokemon()) {
            return ValidationResult.fail("Solo se pueden colocar cartas de Pokemon");
        }
        if (!card.isBasicPokemon()) {
            return ValidationResult.fail("Solo se pueden colocar Pokemon Basicos desde la mano");
        }
        if ("BENCH".equals(position) && state.getCurrentPlayerBench().size() >= 5) {
            return ValidationResult.fail("La banca esta llena (maximo 5 Pokemon)");
        }
        if (!state.getCurrentPlayerHand().contains(card)) {
            return ValidationResult.fail("La carta no esta en tu mano");
        }
        return ValidationResult.ok();
    }

    public ValidationResult validateEvolution(PokemonInPlay target, CardData evolutionCard, GameStateSnapshot state) {
        if (Boolean.TRUE.equals(state.getIsFirstTurn())) {
            return ValidationResult.fail("No se puede evolucionar en el primer turno");
        }
        if (target == null || evolutionCard == null || evolutionCard.getEvolvesFrom() == null || !evolutionCard.getEvolvesFrom().equals(target.getName())) {
            return ValidationResult.fail((evolutionCard != null ? evolutionCard.getName() : "La carta") + " no evoluciona de " + (target != null ? target.getName() : "ese Pokemon"));
        }
        if (target.getTurnsSinceInPlay() < 1) {
            return ValidationResult.fail(target.getName() + " lleva solo 0 turnos en juego - debe esperar 1 turno");
        }
        if (Boolean.TRUE.equals(target.getEvolvedThisTurn())) {
            return ValidationResult.fail(target.getName() + " ya evoluciono este turno");
        }
        boolean inHand = state.getCurrentPlayerHand().stream().anyMatch(card -> evolutionCard.getId().equals(card.getId()));
        if (!inHand) {
            return ValidationResult.fail("La carta de evolucion no esta en tu mano");
        }
        return ValidationResult.ok();
    }

    public ValidationResult validateAttachEnergy(CardData energyCard, PokemonInPlay target, GameStateSnapshot state) {
        if (energyCard == null || !energyCard.isEnergy()) {
            return ValidationResult.fail("Solo se pueden unir cartas de Energia");
        }
        if (Boolean.TRUE.equals(state.getEnergyAttachedThisTurn())) {
            return ValidationResult.fail("Ya uniste una Energia este turno");
        }
        if (target == null) {
            return ValidationResult.fail("El Pokemon objetivo no existe");
        }
        return ValidationResult.ok();
    }

    public ValidationResult validatePlayTrainer(CardData trainerCard, GameStateSnapshot state) {
        if (trainerCard == null || !trainerCard.isTrainer()) {
            return ValidationResult.fail("La carta no es de Entrenador");
        }
        List<String> subtypes = trainerCard.getSubtypes();
        if (subtypes != null && subtypes.contains("Supporter") && Boolean.TRUE.equals(state.getSupporterPlayedThisTurn())) {
            return ValidationResult.fail("Solo se puede jugar un Partidario por turno");
        }
        if (subtypes != null && subtypes.contains("Stadium") && Boolean.TRUE.equals(state.getStadiumPlayedThisTurn())) {
            return ValidationResult.fail("Solo se puede jugar un Estadio por turno");
        }
        return ValidationResult.ok();
    }

    public ValidationResult validateRetreat(PokemonInPlay activePokemon, List<EnergyAttached> energiesToDiscard, GameStateSnapshot state) {
        if (Boolean.TRUE.equals(state.getRetreatUsedThisTurn())) {
            return ValidationResult.fail("Ya retiraste un Pokemon este turno");
        }
        if (activePokemon.isAsleep()) {
            return ValidationResult.fail("El Pokemon dormido no puede retirarse");
        }
        if (activePokemon.isParalyzed()) {
            return ValidationResult.fail("El Pokemon paralizado no puede retirarse");
        }
        if (state.getCurrentPlayerBench().isEmpty()) {
            return ValidationResult.fail("No hay Pokemon en la banca para reemplazar");
        }
        int retreatCost = activePokemon.getRetreatCost() != null ? activePokemon.getRetreatCost() : 0;
        if (energiesToDiscard == null || energiesToDiscard.size() < retreatCost) {
            return ValidationResult.fail("Necesitas descartar " + retreatCost + " Energias para retirar (tienes " + (energiesToDiscard == null ? 0 : energiesToDiscard.size()) + ")");
        }
        return ValidationResult.ok();
    }

    public ValidationResult validateAttack(PokemonInPlay attacker, int attackIndex, GameStateSnapshot state) {
        if (Boolean.TRUE.equals(state.getIsFirstTurn()) && state.getCurrentTurnPlayerId().equals(state.getPlayer1Id())) {
            return ValidationResult.fail("El jugador que empieza no puede atacar en su primer turno");
        }
        if (attacker == null) {
            return ValidationResult.fail("No tienes Pokemon activo");
        }
        if (attacker.isAsleep()) {
            return ValidationResult.fail("El Pokemon dormido no puede atacar");
        }
        if (attacker.isParalyzed()) {
            return ValidationResult.fail("El Pokemon paralizado no puede atacar");
        }
        if (attacker.getAttacks() == null || attackIndex < 0 || attackIndex >= attacker.getAttacks().size()) {
            return ValidationResult.fail("Indice de ataque invalido");
        }
        return validateEnoughEnergyForAttack(attacker, attacker.getAttacks().get(attackIndex));
    }

    private ValidationResult validateEnoughEnergyForAttack(PokemonInPlay pokemon, AttackData attack) {
        if (attack.getCost() == null || attack.getCost().isEmpty()) {
            return ValidationResult.ok();
        }

        List<EnergyAttached> attachedEnergies = pokemon.getAttachedEnergies();
        if (attachedEnergies == null) {
            attachedEnergies = List.of();
        }

        Map<String, Long> required = attack.getCost().stream()
            .collect(Collectors.groupingBy(cost -> cost, Collectors.counting()));
        Map<String, Long> available = attachedEnergies.stream()
            .collect(Collectors.groupingBy(EnergyAttached::getType, Collectors.counting()));

        long totalAvailable = attachedEnergies.size();
        long totalRequired = attack.getCost().size();
        if (totalAvailable < totalRequired) {
            return ValidationResult.fail("Faltan " + (totalRequired - totalAvailable) + " Energias para usar " + attack.getName());
        }

        for (Map.Entry<String, Long> requirement : required.entrySet()) {
            if ("Colorless".equals(requirement.getKey())) {
                continue;
            }
            long have = available.getOrDefault(requirement.getKey(), 0L);
            if (have < requirement.getValue()) {
                return ValidationResult.fail("Falta " + (requirement.getValue() - have) + " Energia de tipo " + requirement.getKey());
            }
        }

        return ValidationResult.ok();
    }

    private boolean isBasicEnergy(CardData card) {
        return card != null && card.isEnergy() && card.getSubtypes() != null && card.getSubtypes().contains("Basic");
    }
}