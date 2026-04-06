package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.ActionResult;
import com.pokemon.tcg.engine.model.GameEvent;
import com.pokemon.tcg.engine.model.GameEventType;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.ValidationResult;
import com.pokemon.tcg.exception.InvalidMoveException;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.PokemonInPlay;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class TrainerCardEffectProcessor {

    private final RuleValidator ruleValidator;

    public TrainerCardEffectProcessor(RuleValidator ruleValidator) {
        this.ruleValidator = ruleValidator;
    }

    public ActionResult process(CardData card, GameStateSnapshot state, Map<String, Object> data) {
        ValidationResult v = ruleValidator.validatePlayTrainer(card, state);
        if (!v.isValid()) return ActionResult.fail(v.getErrorMessage(), state);

        // Quitar de la mano antes de aplicar el efecto
        state.getCurrentPlayerHand().removeIf(c -> c.getId().equals(card.getId()));

        ActionResult result = switch (card.getName()) {
            case "Potion", "Poción"                                         -> effectPotion(card, state, data);
            case "Switch", "Cambio"                                         -> effectSwitch(card, state, data);
            case "Pokémon Communication", "Comunicación Pokémon"            -> effectPokemonCommunication(card, state, data);
            case "Professor Sycamore", "Profesor Sycamore"                  -> effectProfessorSycamore(card, state);
            case "N"                                                         -> effectN(card, state);
            default -> card.isStadium()
                ? effectStadium(card, state)
                : card.isPokemonTool()
                    ? effectPokemonTool(card, state, data)
                    : ActionResult.fail("Efecto de carta no implementado: " + card.getName(), state);
        };

        // Marcar flags de turno
        if (card.isSupporter()) state.setSupporterPlayedThisTurn(true);
        if (card.isStadium())   state.setStadiumPlayedThisTurn(true);

        return result;
    }

    // ── Poción: cura 30 daños (3 counters) al Pokémon objetivo ───────────

    private ActionResult effectPotion(CardData card, GameStateSnapshot state, Map<String, Object> data) {
        String targetPosition = (String) data.getOrDefault("targetPosition", "ACTIVE");
        PokemonInPlay target = findTarget(state, targetPosition);
        if (target == null) return ActionResult.fail("Pokémon objetivo no encontrado", state);

        int healed = Math.min(3, target.getDamageCounters() != null ? target.getDamageCounters() : 0);
        target.setDamageCounters((target.getDamageCounters() != null ? target.getDamageCounters() : 0) - healed);
        discardCard(card, state);

        return ActionResult.builder().success(true).updatedState(state).gameOver(false)
            .events(List.of(new GameEvent(GameEventType.TRAINER_PLAYED,
                Map.of("card", card.getName(), "healed", healed * 10, "target", target.getName()))))
            .build();
    }

    // ── Cambio: intercambia Activo con Banca sin costo de retirada ────────

    private ActionResult effectSwitch(CardData card, GameStateSnapshot state, Map<String, Object> data) {
        List<PokemonInPlay> bench = state.getCurrentPlayerBench();
        if (bench.isEmpty()) return ActionResult.fail("No hay Pokémon en la Banca para intercambiar", state);

        int benchIndex = data.containsKey("benchIndex")
            ? ((Number) data.get("benchIndex")).intValue() : 0;
        if (benchIndex >= bench.size()) return ActionResult.fail("Índice de banca inválido", state);

        PokemonInPlay oldActive = state.getCurrentPlayerActivePokemon();
        PokemonInPlay newActive = bench.remove(benchIndex);
        bench.add(oldActive);
        setActiveForCurrentPlayer(state, newActive);
        discardCard(card, state);

        return ActionResult.builder().success(true).updatedState(state).gameOver(false)
            .events(List.of(new GameEvent(GameEventType.POKEMON_RETREATED,
                Map.of("via", "Switch", "newActive", newActive.getName()))))
            .build();
    }

    // ── Comunicación Pokémon: buscar 1 Pokémon del mazo ──────────────────

    private ActionResult effectPokemonCommunication(CardData card, GameStateSnapshot state, Map<String, Object> data) {
        String targetCardId = (String) data.get("targetCardId");
        List<CardData> deck = state.getCurrentPlayerDeck();

        CardData found = deck.stream()
            .filter(c -> c.getId().equals(targetCardId) && c.isPokemon())
            .findFirst()
            .orElseThrow(() -> new InvalidMoveException("Pokémon " + targetCardId + " no encontrado en el mazo"));

        deck.removeIf(c -> c.getId().equals(targetCardId));
        state.getCurrentPlayerHand().add(found);
        Collections.shuffle(deck);
        discardCard(card, state);

        return ActionResult.builder().success(true).updatedState(state).gameOver(false)
            .events(List.of(new GameEvent(GameEventType.CARD_DRAWN,
                Map.of("card", card.getName(), "drawn", found.getName()))))
            .build();
    }

    // ── Profesor Sycamore: descartar mano y robar 7 cartas ───────────────

    private ActionResult effectProfessorSycamore(CardData card, GameStateSnapshot state) {
        List<CardData> hand    = state.getCurrentPlayerHand();
        List<CardData> discard = state.getCurrentPlayerDiscard();
        List<CardData> deck    = state.getCurrentPlayerDeck();

        discard.addAll(hand);
        hand.clear();

        int draw = Math.min(7, deck.size());
        for (int i = 0; i < draw; i++) hand.add(deck.remove(0));

        discardCard(card, state);

        return ActionResult.builder().success(true).updatedState(state).gameOver(false)
            .events(List.of(new GameEvent(GameEventType.CARD_DRAWN,
                Map.of("card", "Profesor Sycamore", "drew", draw))))
            .build();
    }

    // ── N: cada jugador baraja mano en mazo y roba según premios restantes ─

    private ActionResult effectN(CardData card, GameStateSnapshot state) {
        state.getP1Deck().addAll(state.getP1Hand());
        state.getP1Hand().clear();
        Collections.shuffle(state.getP1Deck());
        int draw1 = Math.min(state.getP1Prizes().size(), state.getP1Deck().size());
        for (int i = 0; i < draw1; i++) state.getP1Hand().add(state.getP1Deck().remove(0));

        state.getP2Deck().addAll(state.getP2Hand());
        state.getP2Hand().clear();
        Collections.shuffle(state.getP2Deck());
        int draw2 = Math.min(state.getP2Prizes().size(), state.getP2Deck().size());
        for (int i = 0; i < draw2; i++) state.getP2Hand().add(state.getP2Deck().remove(0));

        discardCard(card, state);

        return ActionResult.builder().success(true).updatedState(state).gameOver(false)
            .events(List.of(new GameEvent(GameEventType.CARD_DRAWN,
                Map.of("card", "N", "p1Drew", draw1, "p2Drew", draw2))))
            .build();
    }

    // ── Estadio: reemplaza el estadio activo ──────────────────────────────

    private ActionResult effectStadium(CardData card, GameStateSnapshot state) {
        if (state.getStadiumCard() != null) {
            state.getCurrentPlayerDiscard().add(state.getStadiumCard());
        }
        state.setStadiumCard(card);

        return ActionResult.builder().success(true).updatedState(state).gameOver(false)
            .events(List.of(new GameEvent(GameEventType.TRAINER_PLAYED,
                Map.of("card", card.getName(), "type", "Stadium"))))
            .build();
    }

    // ── Herramienta Pokémon: adjuntar al Pokémon objetivo ────────────────

    private ActionResult effectPokemonTool(CardData card, GameStateSnapshot state, Map<String, Object> data) {
        String targetPosition = (String) data.getOrDefault("targetPosition", "ACTIVE");
        PokemonInPlay target = findTarget(state, targetPosition);
        if (target == null) return ActionResult.fail("Pokémon objetivo no encontrado", state);
        if (target.getAttachedTool() != null) {
            return ActionResult.fail(target.getName() + " ya tiene una Herramienta unida", state);
        }
        target.setAttachedTool(card);

        return ActionResult.builder().success(true).updatedState(state).gameOver(false)
            .events(List.of(new GameEvent(GameEventType.TRAINER_PLAYED,
                Map.of("card", card.getName(), "type", "Tool", "target", target.getName()))))
            .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private PokemonInPlay findTarget(GameStateSnapshot state, String position) {
        if ("ACTIVE".equals(position)) return state.getCurrentPlayerActivePokemon();
        if (position.startsWith("BENCH_")) {
            int idx = Integer.parseInt(position.substring(6));
            List<PokemonInPlay> bench = state.getCurrentPlayerBench();
            return idx < bench.size() ? bench.get(idx) : null;
        }
        return null;
    }

    private void discardCard(CardData card, GameStateSnapshot state) {
        state.getCurrentPlayerDiscard().add(card);
    }

    private void setActiveForCurrentPlayer(GameStateSnapshot state, PokemonInPlay pokemon) {
        if (state.getCurrentTurnPlayerId().equals(state.getPlayer1Id())) {
            state.setP1ActivePokemon(pokemon);
        } else {
            state.setP2ActivePokemon(pokemon);
        }
    }
}
