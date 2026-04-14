package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.dto.GameDTO;
import com.pokemon.tcg.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public ResponseEntity<List<GameDTO>> list() {
        return ResponseEntity.ok(gameService.getAvailableGames());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.getGame(id));
    }

    @PostMapping
    public ResponseEntity<GameDTO> create(@RequestParam Long playerId,
                                           @RequestBody Map<String, Object> body) {
        Long deckId = ((Number) body.get("deckId")).longValue();
        String gameName = body.get("gameName") instanceof String ? ((String) body.get("gameName")).trim() : null;
        GameDTO game = gameService.createGame(playerId, deckId, gameName);
        return ResponseEntity.created(URI.create("/api/games/" + game.getId())).body(game);
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<GameDTO> join(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body) {
        Long playerId = ((Number) body.get("playerId")).longValue();
        Long deckId   = ((Number) body.get("deckId")).longValue();
        return ResponseEntity.ok(gameService.joinGame(id, playerId, deckId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> abandon(@PathVariable Long id,
                                         @RequestParam Long playerId) {
        gameService.abandonGame(id, playerId);
        return ResponseEntity.noContent().build();
    }
}
