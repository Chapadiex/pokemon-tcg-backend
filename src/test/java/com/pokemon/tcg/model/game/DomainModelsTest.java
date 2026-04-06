package com.pokemon.tcg.model.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.config.JacksonConfig;
import com.pokemon.tcg.model.dto.CardDTO;
import com.pokemon.tcg.model.dto.DeckDTO;
import com.pokemon.tcg.model.dto.GameDTO;
import com.pokemon.tcg.model.dto.PlayerDTO;
import com.pokemon.tcg.model.entity.CardInDeck;
import com.pokemon.tcg.model.entity.Deck;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.entity.Player;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.model.enums.VictoryCondition;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelsTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Test
    void shouldDeserializeCardDataFromApiJson() throws Exception {
        String apiJson = """
            {
              "id": "base1-4",
              "name": "Charizard",
              "supertype": "Pok\u00e9mon",
              "subtypes": ["Stage 2"],
              "hp": "120",
              "types": ["Fire"],
              "evolvesFrom": "Charmeleon",
              "abilities": [
                { "name": "Energy Burn", "text": "As often as you like...", "type": "Pok\u00e9-Power" }
              ],
              "attacks": [
                {
                  "name": "Fire Spin",
                  "cost": ["Fire", "Fire", "Fire", "Fire"],
                  "convertedEnergyCost": 4,
                  "damage": "100",
                  "text": "Discard 2 Energy cards attached to Charizard in order to use this attack."
                }
              ],
              "weaknesses": [{ "type": "Water", "value": "\u00d72" }],
              "resistances": [{ "type": "Fighting", "value": "-30" }],
              "retreatCost": ["Colorless", "Colorless", "Colorless"],
              "convertedRetreatCost": 3,
              "set": { "id": "base1", "name": "Base", "series": "Base" },
              "images": {
                "small": "https://images.pokemontcg.io/base1/4.png",
                "large": "https://images.pokemontcg.io/base1/4_hires.png"
              }
            }
            """;

        CardData card = objectMapper.readValue(apiJson, CardData.class);

        assertThat(card.getHp()).isEqualTo(120);
        assertThat(card.isPokemon()).isTrue();
        assertThat(card.isStage2()).isTrue();
        assertThat(card.getImageUrl()).isEqualTo("https://images.pokemontcg.io/base1/4.png");
    }

    @Test
    void shouldExposeCardAndAttackHelpers() {
        CardData charmander = CardData.builder()
            .id("base1-46")
            .name("Charmander")
            .supertype("Pok\u00e9mon")
            .subtypes(List.of("Basic"))
            .types(List.of("Fire"))
            .images(new CardImages("small-url", "large-url"))
            .build();

        CardData potion = CardData.builder()
            .id("base1-94")
            .name("Potion")
            .supertype("Trainer")
            .subtypes(List.of("Item"))
            .build();

        AttackData fixedDamage = AttackData.builder().damage("100").cost(List.of("Fire", "Fire")).build();
        AttackData variableDamage = AttackData.builder().damage("10\u00d7").cost(List.of("Fire")).build();
        AttackData blankDamage = AttackData.builder().damage("").build();

        assertThat(charmander.isBasicPokemon()).isTrue();
        assertThat(potion.isBasicPokemon()).isFalse();
        assertThat(potion.isItem()).isTrue();
        assertThat(charmander.getPrimaryType()).isEqualTo("Fire");
        assertThat(charmander.getImageUrl()).isEqualTo("small-url");
        assertThat(fixedDamage.getBaseDamageValue()).isEqualTo(100);
        assertThat(variableDamage.getBaseDamageValue()).isEqualTo(10);
        assertThat(blankDamage.getBaseDamageValue()).isZero();
        assertThat(variableDamage.hasVariableDamage()).isTrue();
        assertThat(fixedDamage.getTotalEnergyCost()).isEqualTo(2);
    }

    @Test
    void shouldComputeWeaknessAndPokemonInPlayHelpers() {
        CardData charizard = CardData.builder()
            .id("base1-4")
            .name("Charizard")
            .supertype("Pok\u00e9mon")
            .subtypes(List.of("Stage 2"))
            .hp(120)
            .types(List.of("Fire"))
            .attacks(List.of(AttackData.builder().name("Fire Spin").build()))
            .abilities(List.of(AbilityData.builder().name("Energy Burn").build()))
            .weaknesses(List.of(new WeaknessResistance("Water", "\u00d72")))
            .resistances(List.of(new WeaknessResistance("Fighting", "-30")))
            .convertedRetreatCost(3)
            .images(new CardImages("small-url", "large-url"))
            .build();

        PokemonInPlay inPlay = PokemonInPlay.fromCard(charizard);
        inPlay.setDamageCounters(3);
        inPlay.setAttachedEnergies(List.of(
            EnergyAttached.builder().cardId("e1").type("Fire").name("Fire Energy").build(),
            EnergyAttached.builder().cardId("e2").type("Fire").name("Fire Energy").build()
        ));

        CardData charmeleon = CardData.builder()
            .id("base1-24")
            .name("Charmeleon")
            .supertype("Pok\u00e9mon")
            .subtypes(List.of("Stage 1"))
            .hp(80)
            .types(List.of("Fire"))
            .convertedRetreatCost(2)
            .build();

        PokemonInPlay evolved = PokemonInPlay.evolve(inPlay, charmeleon);

        assertThat(charizard.getWeaknesses().getFirst().computeModifier(50)).isEqualTo(50);
        assertThat(inPlay.getCurrentHp()).isEqualTo(90);
        assertThat(inPlay.countEnergyOfType("Fire")).isEqualTo(2);
        assertThat(inPlay.totalEnergies()).isEqualTo(2);
        assertThat(evolved.getDamageCounters()).isEqualTo(3);
        assertThat(evolved.getAttachedEnergies()).hasSize(2);
        assertThat(evolved.getEvolvedThisTurn()).isTrue();
    }

    @Test
    void shouldBuildDtosFromDomainObjects() {
        Player player1 = Player.builder().id(1L).username("ash").build();
        Player player2 = Player.builder().id(2L).username("misty").build();

        Deck deck = Deck.builder()
            .id(10L)
            .name("Fire Deck")
            .isValid(true)
            .createdAt(LocalDateTime.of(2026, 4, 5, 12, 0))
            .cards(List.of(
                CardInDeck.builder().cardId("base1-4").quantity(2).build(),
                CardInDeck.builder().cardId("base1-46").quantity(3).build()
            ))
            .build();

        Game game = Game.builder()
            .id(99L)
            .status(GameStatus.ACTIVE)
            .player1(player1)
            .player2(player2)
            .currentTurnPlayerId(1L)
            .turnNumber(4)
            .winner(player1)
            .victoryCondition(VictoryCondition.PRIZES)
            .createdAt(LocalDateTime.of(2026, 4, 5, 13, 30))
            .build();

        CardData card = CardData.builder()
            .id("base1-4")
            .name("Charizard")
            .supertype("Pok\u00e9mon")
            .subtypes(List.of("Stage 2"))
            .hp(120)
            .types(List.of("Fire"))
            .evolvesFrom("Charmeleon")
            .attacks(List.of(AttackData.builder().name("Fire Spin").build()))
            .weaknesses(List.of(new WeaknessResistance("Water", "\u00d72")))
            .resistances(List.of(new WeaknessResistance("Fighting", "-30")))
            .convertedRetreatCost(3)
            .text("Discard 2 Energy cards attached to Charizard in order to use this attack.")
            .images(new CardImages("small-url", "large-url"))
            .set(SetData.builder().id("base1").name("Base").series("Base").build())
            .build();

        PlayerDTO playerDto = PlayerDTO.from(player1);
        DeckDTO deckDto = DeckDTO.from(deck);
        GameDTO gameDto = GameDTO.from(game);
        CardDTO cardDto = CardDTO.from(card);

        assertThat(playerDto.getUsername()).isEqualTo("ash");
        assertThat(deckDto.getCardCount()).isEqualTo(5);
        assertThat(deckDto.getCardIds()).containsExactly("base1-4", "base1-46");
        assertThat(gameDto.getStatus()).isEqualTo("ACTIVE");
        assertThat(gameDto.getWinnerId()).isEqualTo(1L);
        assertThat(cardDto.getImageUrl()).isEqualTo("small-url");
        assertThat(cardDto.getSetName()).isEqualTo("Base");
    }
}
