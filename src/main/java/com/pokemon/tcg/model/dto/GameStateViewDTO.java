package com.pokemon.tcg.model.dto;

import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.PokemonInPlay;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class GameStateViewDTO {

    private Long    gameId;
    private String  status;
    private Long    currentTurnPlayerId;
    private Integer turnNumber;
    private Boolean isFirstTurn;

    private PlayerViewDTO   player;
    private OpponentViewDTO opponent;
    private CardDTO         stadiumCard;
    private TurnActionsDTO  turnActions;

    @Data
    @Builder
    public static class PlayerViewDTO {
        private Long              id;
        private int               deckCount;
        private List<CardDTO>     hand;
        private int               prizesCount;
        private List<CardDTO>     discard;
        private PokemonInPlay     activePokemon;
        private List<PokemonInPlay> bench;
    }

    @Data
    @Builder
    public static class OpponentViewDTO {
        private Long              id;
        private int               deckCount;
        private int               handCount;
        private int               prizesCount;
        private List<CardDTO>     discard;
        private PokemonInPlay     activePokemon;
        private List<PokemonInPlay> bench;
    }

    @Data
    @Builder
    public static class TurnActionsDTO {
        private boolean energyAttached;
        private boolean supporterPlayed;
        private boolean stadiumPlayed;
        private boolean retreatUsed;
    }

    public static GameStateViewDTO projectFor(GameStateSnapshot snapshot, Long viewingPlayerId) {
        boolean isP1 = viewingPlayerId.equals(snapshot.getPlayer1Id());

        PlayerViewDTO myState = PlayerViewDTO.builder()
            .id(viewingPlayerId)
            .deckCount(isP1 ? snapshot.getP1Deck().size()    : snapshot.getP2Deck().size())
            .hand(toDTOs(isP1 ? snapshot.getP1Hand()          : snapshot.getP2Hand()))
            .prizesCount(isP1 ? snapshot.getP1Prizes().size() : snapshot.getP2Prizes().size())
            .discard(toDTOs(isP1 ? snapshot.getP1Discard()    : snapshot.getP2Discard()))
            .activePokemon(isP1 ? snapshot.getP1ActivePokemon() : snapshot.getP2ActivePokemon())
            .bench(isP1 ? snapshot.getP1Bench()               : snapshot.getP2Bench())
            .build();

        OpponentViewDTO opponentState = OpponentViewDTO.builder()
            .id(isP1 ? snapshot.getPlayer2Id()           : snapshot.getPlayer1Id())
            .deckCount(isP1 ? snapshot.getP2Deck().size()    : snapshot.getP1Deck().size())
            .handCount(isP1 ? snapshot.getP2Hand().size()    : snapshot.getP1Hand().size())
            .prizesCount(isP1 ? snapshot.getP2Prizes().size(): snapshot.getP1Prizes().size())
            .discard(toDTOs(isP1 ? snapshot.getP2Discard()   : snapshot.getP1Discard()))
            .activePokemon(isP1 ? snapshot.getP2ActivePokemon() : snapshot.getP1ActivePokemon())
            .bench(isP1 ? snapshot.getP2Bench()              : snapshot.getP1Bench())
            .build();

        return GameStateViewDTO.builder()
            .gameId(snapshot.getGameId())
            .status(snapshot.getStatus().name())
            .currentTurnPlayerId(snapshot.getCurrentTurnPlayerId())
            .turnNumber(snapshot.getTurnNumber())
            .isFirstTurn(snapshot.getIsFirstTurn())
            .player(myState)
            .opponent(opponentState)
            .stadiumCard(toDTO(snapshot.getStadiumCard()))
            .turnActions(TurnActionsDTO.builder()
                .energyAttached(Boolean.TRUE.equals(snapshot.getEnergyAttachedThisTurn()))
                .supporterPlayed(Boolean.TRUE.equals(snapshot.getSupporterPlayedThisTurn()))
                .stadiumPlayed(Boolean.TRUE.equals(snapshot.getStadiumPlayedThisTurn()))
                .retreatUsed(Boolean.TRUE.equals(snapshot.getRetreatUsedThisTurn()))
                .build())
            .build();
    }

    private static List<CardDTO> toDTOs(List<CardData> cards) {
        if (cards == null) return new ArrayList<>();
        return cards.stream().map(CardDTO::from).collect(Collectors.toList());
    }

    private static CardDTO toDTO(CardData card) {
        return card != null ? CardDTO.from(card) : null;
    }
}
