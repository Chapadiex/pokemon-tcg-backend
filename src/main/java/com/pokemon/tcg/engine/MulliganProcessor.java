package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.GameEvent;
import com.pokemon.tcg.engine.model.GameEventType;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.engine.model.MulliganResult;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.SetupPhaseState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class MulliganProcessor {
    private final RuleValidator ruleValidator;
    private final Random random;

    @Autowired
    public MulliganProcessor(RuleValidator ruleValidator) {
        this(ruleValidator, new Random());
    }

    public MulliganProcessor(RuleValidator ruleValidator, Random random) {
        this.ruleValidator = ruleValidator;
        this.random = random;
    }

    public MulliganResult processMulliganRound(GameStateSnapshot state) {
        List<CardData> hand1 = state.getP1Hand();
        List<CardData> hand2 = state.getP2Hand();
        boolean p1HasBasic = ruleValidator.handHasBasicPokemon(hand1);
        boolean p2HasBasic = ruleValidator.handHasBasicPokemon(hand2);
        SetupPhaseState setupState = state.getSetupState();

        if (p1HasBasic && p2HasBasic) {
            setupState.setPlayer1CanDrawExtra(false);
            setupState.setPlayer2CanDrawExtra(false);
            setupState.setCurrentStep(SetupPhaseState.SetupStep.INITIAL_PLACEMENT);
            return MulliganResult.bothReady();
        }

        List<GameEvent> events = new ArrayList<>();

        if (!p1HasBasic && !p2HasBasic) {
            setupState.setPlayer1MulliganCount(setupState.getPlayer1MulliganCount() + 1);
            setupState.setPlayer2MulliganCount(setupState.getPlayer2MulliganCount() + 1);
            setupState.setPlayer1CanDrawExtra(false);
            setupState.setPlayer2CanDrawExtra(false);
            setupState.setCurrentStep(SetupPhaseState.SetupStep.MULLIGAN_CHECK);
            events.add(new GameEvent(GameEventType.MULLIGAN_BOTH,
                Map.of("player1Id", state.getPlayer1Id(), "player2Id", state.getPlayer2Id())));
            returnHandToDeck(state.getP1Hand(), state.getP1Deck());
            returnHandToDeck(state.getP2Hand(), state.getP2Deck());
            drawCards(state.getP1Hand(), state.getP1Deck(), 7);
            drawCards(state.getP2Hand(), state.getP2Deck(), 7);
            return MulliganResult.continueLoop(events, false, false);
        }

        if (!p1HasBasic) {
            setupState.setPlayer1MulliganCount(setupState.getPlayer1MulliganCount() + 1);
            setupState.setPlayer1CanDrawExtra(false);
            setupState.setPlayer2CanDrawExtra(true);
            setupState.setCurrentStep(SetupPhaseState.SetupStep.WAITING_EXTRA_DRAW);
            events.add(new GameEvent(GameEventType.MULLIGAN_PLAYER,
                Map.of("playerId", state.getPlayer1Id(), "revealedHandSize", hand1.size())));
            returnHandToDeck(state.getP1Hand(), state.getP1Deck());
            drawCards(state.getP1Hand(), state.getP1Deck(), 7);
            return MulliganResult.continueLoop(events, false, true);
        }

        setupState.setPlayer2MulliganCount(setupState.getPlayer2MulliganCount() + 1);
        setupState.setPlayer1CanDrawExtra(true);
        setupState.setPlayer2CanDrawExtra(false);
        setupState.setCurrentStep(SetupPhaseState.SetupStep.WAITING_EXTRA_DRAW);
        events.add(new GameEvent(GameEventType.MULLIGAN_PLAYER,
            Map.of("playerId", state.getPlayer2Id(), "revealedHandSize", hand2.size())));
        returnHandToDeck(state.getP2Hand(), state.getP2Deck());
        drawCards(state.getP2Hand(), state.getP2Deck(), 7);
        return MulliganResult.continueLoop(events, true, false);
    }

    public void applyExtraDraw(GameStateSnapshot state, Long playerId) {
        if (playerId.equals(state.getPlayer1Id()) && state.getSetupState().isPlayer1CanDrawExtra()) {
            drawCards(state.getP1Hand(), state.getP1Deck(), 1);
        } else if (playerId.equals(state.getPlayer2Id()) && state.getSetupState().isPlayer2CanDrawExtra()) {
            drawCards(state.getP2Hand(), state.getP2Deck(), 1);
        }

        state.getSetupState().setPlayer1CanDrawExtra(false);
        state.getSetupState().setPlayer2CanDrawExtra(false);
        state.getSetupState().setCurrentStep(SetupPhaseState.SetupStep.MULLIGAN_CHECK);
    }

    public void declineExtraDraw(GameStateSnapshot state, Long playerId) {
        if (playerId.equals(state.getPlayer1Id()) && !state.getSetupState().isPlayer1CanDrawExtra()) {
            return;
        }
        if (playerId.equals(state.getPlayer2Id()) && !state.getSetupState().isPlayer2CanDrawExtra()) {
            return;
        }

        state.getSetupState().setPlayer1CanDrawExtra(false);
        state.getSetupState().setPlayer2CanDrawExtra(false);
        state.getSetupState().setCurrentStep(SetupPhaseState.SetupStep.MULLIGAN_CHECK);
    }

    private void returnHandToDeck(List<CardData> hand, List<CardData> deck) {
        deck.addAll(hand);
        hand.clear();
        Collections.shuffle(deck, random);
    }

    private void drawCards(List<CardData> hand, List<CardData> deck, int count) {
        int actual = Math.min(count, deck.size());
        for (int i = 0; i < actual; i++) {
            hand.add(deck.remove(0));
        }
    }
}