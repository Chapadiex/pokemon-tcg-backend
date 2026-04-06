package com.pokemon.tcg.model.dto;

import com.pokemon.tcg.model.entity.CardInDeck;
import com.pokemon.tcg.model.entity.Deck;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeckDTOTest {

    @Test
    void from_expandsCardIdsUsingQuantity() {
        Deck deck = Deck.builder()
            .id(10L)
            .name("Test deck")
            .isValid(false)
            .cards(List.of(
                CardInDeck.builder().cardId("card-a").quantity(4).build(),
                CardInDeck.builder().cardId("card-b").quantity(2).build()
            ))
            .build();

        DeckDTO dto = DeckDTO.from(deck);

        assertEquals(6, dto.getCardCount());
        assertEquals(6, dto.getCardIds().size());
        assertEquals(4, dto.getCardIds().stream().filter(id -> id.equals("card-a")).count());
        assertEquals(2, dto.getCardIds().stream().filter(id -> id.equals("card-b")).count());
    }
}
