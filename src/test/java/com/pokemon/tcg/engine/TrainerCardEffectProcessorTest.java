package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.ActionResult;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.PokemonInPlay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerCardEffectProcessorTest {

    private final TrainerCardEffectProcessor processor =
        new TrainerCardEffectProcessor(new RuleValidator());

    private GameStateSnapshot state;

    @BeforeEach
    void setUp() {
        PokemonInPlay active = PokemonInPlay.builder()
            .cardId("p1").name("Charmander").hp(60).damageCounters(3)
            .attachedEnergies(new ArrayList<>()).attacks(new ArrayList<>())
            .turnsSinceInPlay(1).evolvedThisTurn(false).activeEffects(new ArrayList<>())
            .build();
        PokemonInPlay bench = PokemonInPlay.builder()
            .cardId("p1b").name("Squirtle").hp(50).damageCounters(0)
            .attachedEnergies(new ArrayList<>()).attacks(new ArrayList<>())
            .turnsSinceInPlay(1).evolvedThisTurn(false).activeEffects(new ArrayList<>())
            .build();

        state = GameStateSnapshot.builder()
            .player1Id(1L).player2Id(2L).currentTurnPlayerId(1L)
            .turnNumber(1).isFirstTurn(false).status(GameStatus.ACTIVE)
            .p1Deck(new ArrayList<>())
            .p1Hand(new ArrayList<>())
            .p1Prizes(new ArrayList<>(List.of(card("prize-1"), card("prize-2"))))
            .p1Discard(new ArrayList<>())
            .p1ActivePokemon(active)
            .p1Bench(new ArrayList<>(List.of(bench)))
            .p2Deck(new ArrayList<>())
            .p2Hand(new ArrayList<>())
            .p2Prizes(new ArrayList<>())
            .p2Discard(new ArrayList<>())
            .p2ActivePokemon(PokemonInPlay.builder().cardId("p2").name("Bulbasaur").hp(60)
                .damageCounters(0).attachedEnergies(new ArrayList<>()).attacks(new ArrayList<>())
                .turnsSinceInPlay(1).evolvedThisTurn(false).activeEffects(new ArrayList<>()).build())
            .p2Bench(new ArrayList<>())
            .energyAttachedThisTurn(false).supporterPlayedThisTurn(false)
            .stadiumPlayedThisTurn(false).retreatUsedThisTurn(false)
            .build();
    }

    // ── Poción ────────────────────────────────────────────────────────────

    @Test
    void pocion_cura_30_dano_del_activo() {
        CardData potion = trainer("potion-1", "Potion", "Item");
        state.getP1Hand().add(potion);

        ActionResult result = processor.process(potion, state, Map.of("targetPosition", "ACTIVE"));

        assertThat(result.isSuccess()).isTrue();
        // damageCounters estaba en 3 → cura 3 counters (30 HP)
        assertThat(state.getP1ActivePokemon().getDamageCounters()).isEqualTo(0);
        assertThat(state.getP1Discard()).contains(potion);
        assertThat(state.getP1Hand()).doesNotContain(potion);
    }

    @Test
    void pocion_no_cura_mas_de_lo_que_tiene() {
        state.getP1ActivePokemon().setDamageCounters(1); // solo 1 counter
        CardData potion = trainer("potion-1", "Potion", "Item");
        state.getP1Hand().add(potion);

        ActionResult result = processor.process(potion, state, Map.of("targetPosition", "ACTIVE"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(state.getP1ActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    // ── Cambio ────────────────────────────────────────────────────────────

    @Test
    void cambio_intercambia_activo_con_banca_sin_costo() {
        CardData switchCard = trainer("switch-1", "Switch", "Item");
        state.getP1Hand().add(switchCard);
        String oldActiveName = state.getP1ActivePokemon().getName();

        ActionResult result = processor.process(switchCard, state, Map.of("benchIndex", 0));

        assertThat(result.isSuccess()).isTrue();
        assertThat(state.getP1ActivePokemon().getName()).isEqualTo("Squirtle");
        assertThat(state.getP1Bench()).anyMatch(p -> p.getName().equals(oldActiveName));
        assertThat(state.getP1Discard()).contains(switchCard);
    }

    @Test
    void cambio_falla_si_banca_vacia() {
        state.getP1Bench().clear();
        CardData switchCard = trainer("switch-1", "Switch", "Item");
        state.getP1Hand().add(switchCard);

        ActionResult result = processor.process(switchCard, state, Map.of());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("banca");
    }

    // ── Profesor Sycamore ─────────────────────────────────────────────────

    @Test
    void sycamore_descarta_mano_y_roba_7() {
        // Poner 3 cartas en mano y 10 en mazo
        state.getP1Hand().addAll(List.of(card("h1"), card("h2"), card("h3")));
        for (int i = 0; i < 10; i++) state.getP1Deck().add(card("deck-" + i));

        CardData sycamore = trainer("syc-1", "Professor Sycamore", "Supporter");
        state.getP1Hand().add(sycamore);

        ActionResult result = processor.process(sycamore, state, Map.of());

        assertThat(result.isSuccess()).isTrue();
        assertThat(state.getP1Hand()).hasSize(7);
        assertThat(state.getP1Discard()).contains(card("h1"), card("h2"), card("h3"));
        assertThat(state.getSupporterPlayedThisTurn()).isTrue();
    }

    @Test
    void sycamore_roba_menos_si_mazo_corto() {
        state.getP1Hand().add(card("h1"));
        state.getP1Deck().add(card("deck-1"));
        state.getP1Deck().add(card("deck-2"));

        CardData sycamore = trainer("syc-1", "Professor Sycamore", "Supporter");
        state.getP1Hand().add(sycamore);

        ActionResult result = processor.process(sycamore, state, Map.of());

        assertThat(result.isSuccess()).isTrue();
        assertThat(state.getP1Hand()).hasSize(2); // 2 cartas del mazo (no hay 7)
    }

    // ── N ─────────────────────────────────────────────────────────────────

    @Test
    void n_cada_jugador_roba_segun_sus_premios() {
        // P1 tiene 2 premios, P2 tiene 0 premios
        for (int i = 0; i < 5; i++) state.getP1Deck().add(card("p1d-" + i));
        for (int i = 0; i < 5; i++) state.getP2Deck().add(card("p2d-" + i));
        state.getP1Hand().add(card("hand-1"));
        state.getP2Hand().add(card("hand-2"));

        CardData nCard = trainer("n-1", "N", "Supporter");
        state.getP1Hand().add(nCard);

        ActionResult result = processor.process(nCard, state, Map.of());

        assertThat(result.isSuccess()).isTrue();
        // P1 tiene 2 premios → roba 2
        assertThat(state.getP1Hand()).hasSize(2);
        // P2 tiene 0 premios → roba 0
        assertThat(state.getP2Hand()).isEmpty();
    }

    // ── Estadio ───────────────────────────────────────────────────────────

    @Test
    void estadio_se_coloca_en_juego() {
        CardData stadium = trainer("stad-1", "Burning City", "Stadium");
        stadium = CardData.builder().id("stad-1").name("Burning City")
            .supertype("Trainer").subtypes(List.of("Stadium")).build();
        state.getP1Hand().add(stadium);

        ActionResult result = processor.process(stadium, state, Map.of());

        assertThat(result.isSuccess()).isTrue();
        assertThat(state.getStadiumCard()).isNotNull();
        assertThat(state.getStadiumCard().getName()).isEqualTo("Burning City");
        assertThat(state.getStadiumPlayedThisTurn()).isTrue();
    }

    @Test
    void estadio_reemplaza_anterior_y_lo_descarta() {
        CardData oldStadium = CardData.builder().id("old-stad").name("Old Stadium")
            .supertype("Trainer").subtypes(List.of("Stadium")).build();
        state.setStadiumCard(oldStadium);

        CardData newStadium = CardData.builder().id("new-stad").name("New Stadium")
            .supertype("Trainer").subtypes(List.of("Stadium")).build();
        state.getP1Hand().add(newStadium);

        processor.process(newStadium, state, Map.of());

        assertThat(state.getStadiumCard().getName()).isEqualTo("New Stadium");
        assertThat(state.getP1Discard()).contains(oldStadium);
    }

    // ── Herramienta Pokémon ───────────────────────────────────────────────

    @Test
    void herramienta_se_une_al_activo() {
        CardData tool = CardData.builder().id("tool-1").name("Muscle Band")
            .supertype("Trainer").subtypes(List.of("Pokémon Tool")).build();
        state.getP1Hand().add(tool);

        ActionResult result = processor.process(tool, state, Map.of("targetPosition", "ACTIVE"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(state.getP1ActivePokemon().getAttachedTool()).isEqualTo(tool);
    }

    @Test
    void herramienta_falla_si_ya_tiene_una() {
        CardData existingTool = CardData.builder().id("tool-0").name("Old Tool")
            .supertype("Trainer").subtypes(List.of("Pokémon Tool")).build();
        state.getP1ActivePokemon().setAttachedTool(existingTool);

        CardData tool = CardData.builder().id("tool-1").name("Muscle Band")
            .supertype("Trainer").subtypes(List.of("Pokémon Tool")).build();
        state.getP1Hand().add(tool);

        ActionResult result = processor.process(tool, state, Map.of("targetPosition", "ACTIVE"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("herramienta");
    }

    // ── Validación de turno ────────────────────────────────────────────────

    @Test
    void supporter_falla_si_ya_se_jugo_uno_este_turno() {
        state.setSupporterPlayedThisTurn(true);
        CardData sycamore = trainer("syc-1", "Professor Sycamore", "Supporter");
        state.getP1Hand().add(sycamore);

        ActionResult result = processor.process(sycamore, state, Map.of());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("Partidario");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private CardData card(String id) {
        return CardData.builder().id(id).name(id).build();
    }

    private CardData trainer(String id, String name, String subtype) {
        return CardData.builder()
            .id(id).name(name)
            .supertype("Trainer")
            .subtypes(List.of(subtype))
            .build();
    }
}
