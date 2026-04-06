package com.pokemon.tcg.repository;

import com.pokemon.tcg.model.entity.GameState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GameStateRepository extends JpaRepository<GameState, Long> {
    Optional<GameState> findByGameId(Long gameId);
}
