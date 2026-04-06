package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.KOReplacementResult;
import com.pokemon.tcg.exception.InvalidMoveException;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.enums.VictoryCondition;
import com.pokemon.tcg.model.game.PokemonInPlay;
import com.pokemon.tcg.repository.GameRepository;
import com.pokemon.tcg.service.GameStateMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@Transactional
public class KnockoutReplacementHandler {

    private final GameRepository       gameRepository;
    private final GameStateMapper      mapper;
    private final SimpMessagingTemplate messaging;

    public KnockoutReplacementHandler(GameRepository gameRepository,
                                       GameStateMapper mapper,
                                       SimpMessagingTemplate messaging) {
        this.gameRepository = gameRepository;
        this.mapper          = mapper;
        this.messaging       = messaging;
    }

    /**
     * Llamado después de un KO del Activo: decide si el dueño necesita elegir reemplazo.
     * Si la banca tiene solo un Pokémon, se elige automáticamente.
     * Si está vacía, el jugador pierde.
     */
    public KOReplacementResult handleKnockoutReplacement(
            GameStateSnapshot snapshot, boolean isP1Pokemon, Long gameId) {

        List<PokemonInPlay> bench = isP1Pokemon ? snapshot.getP1Bench() : snapshot.getP2Bench();
        Long ownerId = isP1Pokemon ? snapshot.getPlayer1Id() : snapshot.getPlayer2Id();
        Long winnerId = isP1Pokemon ? snapshot.getPlayer2Id() : snapshot.getPlayer1Id();

        if (bench.isEmpty()) {
            return KOReplacementResult.gameOver(winnerId, VictoryCondition.NO_POKEMON);
        }

        if (bench.size() == 1) {
            PokemonInPlay autoChosen = bench.remove(0);
            setActive(snapshot, autoChosen, isP1Pokemon);
            return KOReplacementResult.autoResolved(autoChosen);
        }

        messaging.convertAndSendToUser(
            String.valueOf(ownerId),
            "/queue/game/" + gameId,
            Map.of("type", "CHOOSE_REPLACEMENT",
                   "bench", bench,
                   "gameId", gameId));

        return KOReplacementResult.waitingForPlayer(ownerId);
    }

    /**
     * El jugador eligió su reemplazo en respuesta al CHOOSE_REPLACEMENT.
     */
    public void applyReplacement(Long gameId, Long playerId, int benchIndex) {
        Game game = gameRepository.findById(gameId).orElseThrow();
        GameStateSnapshot snapshot = mapper.loadSnapshot(game);

        boolean isP1 = playerId.equals(snapshot.getPlayer1Id());
        List<PokemonInPlay> bench = isP1 ? snapshot.getP1Bench() : snapshot.getP2Bench();

        if (benchIndex >= bench.size()) {
            throw new InvalidMoveException("Índice de Banca inválido: " + benchIndex);
        }

        PokemonInPlay chosen = bench.remove(benchIndex);
        setActive(snapshot, chosen, isP1);
        mapper.saveSnapshot(game, snapshot);

        messaging.convertAndSend("/topic/game/" + gameId,
            Map.of("type", "REPLACEMENT_CHOSEN",
                   "playerId", playerId,
                   "pokemon", chosen.getName()));
    }

    private void setActive(GameStateSnapshot state, PokemonInPlay pokemon, boolean isP1) {
        if (isP1) state.setP1ActivePokemon(pokemon);
        else      state.setP2ActivePokemon(pokemon);
    }
}
