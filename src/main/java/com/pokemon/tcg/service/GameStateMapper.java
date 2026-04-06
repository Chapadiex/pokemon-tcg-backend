package com.pokemon.tcg.service;

import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.PendingBenchDamage;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.entity.GameState;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.PokemonInPlay;
import com.pokemon.tcg.model.game.SetupPhaseState;
import com.pokemon.tcg.repository.GameStateRepository;
import com.pokemon.tcg.util.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GameStateMapper {
    private final GameStateRepository gameStateRepository;
    private final JsonUtil jsonUtil;

    public GameStateMapper(GameStateRepository gameStateRepository, JsonUtil jsonUtil) {
        this.gameStateRepository = gameStateRepository;
        this.jsonUtil = jsonUtil;
    }

    public GameStateSnapshot loadSnapshot(Game game) {
        GameState gameState = gameStateRepository.findByGameId(game.getId())
            .orElseThrow(() -> new IllegalStateException("No existe GameState para el juego " + game.getId()));

        SetupPhaseState setupState = SetupPhaseState.builder()
            .player1MulliganCount(nullSafeInt(gameState.getP1MulliganCount()))
            .player2MulliganCount(nullSafeInt(gameState.getP2MulliganCount()))
            .player1CanDrawExtra(Boolean.TRUE.equals(gameState.getP1CanDrawExtra()))
            .player2CanDrawExtra(Boolean.TRUE.equals(gameState.getP2CanDrawExtra()))
            .currentStep(gameState.getSetupStep() != null ? gameState.getSetupStep() : SetupPhaseState.SetupStep.MULLIGAN_CHECK)
            .player1PlacedActive(gameState.getP1ActivePokemon() != null)
            .player2PlacedActive(gameState.getP2ActivePokemon() != null)
            .player1ReadyToStart(Boolean.TRUE.equals(gameState.getP1ReadyToStart()))
            .player2ReadyToStart(Boolean.TRUE.equals(gameState.getP2ReadyToStart()))
            .build();

        return GameStateSnapshot.builder()
            .gameId(game.getId())
            .player1Id(game.getPlayer1().getId())
            .player2Id(game.getPlayer2() != null ? game.getPlayer2().getId() : null)
            .currentTurnPlayerId(game.getCurrentTurnPlayerId())
            .turnNumber(game.getTurnNumber())
            .isFirstTurn(game.getIsFirstTurn())
            .status(game.getStatus())
            .p1Deck(fromCardList(gameState.getP1Deck()))
            .p1Hand(fromCardList(gameState.getP1Hand()))
            .p1Prizes(fromCardList(gameState.getP1Prizes()))
            .p1Discard(fromCardList(gameState.getP1Discard()))
            .p1ActivePokemon(fromPokemon(gameState.getP1ActivePokemon()))
            .p1Bench(fromPokemonList(gameState.getP1Bench()))
            .p2Deck(fromCardList(gameState.getP2Deck()))
            .p2Hand(fromCardList(gameState.getP2Hand()))
            .p2Prizes(fromCardList(gameState.getP2Prizes()))
            .p2Discard(fromCardList(gameState.getP2Discard()))
            .p2ActivePokemon(fromPokemon(gameState.getP2ActivePokemon()))
            .p2Bench(fromPokemonList(gameState.getP2Bench()))
            .stadiumCard(fromCard(gameState.getStadiumCard()))
            .pendingBenchDamage(fromPendingBenchDamageList(gameState.getPendingBenchDamage()))
            .energyAttachedThisTurn(Boolean.TRUE.equals(gameState.getEnergyAttachedThisTurn()))
            .supporterPlayedThisTurn(Boolean.TRUE.equals(gameState.getSupporterPlayedThisTurn()))
            .stadiumPlayedThisTurn(Boolean.TRUE.equals(gameState.getStadiumPlayedThisTurn()))
            .retreatUsedThisTurn(Boolean.TRUE.equals(gameState.getRetreatUsedThisTurn()))
            .setupState(setupState)
            .build();
    }

    public GameState saveSnapshot(Game game, GameStateSnapshot snapshot) {
        GameState gameState = gameStateRepository.findByGameId(game.getId())
            .orElse(GameState.builder().game(game).build());

        game.setStatus(snapshot.getStatus());
        game.setCurrentTurnPlayerId(snapshot.getCurrentTurnPlayerId());
        game.setTurnNumber(snapshot.getTurnNumber());
        game.setIsFirstTurn(snapshot.getIsFirstTurn());

        gameState.setP1Deck(toJson(snapshot.getP1Deck()));
        gameState.setP1Hand(toJson(snapshot.getP1Hand()));
        gameState.setP1Prizes(toJson(snapshot.getP1Prizes()));
        gameState.setP1Discard(toJson(snapshot.getP1Discard()));
        gameState.setP1ActivePokemon(toJson(snapshot.getP1ActivePokemon()));
        gameState.setP1Bench(toJson(snapshot.getP1Bench()));
        gameState.setP2Deck(toJson(snapshot.getP2Deck()));
        gameState.setP2Hand(toJson(snapshot.getP2Hand()));
        gameState.setP2Prizes(toJson(snapshot.getP2Prizes()));
        gameState.setP2Discard(toJson(snapshot.getP2Discard()));
        gameState.setP2ActivePokemon(toJson(snapshot.getP2ActivePokemon()));
        gameState.setP2Bench(toJson(snapshot.getP2Bench()));
        gameState.setStadiumCard(toJson(snapshot.getStadiumCard()));
        gameState.setPendingBenchDamage(toJson(snapshot.getPendingBenchDamage()));
        gameState.setEnergyAttachedThisTurn(Boolean.TRUE.equals(snapshot.getEnergyAttachedThisTurn()));
        gameState.setSupporterPlayedThisTurn(Boolean.TRUE.equals(snapshot.getSupporterPlayedThisTurn()));
        gameState.setStadiumPlayedThisTurn(Boolean.TRUE.equals(snapshot.getStadiumPlayedThisTurn()));
        gameState.setRetreatUsedThisTurn(Boolean.TRUE.equals(snapshot.getRetreatUsedThisTurn()));

        SetupPhaseState setupState = snapshot.getSetupState() != null ? snapshot.getSetupState() : SetupPhaseState.initial();
        gameState.setP1ReadyToStart(setupState.isPlayer1ReadyToStart());
        gameState.setP2ReadyToStart(setupState.isPlayer2ReadyToStart());
        gameState.setP1MulliganCount(setupState.getPlayer1MulliganCount());
        gameState.setP2MulliganCount(setupState.getPlayer2MulliganCount());
        gameState.setP1CanDrawExtra(setupState.isPlayer1CanDrawExtra());
        gameState.setP2CanDrawExtra(setupState.isPlayer2CanDrawExtra());
        gameState.setSetupStep(setupState.getCurrentStep());

        return gameStateRepository.save(gameState);
    }

    private String toJson(Object value) {
        return value == null ? null : jsonUtil.toJson(value);
    }

    private List<CardData> fromCardList(String json) {
        return json == null || json.isBlank() ? new ArrayList<>() : jsonUtil.fromJsonList(json, CardData.class);
    }

    private List<PokemonInPlay> fromPokemonList(String json) {
        return json == null || json.isBlank() ? new ArrayList<>() : jsonUtil.fromJsonList(json, PokemonInPlay.class);
    }

    private CardData fromCard(String json) {
        return json == null || json.isBlank() ? null : jsonUtil.fromJson(json, CardData.class);
    }

    private PokemonInPlay fromPokemon(String json) {
        return json == null || json.isBlank() ? null : jsonUtil.fromJson(json, PokemonInPlay.class);
    }

    private List<PendingBenchDamage> fromPendingBenchDamageList(String json) {
        return json == null || json.isBlank()
            ? new java.util.ArrayList<>()
            : jsonUtil.fromJsonList(json, PendingBenchDamage.class);
    }

    private int nullSafeInt(Integer value) {
        return value != null ? value : 0;
    }
}
