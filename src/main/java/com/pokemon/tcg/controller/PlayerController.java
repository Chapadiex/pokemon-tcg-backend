package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.dto.PlayerDTO;
import com.pokemon.tcg.model.entity.Player;
import com.pokemon.tcg.repository.PlayerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerRepository playerRepository;

    public PlayerController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    /**
     * Registro/login de jugador guest.
     * Si el username ya existe, retorna el jugador existente.
     */
    @PostMapping("/guest")
    public ResponseEntity<PlayerDTO> guestLogin(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank() || username.length() < 2) {
            return ResponseEntity.badRequest().build();
        }
        Player player = playerRepository.findByUsername(username)
            .orElseGet(() -> playerRepository.save(Player.builder().username(username).build()));
        return ResponseEntity.ok(PlayerDTO.from(player));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerDTO> getPlayer(@PathVariable Long id) {
        return playerRepository.findById(id)
            .map(PlayerDTO::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
