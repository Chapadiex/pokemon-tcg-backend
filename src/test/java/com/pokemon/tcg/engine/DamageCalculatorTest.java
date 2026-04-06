package com.pokemon.tcg.engine;

import com.pokemon.tcg.engine.model.DamageResult;
import com.pokemon.tcg.engine.model.GameStateSnapshot;
import com.pokemon.tcg.model.game.ActiveEffect;
import com.pokemon.tcg.model.game.AttackData;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.PokemonInPlay;
import com.pokemon.tcg.model.game.WeaknessResistance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DamageCalculatorTest {
    private final DamageCalculator calculator = new DamageCalculator();

    @Test
    void danio_base_sin_modificadores() {
        DamageResult result = calculator.calculateDamage(attacker(List.of("Fire"), null, null), defender(null, null), AttackData.builder().damage("30").build(), state(null));
        assertThat(result.getFinalDamage()).isEqualTo(30);
    }

    @Test
    void danio_con_debilidad_x2() {
        DamageResult result = calculator.calculateDamage(attacker(List.of("Fire"), null, null), defender(List.of(new WeaknessResistance("Fire", "\u00d72")), null), AttackData.builder().damage("50").build(), state(null));
        assertThat(result.getFinalDamage()).isEqualTo(100);
    }

    @Test
    void danio_con_resistencia_menos30() {
        DamageResult result = calculator.calculateDamage(attacker(List.of("Fire"), null, null), defender(null, List.of(new WeaknessResistance("Fire", "-30"))), AttackData.builder().damage("50").build(), state(null));
        assertThat(result.getFinalDamage()).isEqualTo(20);
    }

    @Test
    void danio_con_herramienta_mas20() {
        CardData tool = CardData.builder().text("This attack does 20 more damage").build();
        DamageResult result = calculator.calculateDamage(attacker(List.of("Fire"), tool, null), defender(null, null), AttackData.builder().damage("50").build(), state(null));
        assertThat(result.getFinalDamage()).isEqualTo(70);
    }

    @Test
    void danio_nunca_negativo() {
        DamageResult result = calculator.calculateDamage(attacker(List.of("Fire"), null, null), defender(null, List.of(new WeaknessResistance("Fire", "-30"))), AttackData.builder().damage("10").build(), state(null));
        assertThat(result.getFinalDamage()).isZero();
    }

    @Test
    void danio_sin_texto_es_cero() {
        DamageResult result = calculator.calculateDamage(attacker(List.of("Fire"), null, null), defender(null, null), AttackData.builder().damage("").build(), state(null));
        assertThat(result.getFinalDamage()).isZero();
    }

    @Test
    void danio_con_efecto_y_estadio() {
        CardData stadium = CardData.builder().text("Fire Pokemon attacks do 10 more damage").build();
        DamageResult result = calculator.calculateDamage(
            attacker(List.of("Fire"), null, List.of(ActiveEffect.builder().type("DAMAGE_MODIFIER").value(20).build())),
            defender(null, null),
            AttackData.builder().damage("30").build(),
            state(stadium)
        );
        assertThat(result.getFinalDamage()).isEqualTo(60);
    }

    private PokemonInPlay attacker(List<String> types, CardData tool, List<ActiveEffect> effects) {
        return PokemonInPlay.builder().name("Attacker").hp(100).types(types).attachedTool(tool).activeEffects(effects).build();
    }

    private PokemonInPlay defender(List<WeaknessResistance> weaknesses, List<WeaknessResistance> resistances) {
        return PokemonInPlay.builder().name("Defender").hp(100).weaknesses(weaknesses).resistances(resistances).build();
    }

    private GameStateSnapshot state(CardData stadium) {
        return GameStateSnapshot.builder().stadiumCard(stadium).build();
    }
}
