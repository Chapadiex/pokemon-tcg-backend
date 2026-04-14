package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.PendingBenchDamage;
import com.pokemon.tcg.exception.InvalidMoveException;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.game.PokemonInPlay;
import com.pokemon.tcg.service.GameStateMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Resuelve la selección del objetivo de daño a Banca del oponente.
 *
 * Cuando un ataque genera {@code BENCH_DAMAGE_PENDING}, el jugador atacante
 * debe elegir el Pokémon de Banca que recibirá el daño. Este componente valida
 * la elección, aplica los contadores de daño y notifica a ambos jugadores.
 */
@Component
public class BenchDamageResolver {

    private final GameStateMapper gameStateMapper;
    private final SimpMessagingTemplate messaging;

    public BenchDamageResolver(GameStateMapper gameStateMapper,
                                SimpMessagingTemplate messaging) {
        this.gameStateMapper = gameStateMapper;
        this.messaging       = messaging;
    }

    /**
     * Aplica el daño pendiente a Banca sobre el Pokémon elegido por el jugador.
     *
     * @param game       entidad de la partida
     * @param playerId   id del jugador que eligió el objetivo
     * @param benchIndex índice del Pokémon en la Banca del oponente
     * @throws InvalidMoveException si no es el turno del jugador o el índice es inválido
     */
    public void resolve(Game game, Long playerId, int benchIndex) {
        GameStateSnapshot snapshot = gameStateMapper.loadSnapshot(game);

        if (!playerId.equals(snapshot.getCurrentTurnPlayerId())) {
            throw new InvalidMoveException("No es tu turno para elegir el objetivo de daño a Banca");
        }

        List<PokemonInPlay> opponentBench = snapshot.getOpponentId().equals(snapshot.getPlayer1Id())
                ? snapshot.getP1Bench()
                : snapshot.getP2Bench();

        if (benchIndex >= opponentBench.size()) {
            throw new InvalidMoveException("Índice de Banca inválido: " + benchIndex);
        }

        if (snapshot.getPendingBenchDamage().isEmpty()) {
            throw new InvalidMoveException("No hay daño a Banca pendiente de resolver");
        }

        PendingBenchDamage pending = snapshot.getPendingBenchDamage().remove(0);
        PokemonInPlay target = opponentBench.get(benchIndex);
        target.setDamageCounters(target.getDamageCounters() + pending.getDamageCounters());

        gameStateMapper.saveSnapshot(game, snapshot);

        messaging.convertAndSend("/topic/game/" + game.getId(),
                Map.of("type",   "BENCH_DAMAGE_APPLIED",
                       "target", target.getName(),
                       "damage", pending.getDamageCounters() * 10));
    }
}
