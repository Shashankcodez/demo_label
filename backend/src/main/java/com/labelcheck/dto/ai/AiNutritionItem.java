package com.labelcheck.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Structured nutritional item extracted visibly from the packaging nutrition facts table.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiNutritionItem(
        String nutrient,
        String amountPerServing,
        String amountPer100g,
        String unit
) {
    public static AiNutritionItem of(String nutrient, String amountPerServing, String amountPer100g, String unit) {
        return new AiNutritionItem(nutrient, amountPerServing, amountPer100g, unit);
    }
}
