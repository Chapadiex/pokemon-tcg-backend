package com.pokemon.tcg.repository;

import com.pokemon.tcg.model.entity.CardInDeck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardInDeckRepository extends JpaRepository<CardInDeck, Long> {}
