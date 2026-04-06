package com.pokemon.tcg.controller;

import com.pokemon.tcg.engine.model.ValidationResult;
import com.pokemon.tcg.model.dto.DeckDTO;
import com.pokemon.tcg.service.DeckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping
    public ResponseEntity<List<DeckDTO>> list(@RequestParam Long playerId) {
        return ResponseEntity.ok(deckService.getDecksForPlayer(playerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeckDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(deckService.getDeck(id));
    }

    @PostMapping
    public ResponseEntity<DeckDTO> create(@RequestParam Long playerId,
                                           @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        @SuppressWarnings("unchecked")
        List<String> cardIds = (List<String>) body.get("cardIds");
        DeckDTO deck = deckService.createDeck(playerId, name, cardIds);
        return ResponseEntity.created(URI.create("/api/decks/" + deck.getId())).body(deck);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeckDTO> update(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        @SuppressWarnings("unchecked")
        List<String> cardIds = (List<String>) body.get("cardIds");
        return ResponseEntity.ok(deckService.updateDeck(id, name, cardIds));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deckService.deleteDeck(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<ValidationResult> validate(@PathVariable Long id) {
        return ResponseEntity.ok(deckService.validateDeck(id));
    }
}
