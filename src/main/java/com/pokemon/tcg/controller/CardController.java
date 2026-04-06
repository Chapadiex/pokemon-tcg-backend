package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.dto.CardDTO;
import com.pokemon.tcg.model.dto.SetDTO;
import com.pokemon.tcg.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<CardDTO>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "1") int page) {
        List<CardDTO> results = cardService.searchCards(name, page).stream()
            .map(CardDTO::from)
            .toList();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(CardDTO.from(cardService.getCardById(id)));
    }

    @GetMapping("/sets")
    public ResponseEntity<List<SetDTO>> getSets() {
        return ResponseEntity.ok(cardService.getAllSets());
    }
}
