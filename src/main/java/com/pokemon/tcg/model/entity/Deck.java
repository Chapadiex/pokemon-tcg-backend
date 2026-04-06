package com.pokemon.tcg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_valid")
    private Boolean isValid = false;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CardInDeck> cards = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
