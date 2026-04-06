package com.pokemon.tcg.service;

import com.pokemon.tcg.engine.RuleValidator;
import com.pokemon.tcg.engine.model.ValidationResult;
import com.pokemon.tcg.exception.GameNotFoundException;
import com.pokemon.tcg.model.dto.DeckDTO;
import com.pokemon.tcg.model.entity.CardInDeck;
import com.pokemon.tcg.model.entity.Deck;
import com.pokemon.tcg.model.entity.Player;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.repository.DeckRepository;
import com.pokemon.tcg.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class DeckService {

    private final DeckRepository deckRepository;
    private final PlayerRepository playerRepository;
    private final CardService cardService;
    private final RuleValidator ruleValidator;

    public DeckService(DeckRepository deckRepository, PlayerRepository playerRepository,
                       CardService cardService, RuleValidator ruleValidator) {
        this.deckRepository = deckRepository;
        this.playerRepository = playerRepository;
        this.cardService = cardService;
        this.ruleValidator = ruleValidator;
    }

    public DeckDTO createDeck(Long playerId, String name, List<String> cardIds) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new GameNotFoundException("Jugador no encontrado: " + playerId));

        Deck deck = Deck.builder()
            .player(player)
            .name(name)
            .isValid(false)
            .cards(new ArrayList<>())
            .build();

        Map<String, Long> counts = cardIds.stream()
            .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        counts.forEach((cardId, qty) -> {
            CardInDeck cid = CardInDeck.builder()
                .deck(deck)
                .cardId(cardId)
                .quantity(qty.intValue())
                .build();
            deck.getCards().add(cid);
        });

        return DeckDTO.from(deckRepository.save(deck));
    }

    public DeckDTO updateDeck(Long deckId, String name, List<String> cardIds) {
        Deck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new GameNotFoundException("Mazo no encontrado: " + deckId));

        if (name != null && !name.isBlank()) {
            deck.setName(name);
        }

        if (cardIds != null) {
            deck.getCards().clear();
            Map<String, Long> counts = cardIds.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
            counts.forEach((cardId, qty) -> deck.getCards().add(
                CardInDeck.builder().deck(deck).cardId(cardId).quantity(qty.intValue()).build()
            ));
            deck.setIsValid(false);
        }

        return DeckDTO.from(deckRepository.save(deck));
    }

    public void deleteDeck(Long deckId) {
        Deck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new GameNotFoundException("Mazo no encontrado: " + deckId));
        deckRepository.delete(deck);
    }

    public ValidationResult validateDeck(Long deckId) {
        Deck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new GameNotFoundException("Mazo no encontrado: " + deckId));

        List<String> cardIds = deck.getCards().stream()
            .flatMap(cid -> Collections.nCopies(cid.getQuantity(), cid.getCardId()).stream())
            .collect(Collectors.toList());

        List<CardData> cards = cardIds.stream()
            .map(cardService::getCardById)
            .collect(Collectors.toList());

        ValidationResult result = ruleValidator.validateDeckComposition(cards);
        deck.setIsValid(result.isValid());
        deckRepository.save(deck);
        return result;
    }

    @Transactional(readOnly = true)
    public List<DeckDTO> getDecksForPlayer(Long playerId) {
        return deckRepository.findByPlayerId(playerId).stream()
            .map(DeckDTO::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeckDTO getDeck(Long deckId) {
        return deckRepository.findById(deckId)
            .map(DeckDTO::from)
            .orElseThrow(() -> new GameNotFoundException("Mazo no encontrado: " + deckId));
    }
}
