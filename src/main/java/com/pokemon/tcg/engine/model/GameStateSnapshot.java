package com.pokemon.tcg.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.PokemonInPlay;
import com.pokemon.tcg.model.game.SetupPhaseState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStateSnapshot {
    private Long gameId;
    private Long player1Id;
    private Long player2Id;
    private Long currentTurnPlayerId;
    private Integer turnNumber;
    private Boolean isFirstTurn;
    private GameStatus status;
    private List<CardData> p1Deck;
    private List<CardData> p1Hand;
    private List<CardData> p1Prizes;
    private List<CardData> p1Discard;
    private PokemonInPlay p1ActivePokemon;
    private List<PokemonInPlay> p1Bench;
    private List<CardData> p2Deck;
    private List<CardData> p2Hand;
    private List<CardData> p2Prizes;
    private List<CardData> p2Discard;
    private PokemonInPlay p2ActivePokemon;
    private List<PokemonInPlay> p2Bench;
    private CardData stadiumCard;
    @Builder.Default
    private List<PendingBenchDamage> pendingBenchDamage = new ArrayList<>();
    private Boolean energyAttachedThisTurn;
    private Boolean supporterPlayedThisTurn;
    private Boolean stadiumPlayedThisTurn;
    private Boolean retreatUsedThisTurn;
    private SetupPhaseState setupState;

    @JsonIgnore
    public Long getOpponentId() {
        if (currentTurnPlayerId == null) return null;
        return currentTurnPlayerId.equals(player1Id) ? player2Id : player1Id;
    }

    @JsonIgnore
    public PokemonInPlay getCurrentPlayerActivePokemon() {
        if (currentTurnPlayerId == null) return null;
        return currentTurnPlayerId.equals(player1Id) ? p1ActivePokemon : p2ActivePokemon;
    }

    @JsonIgnore
    public PokemonInPlay getOpponentActivePokemon() {
        if (currentTurnPlayerId == null) return null;
        return currentTurnPlayerId.equals(player1Id) ? p2ActivePokemon : p1ActivePokemon;
    }

    @JsonIgnore
    public List<PokemonInPlay> getCurrentPlayerBench() {
        if (currentTurnPlayerId == null) return null;
        return currentTurnPlayerId.equals(player1Id) ? p1Bench : p2Bench;
    }

    @JsonIgnore
    public List<CardData> getCurrentPlayerHand() {
        if (currentTurnPlayerId == null) return null;
        return currentTurnPlayerId.equals(player1Id) ? p1Hand : p2Hand;
    }

    @JsonIgnore
    public List<CardData> getCurrentPlayerPrizes() {
        if (currentTurnPlayerId == null) return null;
        return currentTurnPlayerId.equals(player1Id) ? p1Prizes : p2Prizes;
    }

    @JsonIgnore
    public List<CardData> getCurrentPlayerDeck() {
        if (currentTurnPlayerId == null) return null;
        return currentTurnPlayerId.equals(player1Id) ? p1Deck : p2Deck;
    }

    @JsonIgnore
    public List<CardData> getCurrentPlayerDiscard() {
        if (currentTurnPlayerId == null) return null;
        return currentTurnPlayerId.equals(player1Id) ? p1Discard : p2Discard;
    }
}
