package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.ActionResult;
import com.pokemon.tcg.engine.model.BetweenTurnsResult;
import com.pokemon.tcg.engine.model.GameEvent;
import com.pokemon.tcg.engine.model.GameEventType;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.KnockoutResult;
import com.pokemon.tcg.engine.model.ValidationResult;
import com.pokemon.tcg.engine.model.VictoryResult;
import com.pokemon.tcg.model.game.AttackData;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.EnergyAttached;
import com.pokemon.tcg.model.game.PokemonInPlay;
import com.pokemon.tcg.util.CardSelectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class GameEngine {

    private final RuleValidator ruleValidator;
    private final DamageCalculator damageCalculator;
    private final StatusEffectManager statusEffectManager;
    private final VictoryConditionChecker victoryConditionChecker;
    private final TurnManager turnManager;
    private final AttackEffectParser attackEffectParser;
    private final Random random = new Random();

    /** Constructor para Spring (inyección de dependencias). */
    @Autowired
    public GameEngine(RuleValidator ruleValidator, DamageCalculator damageCalculator,
                      StatusEffectManager statusEffectManager,
                      VictoryConditionChecker victoryConditionChecker,
                      AttackEffectParser attackEffectParser) {
        this.ruleValidator = ruleValidator;
        this.damageCalculator = damageCalculator;
        this.statusEffectManager = statusEffectManager;
        this.victoryConditionChecker = victoryConditionChecker;
        this.attackEffectParser = attackEffectParser;
        this.turnManager = new TurnManager(ruleValidator, damageCalculator,
                statusEffectManager, victoryConditionChecker);
    }

    /** Constructor sin argumentos para tests unitarios. */
    public GameEngine() {
        this(new RuleValidator(), new DamageCalculator(),
                new StatusEffectManager(), new VictoryConditionChecker(),
                new AttackEffectParser(new StatusEffectManager()));
    }

    // ── Robar carta (inicio de turno) ─────────────────────────────────────

    public ActionResult drawCard(GameStateSnapshot state) {
        return turnManager.startTurn(state, () -> random.nextBoolean());
    }

    /** Alias de retrocompatibilidad con tests que inyectan CoinFlipper. */
    public ActionResult startTurn(GameStateSnapshot state,
                                   com.pokemon.tcg.engine.model.CoinFlipper coinFlipper) {
        return turnManager.startTurn(state, coinFlipper);
    }

    // ── Colocar Pokémon Básico desde la mano ──────────────────────────────

    public ActionResult placePokemon(GameStateSnapshot state, String cardId, String position) {
        return placePokemon(state, cardId, position, null);
    }

    public ActionResult placePokemon(GameStateSnapshot state, String cardId, String position, Integer handIndex) {
        CardData card = findInHand(state.getCurrentPlayerHand(), cardId, handIndex);
        if (card == null) return ActionResult.fail("Carta no encontrada en la mano: " + cardId, state);

        ValidationResult v = ruleValidator.validatePlacePokemon(card, position, state);
        if (!v.isValid()) return ActionResult.fail(v.getErrorMessage(), state);

        boolean removed = CardSelectionUtil.removeFromHand(state.getCurrentPlayerHand(), cardId, handIndex);
        if (!removed) return ActionResult.fail("No se pudo remover la carta seleccionada de la mano", state);
        PokemonInPlay pokemon = PokemonInPlay.fromCard(card);

        if ("ACTIVE".equals(position)) {
            setActiveForCurrentPlayer(state, pokemon);
        } else {
            state.getCurrentPlayerBench().add(pokemon);
        }

        return ActionResult.builder().success(true).updatedState(state).gameOver(false)
            .events(List.of(new GameEvent(GameEventType.POKEMON_PLACED,
                Map.of("pokemon", pokemon.getName(), "position", position))))
            .build();
    }

    // ── Unir Energía ──────────────────────────────────────────────────────

    public ActionResult attachEnergy(GameStateSnapshot state, String cardId, String targetPosition) {
        return attachEnergy(state, cardId, targetPosition, null);
    }

    public ActionResult attachEnergy(GameStateSnapshot state, String cardId, String targetPosition, Integer handIndex) {
        CardData energyCard = findInHand(state.getCurrentPlayerHand(), cardId, handIndex);
        if (energyCard == null) return ActionResult.fail("Carta de Energía no encontrada en la mano", state);

        PokemonInPlay target = findPokemonAtPosition(state, targetPosition);
        if (target == null) return ActionResult.fail("Pokémon objetivo no encontrado: " + targetPosition, state);

        ValidationResult v = ruleValidator.validateAttachEnergy(energyCard, target, state);
        if (!v.isValid()) return ActionResult.fail(v.getErrorMessage(), state);

        boolean removed = CardSelectionUtil.removeFromHand(state.getCurrentPlayerHand(), cardId, handIndex);
        if (!removed) return ActionResult.fail("No se pudo remover la energia seleccionada de la mano", state);
        EnergyAttached energy = EnergyAttached.builder()
            .cardId(cardId)
            .type(energyCard.getPrimaryType())
            .name(energyCard.getName())
            .build();
        target.getAttachedEnergies().add(energy);
        state.setEnergyAttachedThisTurn(true);

        return ActionResult.builder().success(true).updatedState(state).gameOver(false)
            .events(List.of(new GameEvent(GameEventType.ENERGY_ATTACHED,
                Map.of("energy", energy.getType(), "target", target.getName()))))
            .build();
    }

    // ── Evolucionar Pokémon ───────────────────────────────────────────────

    public ActionResult evolve(GameStateSnapshot state, String evolutionCardId, String targetPosition) {
        return evolve(state, evolutionCardId, targetPosition, null);
    }

    public ActionResult evolve(GameStateSnapshot state, String evolutionCardId, String targetPosition, Integer handIndex) {
        CardData evolutionCard = findInHand(state.getCurrentPlayerHand(), evolutionCardId, handIndex);
        if (evolutionCard == null) return ActionResult.fail("Carta de evolución no encontrada en la mano", state);

        PokemonInPlay target = findPokemonAtPosition(state, targetPosition);
        if (target == null) return ActionResult.fail("Pokémon objetivo no encontrado: " + targetPosition, state);

        ValidationResult v = ruleValidator.validateEvolution(target, evolutionCard, state);
        if (!v.isValid()) return ActionResult.fail(v.getErrorMessage(), state);

        boolean removed = CardSelectionUtil.removeFromHand(state.getCurrentPlayerHand(), evolutionCardId, handIndex);
        if (!removed) return ActionResult.fail("No se pudo remover la carta de evolucion seleccionada de la mano", state);
        PokemonInPlay evolved = PokemonInPlay.evolve(target, evolutionCard);
        replacePokemonAtPosition(state, targetPosition, evolved);

        return ActionResult.builder().success(true).updatedState(state).gameOver(false)
            .events(List.of(new GameEvent(GameEventType.POKEMON_EVOLVED,
                Map.of("from", target.getName(), "to", evolved.getName()))))
            .build();
    }

    // ── Atacar ────────────────────────────────────────────────────────────

    /** Usar desde TurnService (coinFlipper interno). */
    public ActionResult attack(GameStateSnapshot state, int attackIndex) {
        return attackWith(state, attackIndex, () -> random.nextBoolean());
    }

    /** Alias para retrocompatibilidad con tests que inyectan CoinFlipper. */
    public ActionResult executeAttack(GameStateSnapshot state, int attackIndex,
                                       com.pokemon.tcg.engine.model.CoinFlipper coinFlipper) {
        return attackWith(state, attackIndex, coinFlipper);
    }

    private ActionResult attackWith(GameStateSnapshot state, int attackIndex,
                                     com.pokemon.tcg.engine.model.CoinFlipper coinFlipper) {
        PokemonInPlay attacker = state.getCurrentPlayerActivePokemon();
        PokemonInPlay defender = state.getOpponentActivePokemon();

        ValidationResult v = ruleValidator.validateAttack(attacker, attackIndex, state);
        if (!v.isValid()) return ActionResult.fail(v.getErrorMessage(), state);

        List<GameEvent> events = new ArrayList<>();
        AttackData attack = attacker.getAttacks().get(attackIndex);

        // Verificar Confusión — puede cancelar el ataque
        if (statusEffectManager.checkConfusionCancelsAttack(attacker, coinFlipper)) {
            events.add(new GameEvent(GameEventType.ATTACK_EXECUTED,
                Map.of("attack", attack.getName(), "confused", true, "selfDamage", 30)));
            VictoryResult v2 = checkKnockoutsAndVictory(state, events);
            return buildResult(state, events, v2);
        }

        // Calcular y aplicar daño
        var damage = damageCalculator.calculateDamage(attacker, defender, attack, state);
        defender.setDamageCounters(
            (defender.getDamageCounters() != null ? defender.getDamageCounters() : 0)
                + damage.getDamageCounters());

        events.add(new GameEvent(GameEventType.DAMAGE_DEALT, Map.of(
            "attack", attack.getName(),
            "baseDamage", damage.getBaseDamage(),
            "finalDamage", damage.getFinalDamage(),
            "weakness", damage.getWeaknessModifier(),
            "resistance", damage.getResistanceModifier(),
            "target", defender.getName()
        )));

        // Efectos adicionales del ataque (condiciones especiales, descarte de energías)
        attackEffectParser.apply(attack, attacker, defender, state, coinFlipper, events);

        // Verificar KO y victoria
        VictoryResult victory = checkKnockoutsAndVictory(state, events);

        // Proceso entre turnos + cambio de turno (solo si no hay game over)
        if (!victory.isGameOver()) {
            runBetweenTurns(state, events);
            turnManager.resetTurnFlags(state);
            turnManager.switchTurn(state);
            events.add(new GameEvent(GameEventType.TURN_ENDED,
                Map.of("nextTurnPlayerId", state.getCurrentTurnPlayerId())));
        }

        return buildResult(state, events, victory);
    }

    // ── Retirar Pokémon ───────────────────────────────────────────────────

    public ActionResult retreat(GameStateSnapshot state, List<String> energyIdsToDiscard, int newActiveIndex) {
        PokemonInPlay active = state.getCurrentPlayerActivePokemon();
        List<EnergyAttached> toDiscard = active.getAttachedEnergies().stream()
            .filter(e -> energyIdsToDiscard.contains(e.getCardId()))
            .collect(Collectors.toList());

        ValidationResult v = ruleValidator.validateRetreat(active, toDiscard, state);
        if (!v.isValid()) return ActionResult.fail(v.getErrorMessage(), state);

        // Descartar energías
        active.getAttachedEnergies().removeAll(toDiscard);
        toDiscard.forEach(e -> state.getCurrentPlayerDiscard().add(
            CardData.builder().id(e.getCardId()).name(e.getName()).build()
        ));

        // Limpiar condición de rotación al retirarse (POISONED/BURNED se mantienen)
        active.setRotationCondition(null);
        active.setActiveEffects(new ArrayList<>());
        state.getCurrentPlayerBench().add(active);

        // Nuevo activo
        PokemonInPlay newActive = state.getCurrentPlayerBench().remove(newActiveIndex);
        setActiveForCurrentPlayer(state, newActive);
        state.setRetreatUsedThisTurn(true);

        return ActionResult.builder().success(true).updatedState(state).gameOver(false)
            .events(List.of(new GameEvent(GameEventType.POKEMON_RETREATED,
                Map.of("retreated", active.getName(), "newActive", newActive.getName()))))
            .build();
    }

    // ── Fin de turno explícito (sin atacar) ───────────────────────────────

    public ActionResult endTurn(GameStateSnapshot state) {
        List<GameEvent> events = new ArrayList<>();
        runBetweenTurns(state, events);

        VictoryResult victory = checkKnockoutsAndVictory(state, events);
        if (victory.isGameOver()) return buildResult(state, events, victory);

        turnManager.resetTurnFlags(state);
        turnManager.switchTurn(state);
        events.add(new GameEvent(GameEventType.TURN_ENDED,
            Map.of("nextTurnPlayerId", state.getCurrentTurnPlayerId())));

        return ActionResult.builder().success(true).updatedState(state).gameOver(false)
            .events(events).build();
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    private void runBetweenTurns(GameStateSnapshot state, List<GameEvent> events) {
        BetweenTurnsResult result = statusEffectManager.processBetweenTurns(
            state, () -> random.nextBoolean());
        events.addAll(result.getEvents());
    }

    private VictoryResult checkKnockoutsAndVictory(GameStateSnapshot state, List<GameEvent> events) {
        processKnockoutIfNeeded(state.getP1ActivePokemon(), true, state, events);
        processKnockoutIfNeeded(state.getP2ActivePokemon(), false, state, events);

        new ArrayList<>(state.getP1Bench()).stream()
            .filter(p -> p != null && p.isKnockedOut())
            .forEach(p -> processKnockoutIfNeeded(p, true, state, events));
        new ArrayList<>(state.getP2Bench()).stream()
            .filter(p -> p != null && p.isKnockedOut())
            .forEach(p -> processKnockoutIfNeeded(p, false, state, events));

        return victoryConditionChecker.check(state);
    }

    private void processKnockoutIfNeeded(PokemonInPlay pokemon, boolean isP1,
                                          GameStateSnapshot state, List<GameEvent> events) {
        if (pokemon == null || !pokemon.isKnockedOut()) return;
        KnockoutResult ko = turnManager.processKnockout(pokemon, isP1, state);
        events.addAll(ko.getEvents());

        // Promover automáticamente el primer Pokémon de banca si el Activo cayó
        if (isP1 && state.getP1ActivePokemon() != null && state.getP1ActivePokemon().isKnockedOut()) {
            state.setP1ActivePokemon(state.getP1Bench().isEmpty() ? null : state.getP1Bench().remove(0));
        }
        if (!isP1 && state.getP2ActivePokemon() != null && state.getP2ActivePokemon().isKnockedOut()) {
            state.setP2ActivePokemon(state.getP2Bench().isEmpty() ? null : state.getP2Bench().remove(0));
        }
    }

    private ActionResult buildResult(GameStateSnapshot state, List<GameEvent> events, VictoryResult victory) {
        if (victory.isGameOver()) {
            events.add(new GameEvent(GameEventType.GAME_OVER,
                Map.of("winnerId", String.valueOf(victory.getWinnerId()),
                       "condition", String.valueOf(victory.getCondition()))));
        }
        return ActionResult.builder()
            .success(true)
            .updatedState(state)
            .events(events)
            .gameOver(victory.isGameOver())
            .winnerId(victory.getWinnerId())
            .victoryCondition(victory.getCondition())
            .build();
    }

    private CardData findInHand(List<CardData> hand, String cardId, Integer handIndex) {
        return CardSelectionUtil.findInHand(hand, cardId, handIndex);
    }

    private PokemonInPlay findPokemonAtPosition(GameStateSnapshot state, String position) {
        if ("ACTIVE".equals(position)) return state.getCurrentPlayerActivePokemon();
        if (position.startsWith("BENCH_")) {
            int idx = Integer.parseInt(position.substring(6));
            List<PokemonInPlay> bench = state.getCurrentPlayerBench();
            return idx < bench.size() ? bench.get(idx) : null;
        }
        return null;
    }

    private void replacePokemonAtPosition(GameStateSnapshot state, String position, PokemonInPlay pokemon) {
        if ("ACTIVE".equals(position)) {
            setActiveForCurrentPlayer(state, pokemon);
        } else if (position.startsWith("BENCH_")) {
            int idx = Integer.parseInt(position.substring(6));
            state.getCurrentPlayerBench().set(idx, pokemon);
        }
    }

    private void setActiveForCurrentPlayer(GameStateSnapshot state, PokemonInPlay pokemon) {
        if (state.getCurrentTurnPlayerId().equals(state.getPlayer1Id())) {
            state.setP1ActivePokemon(pokemon);
        } else {
            state.setP2ActivePokemon(pokemon);
        }
    }

}
