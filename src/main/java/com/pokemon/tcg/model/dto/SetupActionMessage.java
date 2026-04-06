package com.pokemon.tcg.model.dto;

import lombok.Data;

import java.util.Map;

@Data
public class SetupActionMessage {
    private String type;
    private Map<String, Object> data;
}
