package com.pokemon.tcg.service;

import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.repository.GameRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class SuddenDeathService {

    private final GameRepository       gameRepository;
    private final SetupService         setupService;
    private final SimpMessagingTemplate messaging;

    public SuddenDeathService(GameRepository gameRepository,
                               SetupService setupService,
                               SimpMessagingTemplate messaging) {
        this.gameRepository = gameRepository;
        this.setupService   = setupService;
        this.messaging      = messaging;
    }

    /**
     * Inicia una partida de Muerte Súbita.
     * Mismos jugadores y mazos, pero solo 1 carta de Premio.
     */
    public void startSuddenDeath(Long originalGameId) {
        Game original = gameRepository.findById(originalGameId).orElseThrow();

        Game suddenDeath = Game.builder()
            .player1(original.getPlayer1())
            .player2(original.getPlayer2())
            .player1Deck(original.getPlayer1Deck())
            .player2Deck(original.getPlayer2Deck())
            .status(GameStatus.SETUP)
            .isSuddenDeath(true)
            .parentGameId(originalGameId)
            .build();

        gameRepository.save(suddenDeath);

        setupService.initializeSetupWithPrizeCount(suddenDeath.getId(), 1);

        messaging.convertAndSend("/topic/game/" + originalGameId,
            Map.of("type", "SUDDEN_DEATH_STARTED",
                   "newGameId", suddenDeath.getId()));
    }
}
