package com.pokemon.tcg.model.entity;

import com.pokemon.tcg.model.enums.ActionType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Action {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turn_id", nullable = false)
    private Turn turn;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Column(name = "action_data", columnDefinition = "jsonb")
    private String actionData;

    @Column(name = "is_valid")
    private Boolean isValid = true;

    @Column(name = "validation_error")
    private String validationError;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
