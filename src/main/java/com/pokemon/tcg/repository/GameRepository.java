package com.pokemon.tcg.repository;

import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.enums.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByStatus(GameStatus status);
}
