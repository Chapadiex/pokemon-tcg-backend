package com.pokemon.tcg.repository;

import com.pokemon.tcg.model.entity.Turn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TurnRepository extends JpaRepository<Turn, Long> {
    List<Turn> findByGameIdOrderByTurnNumberAsc(Long gameId);
}
