package com.pokemon.tcg.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class JsonUtil {
    private final ObjectMapper objectMapper;

    public JsonUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> String toJson(T obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando a JSON", e);
        }
    }

    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(unwrapIfJsonString(json), clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializando JSON", e);
        }
    }

    public <T> List<T> fromJsonList(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(unwrapIfJsonString(json),
                objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializando JSON list", e);
        }
    }

    /**
     * Compatibilidad con registros legacy donde una columna JSONB quedo
     * doble-serializada (ej: "\"[{...}]\"").
     */
    private String unwrapIfJsonString(String json) throws JsonProcessingException {
        if (json == null) {
            return null;
        }
        String trimmed = json.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            String unwrapped = objectMapper.readValue(trimmed, new TypeReference<String>() {});
            if (unwrapped != null && !unwrapped.isBlank()) {
                return unwrapped.trim();
            }
        }
        return trimmed;
    }
}
