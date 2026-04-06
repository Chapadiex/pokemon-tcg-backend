package com.pokemon.tcg.model.entity;

import com.pokemon.tcg.model.game.SetupPhaseState;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_states")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", unique = true, nullable = false)
    private Game game;

    @Column(name = "p1_deck", columnDefinition = "jsonb")
    private String p1Deck;
    @Column(name = "p1_hand", columnDefinition = "jsonb")
    private String p1Hand;
    @Column(name = "p1_prizes", columnDefinition = "jsonb")
    private String p1Prizes;
    @Column(name = "p1_discard", columnDefinition = "jsonb")
    private String p1Discard;
    @Column(name = "p1_active_pokemon", columnDefinition = "jsonb")
    private String p1ActivePokemon;
    @Column(name = "p1_bench", columnDefinition = "jsonb")
    private String p1Bench;

    @Column(name = "p2_deck", columnDefinition = "jsonb")
    private String p2Deck;
    @Column(name = "p2_hand", columnDefinition = "jsonb")
    private String p2Hand;
    @Column(name = "p2_prizes", columnDefinition = "jsonb")
    private String p2Prizes;
    @Column(name = "p2_discard", columnDefinition = "jsonb")
    private String p2Discard;
    @Column(name = "p2_active_pokemon", columnDefinition = "jsonb")
    private String p2ActivePokemon;
    @Column(name = "p2_bench", columnDefinition = "jsonb")
    private String p2Bench;

    @Column(name = "stadium_card", columnDefinition = "jsonb")
    private String stadiumCard;

    @Column(name = "pending_bench_damage", columnDefinition = "jsonb")
    private String pendingBenchDamage;

    @Column(name = "energy_attached_this_turn")
    private Boolean energyAttachedThisTurn = false;
    @Column(name = "supporter_played_this_turn")
    private Boolean supporterPlayedThisTurn = false;
    @Column(name = "stadium_played_this_turn")
    private Boolean stadiumPlayedThisTurn = false;
    @Column(name = "retreat_used_this_turn")
    private Boolean retreatUsedThisTurn = false;

    @Column(name = "p1_ready_to_start")
    private Boolean p1ReadyToStart = false;
    @Column(name = "p2_ready_to_start")
    private Boolean p2ReadyToStart = false;
    @Column(name = "p1_mulligan_count")
    private Integer p1MulliganCount = 0;
    @Column(name = "p2_mulligan_count")
    private Integer p2MulliganCount = 0;
    @Column(name = "p1_can_draw_extra")
    private Boolean p1CanDrawExtra = false;
    @Column(name = "p2_can_draw_extra")
    private Boolean p2CanDrawExtra = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "setup_step")
    private SetupPhaseState.SetupStep setupStep = SetupPhaseState.SetupStep.MULLIGAN_CHECK;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
