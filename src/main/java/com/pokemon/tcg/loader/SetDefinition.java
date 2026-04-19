package com.pokemon.tcg.loader;

import com.pokemon.tcg.model.game.CardData;

import java.util.List;

// Contrato para cargar un set de cartas al sistema
// Para agregar un set nuevo (Evolutions, PrimalClash, etc.):
// 1. Crear una nueva clase que implemente esta interface
// 2. Anotarla con @Component
// 3. Spring la inyectará automáticamente en SetDataLoader — no tocar nada más
public interface SetDefinition {

    // ID único del set, debe coincidir con el campo "id" de la expansion en el JSON
    // Ejemplo: "xy1"
    String getSetId();

    // Nombre legible del set para logs
    // Ejemplo: "XY"
    String getSetName();

    // Carga y devuelve todas las cartas del set listas para persistir
    List<CardData> loadCards();
}
