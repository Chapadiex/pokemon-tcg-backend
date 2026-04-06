package com.pokemon.tcg.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class HpDeserializer extends JsonDeserializer<Integer> {
    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctx) throws IOException, JsonProcessingException {
        String value = p.getText();
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
