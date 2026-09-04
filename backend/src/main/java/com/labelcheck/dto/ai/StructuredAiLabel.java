package com.labelcheck.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Strongly-typed packaging label extraction model populated by the Vision AI model.
 * Each statutory element holds value, confidence, and visual evidence.
 * Missing declarations are represented as null or empty FieldExtraction instances.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StructuredAiLabel(
        Double overallConfidence,
        FieldExtraction productName,
        FieldExtraction brand,
        FieldExtraction netQuantity,
        FieldExtraction mrp,
        FieldExtraction mrpIncludesTaxes,
        FieldExtraction unitSalePrice,
        FieldExtraction batchNumber,
        FieldExtraction manufacturedOrPackedDate,
        FieldExtraction bestBeforeOrExpiry,
        FieldExtraction fssaiLicenseNumber,
        FieldExtraction fssaiStatus,
        FieldExtraction manufacturer,
        FieldExtraction packer,
        FieldExtraction marketer,
        FieldExtraction importer,
        FieldExtraction address,
        FieldExtraction countryOfOrigin,
        FieldExtraction phone,
        FieldExtraction email,
        FieldExtraction consumerCare,
        FieldExtraction ingredients,
        FieldExtraction allergens,
        FieldExtraction vegetarianSymbol,
        FieldExtraction storageInstructions,
        List<AiNutritionItem> nutrition,
        List<String> otherDeclarations
) {
    public StructuredAiLabel {
        if (nutrition == null) nutrition = new ArrayList<>();
        if (otherDeclarations == null) otherDeclarations = new ArrayList<>();
    }

    /**
     * Counts how many of the 12 core statutory packaging declarations have visible evidence.
     */
    public int countDetectedFields() {
        int count = 0;
        if (isDeclared(productName)) count++;
        if (isDeclared(brand)) count++;
        if (isDeclared(netQuantity)) count++;
        if (isDeclared(mrp)) count++;
        if (isDeclared(unitSalePrice)) count++;
        if (isDeclared(manufacturer) || isDeclared(packer) || isDeclared(marketer) || isDeclared(importer)) count++;
        if (isDeclared(address)) count++;
        if (isDeclared(countryOfOrigin)) count++;
        if (isDeclared(manufacturedOrPackedDate)) count++;
        if (isDeclared(bestBeforeOrExpiry)) count++;
        if (isDeclared(fssaiLicenseNumber) || (isDeclared(fssaiStatus) && !"NOT_DETECTED".equalsIgnoreCase(fssaiStatus.value()))) count++;
        if (isDeclared(phone) || isDeclared(email) || isDeclared(consumerCare)) count++;
        return count;
    }

    public double calculateAverageConfidence() {
        if (overallConfidence != null && overallConfidence > 0) {
            return Math.max(0.0, Math.min(1.0, overallConfidence));
        }
        double sum = 0.0;
        int count = 0;
        FieldExtraction[] fields = new FieldExtraction[]{
                productName, brand, netQuantity, mrp, unitSalePrice, batchNumber,
                manufacturedOrPackedDate, bestBeforeOrExpiry, fssaiLicenseNumber,
                manufacturer, packer, marketer, address, countryOfOrigin, phone, email, consumerCare
        };
        for (FieldExtraction f : fields) {
            if (isDeclared(f)) {
                sum += f.safeConfidence();
                count++;
            }
        }
        return count > 0 ? (sum / count) : 0.0;
    }

    private static boolean isDeclared(FieldExtraction field) {
        return field != null && field.isPresent();
    }
}
