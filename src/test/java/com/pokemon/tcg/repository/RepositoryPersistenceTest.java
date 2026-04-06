package com.pokemon.tcg.repository;

import com.pokemon.tcg.model.entity.Action;
import com.pokemon.tcg.model.entity.Deck;
import com.pokemon.tcg.model.entity.Game;
import com.pokemon.tcg.model.entity.GameState;
import com.pokemon.tcg.model.entity.Player;
import com.pokemon.tcg.model.entity.Turn;
import com.pokemon.tcg.model.enums.ActionType;
import com.pokemon.tcg.model.enums.GameStatus;
import com.pokemon.tcg.model.enums.VictoryCondition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RepositoryPersistenceTest {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameStateRepository gameStateRepository;

    @Autowired
    private TurnRepository turnRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Test
    void shouldPersistPlayerAndFindByUsername() {
        Player player = playerRepository.save(Player.builder().username("ash").build());

        assertThat(player.getId()).isNotNull();
        assertThat(player.getCreatedAt()).isNotNull();
        assertThat(playerRepository.findByUsername("ash")).contains(player);
    }

    @Test
    void shouldPersistCoreGameEntitiesAndRepositoryQueries() {
        Player player1 = playerRepository.save(Player.builder().username("red").build());
        Player player2 = playerRepository.save(Player.builder().username("blue").build());

        Deck deck1 = deckRepository.save(Deck.builder().player(player1).name("Deck One").isValid(true).build());
        Deck deck2 = deckRepository.save(Deck.builder().player(player2).name("Deck Two").isValid(true).build());

        Game game = gameRepository.save(Game.builder()
            .player1(player1)
            .player2(player2)
            .player1Deck(deck1)
            .player2Deck(deck2)
            .status(GameStatus.ACTIVE)
            .currentTurnPlayerId(player1.getId())
            .turnNumber(1)
            .isFirstTurn(false)
            .victoryCondition(VictoryCondition.PRIZES)
            .build());

        GameState gameState = gameStateRepository.save(GameState.builder()
            .game(game)
            .p1Deck("[]")
            .p1Hand("[]")
            .p1Prizes("[]")
            .p1Discard("[]")
            .p1Bench("[]")
            .p2Deck("[]")
            .p2Hand("[]")
            .p2Prizes("[]")
            .p2Discard("[]")
            .p2Bench("[]")
            .energyAttachedThisTurn(false)
            .supporterPlayedThisTurn(false)
            .stadiumPlayedThisTurn(false)
            .retreatUsedThisTurn(false)
            .build());

        Turn turn = turnRepository.save(Turn.builder()
            .game(game)
            .turnNumber(1)
            .playerId(player1.getId())
            .build());

        Action action = actionRepository.save(Action.builder()
            .turn(turn)
            .actionType(ActionType.DRAW_CARD)
            .actionData("{}")
            .isValid(true)
            .build());

        assertThat(deckRepository.findByPlayerId(player1.getId())).extracting(Deck::getId).contains(deck1.getId());
        assertThat(gameRepository.findByStatus(GameStatus.ACTIVE)).extracting(Game::getId).contains(game.getId());
        assertThat(gameStateRepository.findByGameId(game.getId())).contains(gameState);
        assertThat(turnRepository.findByGameIdOrderByTurnNumberAsc(game.getId())).extracting(Turn::getId).contains(turn.getId());
        assertThat(actionRepository.findAll()).extracting(Action::getId).contains(action.getId());

        List<Turn> turns = turnRepository.findByGameIdOrderByTurnNumberAsc(game.getId());
        assertThat(turns).hasSize(1);
        assertThat(turns.getFirst().getTurnNumber()).isEqualTo(1);
    }
}
