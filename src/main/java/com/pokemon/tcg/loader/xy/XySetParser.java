package com.pokemon.tcg.loader.xy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.loader.xy.dto.XySetFileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

// Parsea el archivo setXY_completo.json desde el classpath
// Devuelve el DTO raíz sin transformar ni mapear — solo lectura del JSON
@Component
public class XySetParser {

    private static final Logger log = LoggerFactory.getLogger(XySetParser.class);
    private final ObjectMapper objectMapper;

    public XySetParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Lee el JSON del classpath y devuelve el DTO raíz
    // classpathResource ejemplo: "/data/sets/setXY_completo.json"
    public XySetFileDto parse(String classpathResource) {
        try {
            InputStream is = getClass().getResourceAsStream(classpathResource);
            if (is == null) {
                throw new IllegalArgumentException(
                    "Recurso no encontrado en el classpath: " + classpathResource
                );
            }
            XySetFileDto result = objectMapper.readValue(is, XySetFileDto.class);
            log.info("Set XY parseado correctamente: {} cartas desde {}",
                result.getCards().size(), classpathResource);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(
                "Error al parsear el JSON del set XY: " + classpathResource, e
            );
        }
    }
}
