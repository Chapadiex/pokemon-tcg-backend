package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.CoinFlipper;
import com.pokemon.tcg.engine.model.GameEvent;
import com.pokemon.tcg.engine.model.GameEventType;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.PendingBenchDamage;
import com.pokemon.tcg.model.enums.StatusCondition;
import com.pokemon.tcg.model.game.AttackData;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.EnergyAttached;
import com.pokemon.tcg.model.game.PokemonInPlay;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parsea y aplica los efectos secundarios de un ataque (condiciones de estado,
 * descarte de energías, daño a Banca, curación y lanzamientos de moneda).
 */
@Component
public class AttackEffectParser {

    private final StatusEffectManager statusEffectManager;

    public AttackEffectParser(StatusEffectManager statusEffectManager) {
        this.statusEffectManager = statusEffectManager;
    }

    /**
     * Aplica todos los efectos del texto del ataque sobre el estado actual del juego.
     *
     * @param attack      datos del ataque ejecutado
     * @param attacker    Pokémon que ataca
     * @param defender    Pokémon que defiende
     * @param state       snapshot del estado del juego
     * @param coinFlipper función para lanzar la moneda (inyectable en tests)
     * @param events      lista donde se acumulan los eventos generados
     */
    public void apply(AttackData attack, PokemonInPlay attacker, PokemonInPlay defender,
                      GameStateSnapshot state, CoinFlipper coinFlipper, List<GameEvent> events) {

        String text = attack.getText();
        if (text == null || text.isBlank()) return;

        boolean requiresCoin = text.matches("(?i).*flip a coin.*");

        // Condiciones directas (sin moneda)
        if (!requiresCoin) {
            applyDirectConditions(text, defender, events);
        }

        // Descarte de energías del atacante
        if (text.contains("Discard") && text.contains("Energy")) {
            int count = extractDiscardCount(text);
            List<EnergyAttached> toRemove = attacker.getAttachedEnergies().stream()
                    .limit(count)
                    .collect(Collectors.toList());
            attacker.getAttachedEnergies().removeAll(toRemove);
        }

        // Daño a Banca del oponente (requiere selección del jugador)
        if (text.matches("(?i).*does?\\s+\\d+\\s+damage to.*bench.*")
                || text.matches("(?i).*hace?\\s+\\d+\\s+de da[ñn]o a.*banca.*")) {
            int benchDmg = extractNumber(text);
            List<PokemonInPlay> oppBench = state.getCurrentTurnPlayerId().equals(state.getPlayer1Id())
                    ? state.getP2Bench() : state.getP1Bench();
            if (!oppBench.isEmpty() && benchDmg > 0) {
                state.getPendingBenchDamage().add(PendingBenchDamage.builder()
                        .damageCounters(benchDmg / 10)
                        .targetPlayerId(state.getOpponentId())
                        .build());
                events.add(new GameEvent(GameEventType.BENCH_DAMAGE_PENDING,
                        Map.of("damage", benchDmg)));
            }
        }

        // Curación del atacante
        if (text.matches("(?i).*heal\\s+\\d+\\s+damage.*")
                || text.matches("(?i).*cura?\\s+\\d+\\s+puntos.*")) {
            int healCounters = extractNumber(text) / 10;
            int current = attacker.getDamageCounters() != null ? attacker.getDamageCounters() : 0;
            int removed = Math.min(healCounters, current);
            attacker.setDamageCounters(current - removed);
            events.add(new GameEvent(GameEventType.POKEMON_HEALED,
                    Map.of("pokemon", attacker.getName(), "healed", removed * 10)));
        }

        // Lanzar moneda para aplicar condición al defensor
        if (requiresCoin) {
            boolean heads = !coinFlipper.flip();
            events.add(new GameEvent(GameEventType.COIN_FLIP,
                    Map.of("result", heads ? "HEADS" : "TAILS")));
            if (heads) {
                applyDirectConditions(text, defender, events);
            }
        }
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    private void applyDirectConditions(String text, PokemonInPlay defender, List<GameEvent> events) {
        if (text.contains("Paralyzed") || text.contains("Paralizado")) {
            statusEffectManager.applyStatus(defender, StatusCondition.PARALYZED);
            events.add(statusEvent(defender, "PARALYZED"));
        }
        if (text.contains("Poisoned") || text.contains("Envenenado")) {
            statusEffectManager.applyStatus(defender, StatusCondition.POISONED);
            events.add(statusEvent(defender, "POISONED"));
        }
        if (text.contains("Asleep") || text.contains("Dormido")) {
            statusEffectManager.applyStatus(defender, StatusCondition.ASLEEP);
            events.add(statusEvent(defender, "ASLEEP"));
        }
        if (text.contains("Burned") || text.contains("Quemado")) {
            statusEffectManager.applyStatus(defender, StatusCondition.BURNED);
            events.add(statusEvent(defender, "BURNED"));
        }
        if (text.contains("Confused") || text.contains("Confundido")) {
            statusEffectManager.applyStatus(defender, StatusCondition.CONFUSED);
            events.add(statusEvent(defender, "CONFUSED"));
        }
    }

    private GameEvent statusEvent(PokemonInPlay pokemon, String condition) {
        return new GameEvent(GameEventType.STATUS_APPLIED,
                Map.of("pokemon", pokemon.getName(), "condition", condition));
    }

    private int extractDiscardCount(String text) {
        Matcher m = Pattern.compile("Discard (\\d+)").matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : 1;
    }

    private int extractNumber(String text) {
        Matcher m = Pattern.compile("(\\d+)").matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}
