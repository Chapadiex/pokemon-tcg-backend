package com.pokemon.tcg.model.dto;

import com.pokemon.tcg.model.entity.CardInDeck;
import com.pokemon.tcg.model.entity.Deck;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Data
@Builder
public class DeckDTO {
    private Long id;
    private String name;
    private Boolean isValid;
    private int cardCount;
    private List<String> cardIds;
    private String createdAt;

    public static DeckDTO from(Deck deck) {
        List<CardInDeck> cards = deck.getCards() != null ? deck.getCards() : Collections.emptyList();
        List<String> cardIds = cards.stream()
            .filter(card -> card.getCardId() != null && card.getQuantity() != null && card.getQuantity() > 0)
            .flatMap(card -> Collections.nCopies(card.getQuantity(), card.getCardId()).stream())
            .filter(Objects::nonNull)
            .toList();

        int cardCount = cards.stream()
            .map(CardInDeck::getQuantity)
            .filter(quantity -> quantity != null)
            .mapToInt(Integer::intValue)
            .sum();

        return DeckDTO.builder()
            .id(deck.getId())
            .name(deck.getName())
            .isValid(deck.getIsValid())
            .cardCount(cardCount)
            .cardIds(cardIds)
            .createdAt(deck.getCreatedAt() != null ? deck.getCreatedAt().toString() : null)
            .build();
    }
}
