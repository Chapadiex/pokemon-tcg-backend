package com.pokemon.tcg.loader;

import com.pokemon.tcg.model.game.CardData;
import com.pokemon.tcg.service.CardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// Carga inicial de sets de cartas al arrancar la aplicación
// Es idempotente: si el set ya existe en cached_cards no lo vuelve a insertar
// Para agregar un set nuevo: solo crear su SetDefinition con @Component
@Component
public class SetDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SetDataLoader.class);

    private final List<SetDefinition> sets;   // Spring inyecta todas las implementaciones
    private final CardService cardService;

    public SetDataLoader(List<SetDefinition> sets, CardService cardService) {
        this.sets = sets;
        this.cardService = cardService;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (SetDefinition set : sets) {
            long existentes = cardService.countCachedCardsBySetId(set.getSetId());
            if (existentes > 0) {
                log.info("Set '{}' ya cargado ({} cartas). Saltando.", set.getSetName(), existentes);
                continue;
            }
            List<CardData> cartas = set.loadCards();
            cardService.bulkCacheCards(cartas);
            log.info("Set '{}' cargado: {} cartas insertadas.", set.getSetName(), cartas.size());
        }
    }
}
