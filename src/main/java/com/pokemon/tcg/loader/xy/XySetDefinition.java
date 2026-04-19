package com.pokemon.tcg.loader.xy;

import com.pokemon.tcg.loader.SetDefinition;
import com.pokemon.tcg.loader.xy.dto.XyExpansionDto;
import com.pokemon.tcg.loader.xy.dto.XySetFileDto;
import com.pokemon.tcg.model.game.CardData;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

// Implementación del set XY
// Carga las 146 cartas desde setXY_completo.json y las convierte al modelo de dominio
@Component
public class XySetDefinition implements SetDefinition {

    private static final String JSON_RESOURCE = "/data/sets/setXY_completo.json";

    private final XySetParser parser;
    private final XyCardMapper mapper;

    public XySetDefinition(XySetParser parser, XyCardMapper mapper) {
        this.parser = parser;
        this.mapper = mapper;
    }

    @Override
    public String getSetId() { return "xy1"; }

    @Override
    public String getSetName() { return "XY"; }

    @Override
    public List<CardData> loadCards() {
        XySetFileDto file = parser.parse(JSON_RESOURCE);
        XyExpansionDto expansion = file.getExpansion();
        return file.getCards().stream()
            .map(dto -> mapper.toCardData(dto, expansion))
            .collect(Collectors.toList());
    }
}
