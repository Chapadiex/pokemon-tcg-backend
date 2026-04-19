package com.pokemon.tcg.service;

import com.pokemon.tcg.client.PokemonTCGClient;
import com.pokemon.tcg.model.dto.SetDTO;
import com.pokemon.tcg.model.game.CardData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CardService {

    private final PokemonTCGClient apiClient;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CardService(PokemonTCGClient apiClient, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public CardData getCardById(String cardId) {
        Optional<CardData> cached = findInCache(cardId);
        if (cached.isPresent()) return cached.get();

        CardData card = apiClient.fetchCard(cardId);
        saveToCache(card);
        return card;
    }

    public List<CardData> searchCards(String query, int page) {
        List<CardData> local = searchInCache(query);
        if (!local.isEmpty()) return local;

        List<CardData> apiResults = apiClient.searchCards(query, page);
        apiResults.forEach(this::saveToCache);
        return apiResults;
    }

    public List<CardData> preloadDeck(List<String> cardIds) {
        return cardIds.stream()
            .distinct()
            .map(this::getCardById)
            .collect(Collectors.toList());
    }

    /**
     * Fetches all card IDs in a single batch cache query.
     * Any IDs not found in cache are fetched from the external API and cached.
     * Much faster than calling getCardById() N times.
     */
    public Map<String, CardData> getCardsByIdsBatch(List<String> cardIds) {
        if (cardIds.isEmpty()) return Collections.emptyMap();

        List<String> unique = cardIds.stream().distinct().collect(Collectors.toList());
        Map<String, CardData> result = new HashMap<>(batchFindInCache(unique));

        // Fetch any missing cards from external API in parallel
        List<String> missing = unique.stream()
            .filter(id -> !result.containsKey(id))
            .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            Map<String, CardData> fetched = new ConcurrentHashMap<>();
            List<CompletableFuture<Void>> futures = missing.stream()
                .map(id -> CompletableFuture.runAsync(() -> {
                    try {
                        CardData card = apiClient.fetchCard(id);
                        saveToCache(card);
                        fetched.put(id, card);
                    } catch (Exception ignored) {}
                }))
                .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            result.putAll(fetched);
        }

        return result;
    }

    private Map<String, CardData> batchFindInCache(List<String> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();

        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = "SELECT card_id, card_data FROM cached_cards WHERE card_id IN (" + placeholders + ")";

        try {
            List<Map.Entry<String, CardData>> rows = jdbcTemplate.query(
                sql,
                ids.toArray(),
                (rs, i) -> {
                    try {
                        String id  = rs.getString("card_id");
                        CardData cd = objectMapper.readValue(rs.getString("card_data"), CardData.class);
                        return Map.entry(id, cd);
                    } catch (Exception e) {
                        return null;
                    }
                }
            );
            return rows.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (DataAccessException ex) {
            // Fallback: fetch one by one if batch query fails
            Map<String, CardData> fallback = new HashMap<>();
            for (String id : ids) {
                findInCache(id).ifPresent(c -> fallback.put(id, c));
            }
            return fallback;
        }
    }

    private Optional<CardData> findInCache(String cardId) {
        try {
            String json = jdbcTemplate.queryForObject(
                "SELECT card_data FROM cached_cards WHERE card_id = ?",
                String.class, cardId);
            return Optional.ofNullable(objectMapper.readValue(json, CardData.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void saveToCache(CardData card) {
        try {
            String json = objectMapper.writeValueAsString(card);
            try {
                jdbcTemplate.update(
                    "INSERT INTO cached_cards (card_id, card_data) VALUES (?, ?::jsonb) " +
                    "ON CONFLICT (card_id) DO NOTHING",
                    card.getId(), json);
            } catch (DataAccessException pgOnlySqlFailed) {
                // H2/dev fallback: upsert without jsonb casts.
                jdbcTemplate.update(
                    "MERGE INTO cached_cards (card_id, card_data, cached_at) KEY(card_id) VALUES (?, ?, CURRENT_TIMESTAMP)",
                    card.getId(), json);
            }
        } catch (Exception ignored) {}
    }

    public List<SetDTO> getAllSets() {
        List<SetDTO> local;
        try {
            local = jdbcTemplate.query(
                "SELECT DISTINCT card_data->'set' as set_data FROM cached_cards WHERE card_data->'set' IS NOT NULL",
                (rs, i) -> {
                    try { return objectMapper.readValue(rs.getString(1), SetDTO.class); }
                    catch (Exception e) { return null; }
                }).stream().filter(Objects::nonNull).collect(Collectors.toList());
        } catch (DataAccessException ex) {
            // In H2 mode json operators may not be available; skip cache and use API.
            local = List.of();
        }

        if (!local.isEmpty()) return local;
        return apiClient.fetchSets();
    }

    // Inserta todas las cartas en cached_cards (idempotente — ON CONFLICT DO NOTHING)
    public void bulkCacheCards(List<CardData> cards) {
        cards.forEach(this::saveToCache);
    }

    // Cuenta cartas en cache para un setId dado (usado por SetDataLoader para idempotencia)
    public long countCachedCardsBySetId(String setId) {
        try {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cached_cards WHERE card_data->'set'->>'id' = ?",
                Long.class, setId);
            return count != null ? count : 0L;
        } catch (DataAccessException ex) {
            // Fallback H2: escanear en Java (card_data->'set' no soportado en H2 dev)
            try {
                return jdbcTemplate.query(
                    "SELECT card_data FROM cached_cards",
                    (rs, i) -> {
                        try { return objectMapper.readValue(rs.getString(1), CardData.class); }
                        catch (Exception e) { return null; }
                    })
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(c -> c.getSet() != null && setId.equals(c.getSet().getId()))
                    .count();
            } catch (DataAccessException inner) {
                return 0L;
            }
        }
    }

    private List<CardData> searchInCache(String query) {
        try {
            return jdbcTemplate.query(
                "SELECT card_data FROM cached_cards WHERE card_data->>'name' ILIKE ?",
                (rs, i) -> {
                    try { return objectMapper.readValue(rs.getString(1), CardData.class); }
                    catch (Exception e) { return null; }
                }, "%" + query + "%")
                .stream().filter(Objects::nonNull).collect(Collectors.toList());
        } catch (DataAccessException ex) {
            // Cross-db fallback for H2/dev: fetch cached JSON and filter in Java.
            String normalized = query.toLowerCase(Locale.ROOT);
            try {
                return jdbcTemplate.query(
                    "SELECT card_data FROM cached_cards",
                    (rs, i) -> {
                        try { return objectMapper.readValue(rs.getString(1), CardData.class); }
                        catch (Exception e) { return null; }
                    })
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(card -> card.getName() != null && card.getName().toLowerCase(Locale.ROOT).contains(normalized))
                    .collect(Collectors.toList());
            } catch (DataAccessException inner) {
                return List.of();
            }
        }
    }
}
