package com.pokemon.tcg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "turns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "turn_number", nullable = false)
    private Integer turnNumber;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @OneToMany(mappedBy = "turn", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private List<Action> actions = new ArrayList<>();

    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    protected void onCreate() { startedAt = LocalDateTime.now(); }
}
