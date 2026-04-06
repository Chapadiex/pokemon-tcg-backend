package com.pokemon.tcg.repository;

import com.pokemon.tcg.model.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionRepository extends JpaRepository<Action, Long> {}
