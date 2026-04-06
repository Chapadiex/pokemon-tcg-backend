package com.pokemon.tcg.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cards_in_deck")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardInDeck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @Column(name = "card_id", nullable = false, length = 50)
    private String cardId;

    @Column(nullable = false)
    private Integer quantity;
}
