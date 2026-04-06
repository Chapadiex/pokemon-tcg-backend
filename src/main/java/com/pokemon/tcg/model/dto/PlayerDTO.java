package com.pokemon.tcg.model.dto;

import com.pokemon.tcg.model.entity.Player;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerDTO {
    private Long   id;
    private String username;

    public static PlayerDTO from(Player p) {
        return PlayerDTO.builder().id(p.getId()).username(p.getUsername()).build();
    }
}
