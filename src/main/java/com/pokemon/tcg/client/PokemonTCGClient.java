package com.pokemon.tcg.client;

import com.pokemon.tcg.model.dto.SetDTO;
import com.pokemon.tcg.model.game.AttackData;
import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.model.game.CardImages;
import com.pokemon.tcg.model.game.SetData;
import com.pokemon.tcg.model.game.WeaknessResistance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
public class PokemonTCGClient {

    @Value("${pokemon.api.base-url}")
    private String baseUrl;

    @Value("${pokemon.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public PokemonTCGClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public CardData fetchCard(String cardId) {
        ResponseEntity<Map> response = restTemplate.exchange(
            baseUrl + "/cards/" + cardId,
            HttpMethod.GET, buildEntity(), Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return mapToCardData(data);
    }

    public List<CardData> searchCards(String query, int page) {
        String normalized = query == null ? "" : query.trim();
        String pokemonQuery = "name:*" + normalized + "*";
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/cards")
            .queryParam("q", pokemonQuery)
            .queryParam("page", page)
            .queryParam("pageSize", 20)
            .build()
            .toUriString();
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        if (data == null) return List.of();
        return data.stream().map(this::mapToCardData).toList();
    }

    @SuppressWarnings("unchecked")
    public List<SetDTO> fetchSets() {
        ResponseEntity<Map> response = restTemplate.exchange(
            baseUrl + "/sets?orderBy=releaseDate",
            HttpMethod.GET, buildEntity(), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        if (data == null) return List.of();
        return data.stream().map(d -> SetDTO.builder()
            .id((String) d.get("id"))
            .name((String) d.get("name"))
            .series((String) d.get("series"))
            .total(d.get("total") != null ? ((Number) d.get("total")).intValue() : null)
            .build()).toList();
    }

    private HttpEntity<Void> buildEntity() {
        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("X-Api-Key", apiKey);
        }
        return new HttpEntity<>(headers);
    }

    @SuppressWarnings("unchecked")
    private CardData mapToCardData(Map<String, Object> d) {
        CardData.CardDataBuilder builder = CardData.builder()
            .id((String) d.get("id"))
            .name((String) d.get("name"))
            .supertype((String) d.get("supertype"))
            .subtypes((List<String>) d.get("subtypes"))
            .types((List<String>) d.get("types"))
            .evolvesFrom((String) d.get("evolvesFrom"))
            .text((String) d.get("text"));

        // HP viene como String en la API
        if (d.get("hp") != null) {
            try { builder.hp(Integer.parseInt((String) d.get("hp"))); }
            catch (NumberFormatException ignored) {}
        }

        // retreatCost
        List<String> retreatCost = (List<String>) d.get("retreatCost");
        builder.retreatCost(retreatCost);
        if (d.get("convertedRetreatCost") != null) {
            builder.convertedRetreatCost(((Number) d.get("convertedRetreatCost")).intValue());
        }

        // Attacks
        List<Map<String, Object>> attacks = (List<Map<String, Object>>) d.get("attacks");
        if (attacks != null) {
            builder.attacks(attacks.stream().map(a -> AttackData.builder()
                .name((String) a.get("name"))
                .cost((List<String>) a.get("cost"))
                .damage((String) a.get("damage"))
                .text((String) a.get("text"))
                .convertedEnergyCost(a.get("convertedEnergyCost") != null
                    ? ((Number) a.get("convertedEnergyCost")).intValue() : 0)
                .build()).toList());
        }

        // Weaknesses / Resistances
        List<Map<String, Object>> weaknesses = (List<Map<String, Object>>) d.get("weaknesses");
        if (weaknesses != null) {
            builder.weaknesses(weaknesses.stream().map(w ->
                new WeaknessResistance((String) w.get("type"), (String) w.get("value"))).toList());
        }
        List<Map<String, Object>> resistances = (List<Map<String, Object>>) d.get("resistances");
        if (resistances != null) {
            builder.resistances(resistances.stream().map(r ->
                new WeaknessResistance((String) r.get("type"), (String) r.get("value"))).toList());
        }

        // Images
        Map<String, Object> images = (Map<String, Object>) d.get("images");
        if (images != null) {
            builder.images(new CardImages((String) images.get("small"), (String) images.get("large")));
        }

        // Set
        Map<String, Object> set = (Map<String, Object>) d.get("set");
        if (set != null) {
            builder.set(new SetData((String) set.get("id"), (String) set.get("name"), (String) set.get("series")));
        }

        return builder.build();
    }
}
