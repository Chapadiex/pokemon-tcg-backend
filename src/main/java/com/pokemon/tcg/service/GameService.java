package com.pokemon.tcg.service;

import com.pokemon.tcg.exception.GameNotFoundException;
import com.pokemon.tcg.exception.InvalidMoveException;
import com.pokemon.tcg.model.dto.GameDTO;
import com.pokemon.tcg.model.entity.Deck;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.entity.Player;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.model.enums.VictoryCondition;
import com.pokemon.tcg.repository.DeckRepository;
import com.pokemon.tcg.repository.GameRepository;
import com.pokemon.tcg.repository.PlayerRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;
    private final DeckRepository deckRepository;
    private final PlayerRepository playerRepository;
    private final SetupService setupService;
    private final SimpMessagingTemplate messaging;

    public GameService(GameRepository gameRepository, DeckRepository deckRepository,
                       PlayerRepository playerRepository, SetupService setupService,
                       SimpMessagingTemplate messaging) {
        this.gameRepository = gameRepository;
        this.deckRepository = deckRepository;
        this.playerRepository = playerRepository;
        this.setupService = setupService;
        this.messaging = messaging;
    }

    public GameDTO createGame(Long playerId, Long deckId, String gameName) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new GameNotFoundException("Jugador no encontrado: " + playerId));
        Deck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new GameNotFoundException("Mazo no encontrado: " + deckId));

        if (!Boolean.TRUE.equals(deck.getIsValid())) {
            throw new InvalidMoveException("El mazo no está validado");
        }

        Game game = Game.builder()
            .player1(player)
            .player1Deck(deck)
            .gameName(normalizeGameName(gameName))
            .status(GameStatus.WAITING)
            .build();

        return GameDTO.from(gameRepository.save(game));
    }

    private String normalizeGameName(String gameName) {
        if (gameName == null) {
            return null;
        }

        String normalized = gameName.trim();
        if (normalized.isBlank()) {
            return null;
        }

        return normalized.length() > 40 ? normalized.substring(0, 40) : normalized;
    }

    public GameDTO joinGame(Long gameId, Long playerId, Long deckId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameNotFoundException(gameId));

        if (game.getStatus() != GameStatus.WAITING) {
            throw new InvalidMoveException("La partida no está disponible (estado: " + game.getStatus() + ")");
        }
        if (game.getPlayer1().getId().equals(playerId)) {
            throw new InvalidMoveException("No puedes unirte a tu propia partida");
        }

        Player player2 = playerRepository.findById(playerId)
            .orElseThrow(() -> new GameNotFoundException("Jugador no encontrado: " + playerId));
        Deck deck2 = deckRepository.findById(deckId)
            .orElseThrow(() -> new GameNotFoundException("Mazo no encontrado: " + deckId));

        if (!Boolean.TRUE.equals(deck2.getIsValid())) {
            throw new InvalidMoveException("El mazo no está validado");
        }

        game.setPlayer2(player2);
        game.setPlayer2Deck(deck2);
        game.setStatus(GameStatus.SETUP);
        gameRepository.save(game);

        // Iniciar el setup (baraja, mulligan, colocación inicial)
        setupService.initializeSetup(game.getId());

        return GameDTO.from(game);
    }

    @Transactional(readOnly = true)
    public GameDTO getGame(Long gameId) {
        return gameRepository.findById(gameId)
            .map(GameDTO::from)
            .orElseThrow(() -> new GameNotFoundException(gameId));
    }

    @Transactional(readOnly = true)
    public List<GameDTO> getAvailableGames() {
        return gameRepository.findByStatus(GameStatus.WAITING).stream()
            .map(GameDTO::from)
            .collect(Collectors.toList());
    }

    public void abandonGame(Long gameId, Long playerId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameNotFoundException(gameId));

        boolean isPlayer1 = game.getPlayer1() != null && playerId.equals(game.getPlayer1().getId());
        boolean isPlayer2 = game.getPlayer2() != null && playerId.equals(game.getPlayer2().getId());
        if (!isPlayer1 && !isPlayer2) {
            throw new InvalidMoveException("No eres parte de esta partida");
        }
        if (game.getStatus() == GameStatus.FINISHED || game.getStatus() == GameStatus.CANCELLED) {
            throw new InvalidMoveException("La partida ya terminó");
        }

        if (game.getStatus() == GameStatus.WAITING) {
            game.setStatus(GameStatus.CANCELLED);
            gameRepository.save(game);
            return;
        }

        Long winnerId = isPlayer1
            ? (game.getPlayer2() != null ? game.getPlayer2().getId() : null)
            : game.getPlayer1().getId();

        game.setStatus(GameStatus.FINISHED);
        game.setVictoryCondition(VictoryCondition.SURRENDER);
        game.setFinishedAt(LocalDateTime.now());
        if (winnerId != null) {
            boolean p1Wins = winnerId.equals(game.getPlayer1().getId());
            game.setWinner(p1Wins ? game.getPlayer1() : game.getPlayer2());
        }
        gameRepository.save(game);

        messaging.convertAndSend("/topic/game/" + gameId,
            Map.of("type", "GAME_OVER",
                   "winnerId", winnerId != null ? winnerId : "",
                   "condition", "SURRENDER"));
    }
}
