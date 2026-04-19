package com.pokemon.tcg.loader.xy;

import com.pokemon.tcg.loader.xy.dto.XyCardDto;
import com.pokemon.tcg.loader.xy.dto.XySetFileDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class XySetParserTest {

    @Autowired
    private XySetParser parser;

    @Test
    void debeCargar146Cartas() {
        XySetFileDto file = parser.parse("/data/sets/setXY_completo.json");
        assertEquals(146, file.getCards().size());
    }

    @Test
    void debeClasificarSupertypesCorrectamente() {
        XySetFileDto file = parser.parse("/data/sets/setXY_completo.json");
        long pokemon  = file.getCards().stream()
            .filter(c -> "Pok\u00e9mon".equals(c.getSupertype())).count();
        long trainers = file.getCards().stream()
            .filter(c -> "Trainer".equals(c.getSupertype())).count();
        long energies = file.getCards().stream()
            .filter(c -> "Energy".equals(c.getSupertype())).count();
        assertEquals(120, pokemon);
        assertEquals(15, trainers);
        assertEquals(11, energies);
    }

    @Test
    void debeParsearAbilitiesCorrectamente() {
        XySetFileDto file = parser.parse("/data/sets/setXY_completo.json");
        long conAbility = file.getCards().stream()
            .filter(c -> c.getAbilities() != null && !c.getAbilities().isEmpty())
            .count();
        assertEquals(12, conAbility);
    }

    @Test
    void debeParsearCardsEXConRegla2Premios() {
        XySetFileDto file = parser.parse("/data/sets/setXY_completo.json");
        long exCards = file.getCards().stream()
            .filter(c -> c.getSubtypes() != null && c.getSubtypes().contains("EX"))
            .count();
        assertEquals(14, exCards);
        // Verificar que todas las EX tienen la regla de 2 premios en rules[0]
        file.getCards().stream()
            .filter(c -> c.getSubtypes() != null && c.getSubtypes().contains("EX"))
            .forEach(c -> {
                assertNotNull(c.getRules(), "EX card debe tener rules: " + c.getName());
                assertTrue(c.getRules().get(0).contains("2 Prize cards"),
                    "EX rule debe mencionar 2 prizes: " + c.getName());
            });
    }

    @Test
    void debeParsearDamagePatterns() {
        XySetFileDto file = parser.parse("/data/sets/setXY_completo.json");
        // Venusaur-EX tiene "60+" como primer ataque
        XyCardDto venusaur = file.getCards().stream()
            .filter(c -> "Venusaur-EX".equals(c.getName()))
            .findFirst().orElseThrow();
        assertNotNull(venusaur.getAttacks().get(0).getDamage());
    }

    @Test
    void debeParsearTrainersConRules() {
        XySetFileDto file = parser.parse("/data/sets/setXY_completo.json");
        XyCardDto sycamore = file.getCards().stream()
            .filter(c -> "Professor Sycamore".equals(c.getName()))
            .findFirst().orElseThrow();
        assertNotNull(sycamore.getRules());
        assertFalse(sycamore.getRules().isEmpty());
        assertTrue(sycamore.getRules().get(0).contains("Discard your hand and draw 7 cards"));
    }
}
