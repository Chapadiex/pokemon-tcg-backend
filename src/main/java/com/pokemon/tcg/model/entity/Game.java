package com.pokemon.tcg.model.entity;

import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.model.enums.VictoryCondition;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id", nullable = false)
    private Player player1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id")
    private Player player2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_deck_id", nullable = false)
    private Deck player1Deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_deck_id")
    private Deck player2Deck;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status = GameStatus.WAITING;

    @Column(name = "current_turn_player_id")
    private Long currentTurnPlayerId;

    @Column(name = "turn_number")
    private Integer turnNumber = 0;

    @Column(name = "is_first_turn")
    private Boolean isFirstTurn = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Player winner;

    @Enumerated(EnumType.STRING)
    @Column(name = "victory_condition")
    private VictoryCondition victoryCondition;

    @OneToOne(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private GameState gameState;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    @OrderBy("turnNumber ASC")
    private List<Turn> turns = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_sudden_death")
    private Boolean isSuddenDeath = false;

    @Column(name = "parent_game_id")
    private Long parentGameId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
