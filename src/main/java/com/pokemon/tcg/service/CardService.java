package com.pokemon.tcg.service;

import com.pokemon.tcg.client.PokemonTCGClient;
import com.pokemon.tcg.model.dto.SetDTO;
import com.pokemon.tcg.model.game.CardData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
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
