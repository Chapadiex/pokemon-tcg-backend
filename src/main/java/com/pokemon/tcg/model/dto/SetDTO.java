package com.pokemon.tcg.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetDTO {
    private String  id;
    private String  name;
    private String  series;
    private Integer total;
    private String  imageUrl;
}
