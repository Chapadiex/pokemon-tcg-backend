-- ===================================
-- PLAYERS
-- ===================================
CREATE TABLE IF NOT EXISTS players (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  UNIQUE NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_players_username ON players(username);

-- ===================================
-- DECKS
-- ===================================
CREATE TABLE IF NOT EXISTS decks (
    id          BIGSERIAL PRIMARY KEY,
    player_id   BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    is_valid    BOOLEAN      DEFAULT FALSE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_decks_player ON decks(player_id);

-- ===================================
-- CARDS IN DECK
-- ===================================
CREATE TABLE IF NOT EXISTS cards_in_deck (
    id       BIGSERIAL PRIMARY KEY,
    deck_id  BIGINT      NOT NULL REFERENCES decks(id) ON DELETE CASCADE,
    card_id  VARCHAR(50) NOT NULL,
    quantity INT         NOT NULL CHECK (quantity > 0 AND quantity <= 4),
    UNIQUE (deck_id, card_id)
);

CREATE INDEX IF NOT EXISTS idx_cards_deck ON cards_in_deck(deck_id);

-- ===================================
-- GAMES
-- ===================================
CREATE TYPE IF NOT EXISTS game_status AS ENUM (
    'WAITING', 'SETUP', 'ACTIVE', 'FINISHED', 'CANCELLED'
);

CREATE TYPE IF NOT EXISTS victory_condition AS ENUM (
    'PRIZES', 'NO_POKEMON', 'DECK_OUT', 'SURRENDER', 'SUDDEN_DEATH'
);

CREATE TABLE IF NOT EXISTS games (
    id                      BIGSERIAL PRIMARY KEY,
    player1_id              BIGINT       NOT NULL REFERENCES players(id),
    player2_id              BIGINT                REFERENCES players(id),
    player1_deck_id         BIGINT       NOT NULL REFERENCES decks(id),
    player2_deck_id         BIGINT                REFERENCES decks(id),
    status                  game_status  DEFAULT 'WAITING',
    current_turn_player_id  BIGINT,
    turn_number             INT          DEFAULT 0,
    is_first_turn           BOOLEAN      DEFAULT TRUE,
    winner_id               BIGINT       REFERENCES players(id),
    victory_condition       victory_condition,
    is_sudden_death         BOOLEAN      DEFAULT FALSE,
    parent_game_id          BIGINT       REFERENCES games(id),
    created_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    started_at              TIMESTAMP,
    finished_at             TIMESTAMP,
    CONSTRAINT valid_players CHECK (player1_id != player2_id)
);

CREATE INDEX IF NOT EXISTS idx_games_status  ON games(status);
CREATE INDEX IF NOT EXISTS idx_games_players ON games(player1_id, player2_id);

-- ===================================
-- GAME STATES
-- ===================================
CREATE TABLE IF NOT EXISTS game_states (
    id                          BIGSERIAL PRIMARY KEY,
    game_id                     BIGINT    UNIQUE NOT NULL REFERENCES games(id) ON DELETE CASCADE,

    -- Player 1
    p1_deck                     JSONB     NOT NULL DEFAULT '[]',
    p1_hand                     JSONB     NOT NULL DEFAULT '[]',
    p1_prizes                   JSONB     NOT NULL DEFAULT '[]',
    p1_discard                  JSONB     NOT NULL DEFAULT '[]',
    p1_active_pokemon           JSONB,
    p1_bench                    JSONB     NOT NULL DEFAULT '[]',

    -- Player 2
    p2_deck                     JSONB     NOT NULL DEFAULT '[]',
    p2_hand                     JSONB     NOT NULL DEFAULT '[]',
    p2_prizes                   JSONB     NOT NULL DEFAULT '[]',
    p2_discard                  JSONB     NOT NULL DEFAULT '[]',
    p2_active_pokemon           JSONB,
    p2_bench                    JSONB     NOT NULL DEFAULT '[]',

    -- Shared
    stadium_card                JSONB,
    pending_bench_damage        JSONB     DEFAULT '[]',

    -- Turn flags
    energy_attached_this_turn   BOOLEAN   DEFAULT FALSE,
    supporter_played_this_turn  BOOLEAN   DEFAULT FALSE,
    stadium_played_this_turn    BOOLEAN   DEFAULT FALSE,
    retreat_used_this_turn      BOOLEAN   DEFAULT FALSE,

    last_updated                TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_game_states_game ON game_states(game_id);

-- ===================================
-- TURNS
-- ===================================
CREATE TABLE IF NOT EXISTS turns (
    id           BIGSERIAL PRIMARY KEY,
    game_id      BIGINT    NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    turn_number  INT       NOT NULL,
    player_id    BIGINT    NOT NULL REFERENCES players(id),
    started_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_turns_game ON turns(game_id);

-- ===================================
-- ACTIONS
-- ===================================
CREATE TYPE IF NOT EXISTS action_type AS ENUM (
    'DRAW_CARD', 'PLACE_POKEMON', 'EVOLVE', 'ATTACH_ENERGY',
    'PLAY_TRAINER', 'RETREAT', 'ATTACK', 'BETWEEN_TURNS',
    'TAKE_PRIZE', 'KNOCKOUT'
);

CREATE TABLE IF NOT EXISTS actions (
    id               BIGSERIAL    PRIMARY KEY,
    turn_id          BIGINT       NOT NULL REFERENCES turns(id) ON DELETE CASCADE,
    action_type      action_type  NOT NULL,
    action_data      JSONB        NOT NULL DEFAULT '{}',
    is_valid         BOOLEAN      DEFAULT TRUE,
    validation_error TEXT,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_actions_turn ON actions(turn_id);

-- ===================================
-- CACHED CARDS (cache pokemontcg.io)
-- ===================================
CREATE TABLE IF NOT EXISTS cached_cards (
    card_id    VARCHAR(50) PRIMARY KEY,
    card_data  JSONB       NOT NULL,
    cached_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cached_cards_name
    ON cached_cards((card_data->>'name'));
CREATE INDEX IF NOT EXISTS idx_cached_cards_supertype
    ON cached_cards((card_data->>'supertype'));
