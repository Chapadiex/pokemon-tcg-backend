package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.ActionResult;
import com.pokemon.tcg.engine.model.CoinFlipper;
import com.pokemon.tcg.engine.model.GameEvent;
import com.pokemon.tcg.engine.model.GameEventType;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.KnockoutResult;
import com.pokemon.tcg.engine.model.VictoryResult;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.EnergyAttached;
import com.pokemon.tcg.model.game.PokemonInPlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TurnManager {
    private final RuleValidator ruleValidator;
    private final DamageCalculator damageCalculator;
    private final StatusEffectManager statusEffectManager;
    private final VictoryConditionChecker victoryChecker;

    public TurnManager(RuleValidator ruleValidator, DamageCalculator damageCalculator,
                       StatusEffectManager statusEffectManager, VictoryConditionChecker victoryChecker) {
        this.ruleValidator = ruleValidator;
        this.damageCalculator = damageCalculator;
        this.statusEffectManager = statusEffectManager;
        this.victoryChecker = victoryChecker;
    }

    public ActionResult startTurn(GameStateSnapshot state, CoinFlipper coinFlipper) {
        List<GameEvent> events = new ArrayList<>();
        boolean isFirstTurnOfGame = Boolean.TRUE.equals(state.getIsFirstTurn()) && Integer.valueOf(0).equals(state.getTurnNumber());

        if (!isFirstTurnOfGame) {
            VictoryResult deckOut = victoryChecker.checkDeckOut(state, state.getCurrentTurnPlayerId());
            if (deckOut.isGameOver()) {
                return ActionResult.builder()
                    .success(true)
                    .gameOver(true)
                    .winnerId(deckOut.getWinnerId())
                    .victoryCondition(deckOut.getCondition())
                    .events(events)
                    .updatedState(state)
                    .build();
            }

            List<CardData> deck = state.getCurrentPlayerDeck();
            List<CardData> hand = state.getCurrentPlayerHand();
            CardData drawn = deck.remove(0);
            hand.add(drawn);
            events.add(new GameEvent(GameEventType.CARD_DRAWN, Map.of("cardId", drawn.getId())));
        }

        return ActionResult.builder()
            .success(true)
            .gameOver(false)
            .events(events)
            .updatedState(state)
            .build();
    }

    public void resetTurnFlags(GameStateSnapshot state) {
        state.setEnergyAttachedThisTurn(false);
        state.setSupporterPlayedThisTurn(false);
        state.setStadiumPlayedThisTurn(false);
        state.setRetreatUsedThisTurn(false);
        state.setIsFirstTurn(false);

        incrementTurnsInPlay(state.getP1ActivePokemon());
        state.getP1Bench().forEach(this::incrementTurnsInPlay);
        incrementTurnsInPlay(state.getP2ActivePokemon());
        state.getP2Bench().forEach(this::incrementTurnsInPlay);

        resetEvolvedFlag(state.getP1ActivePokemon());
        state.getP1Bench().forEach(this::resetEvolvedFlag);
        resetEvolvedFlag(state.getP2ActivePokemon());
        state.getP2Bench().forEach(this::resetEvolvedFlag);
    }

    public void switchTurn(GameStateSnapshot state) {
        state.setCurrentTurnPlayerId(state.getOpponentId());
        state.setTurnNumber(state.getTurnNumber() + 1);
    }

    public KnockoutResult processKnockout(PokemonInPlay knockedOut, boolean player1Pokemon, GameStateSnapshot state) {
        List<GameEvent> events = new ArrayList<>();
        List<CardData> discard = player1Pokemon ? state.getP1Discard() : state.getP2Discard();
        List<CardData> opponentPrizes = player1Pokemon ? state.getP2Prizes() : state.getP1Prizes();

        discard.add(toCardData(knockedOut));
        if (knockedOut.getAttachedEnergies() != null) {
            knockedOut.getAttachedEnergies().forEach(energy -> discard.add(energyToCardData(energy)));
        }
        if (knockedOut.getAttachedTool() != null) {
            discard.add(knockedOut.getAttachedTool());
        }

        if (!opponentPrizes.isEmpty()) {
            opponentPrizes.remove(0);
            events.add(new GameEvent(GameEventType.PRIZE_TAKEN, Map.of("by", player1Pokemon ? "player2" : "player1")));
        }

        events.add(new GameEvent(GameEventType.POKEMON_KNOCKED_OUT, Map.of("pokemon", knockedOut.getName())));
        VictoryResult victoryResult = victoryChecker.check(state);
        return new KnockoutResult(events, victoryResult);
    }

    private void incrementTurnsInPlay(PokemonInPlay pokemon) {
        if (pokemon != null) {
            pokemon.setTurnsSinceInPlay((pokemon.getTurnsSinceInPlay() != null ? pokemon.getTurnsSinceInPlay() : 0) + 1);
        }
    }

    private void resetEvolvedFlag(PokemonInPlay pokemon) {
        if (pokemon != null) {
            pokemon.setEvolvedThisTurn(false);
        }
    }

    private CardData toCardData(PokemonInPlay pokemon) {
        return CardData.builder().id(pokemon.getCardId()).name(pokemon.getName()).build();
    }

    private CardData energyToCardData(EnergyAttached energy) {
        return CardData.builder().id(energy.getCardId()).name(energy.getName()).supertype("Energy").build();
    }
}
