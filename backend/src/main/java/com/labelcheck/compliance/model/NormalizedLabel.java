package com.labelcheck.compliance.model;

import com.labelcheck.dto.StructuredLabelData;

import java.util.Map;

/**
 * Normalized data model decoupling extraction results from statutory legal rule validation.
 * Preserves raw evidence, normalized values, and field-level confidence scores.
 */
public record NormalizedLabel(
        String productName,
        String genericName,
        String brand,
        String netQuantity,
        Double netQuantityNumeric,
        String netQuantityUnit,
        String mrp,
        Double mrpNumeric,
        Boolean mrpInclusiveOfTaxes,
        String unitSalePrice,
        Double unitSalePriceNumeric,
        String unitSalePriceUnit,
        String manufacturerName,
        String manufacturerAddress,
        String packerName,
        String packerAddress,
        String marketerName,
        String marketerAddress,
        String importerName,
        String importerAddress,
        String countryOfOrigin,
        String manufactureOrPackingDate,
        String bestBeforeOrExpiry,
        String batchNumber,
        String customerCarePhone,
        String customerCareEmail,
        String customerCareAddress,
        String fssaiLicenseNumber,
        String fssaiStatus,
        Map<String, String> fieldEvidence,
        Map<String, Double> fieldConfidence,
        String rawOcrText,
        boolean imageQualitySufficient
) {
    /**
     * Converts a legacy StructuredLabelData instance into a NormalizedLabel.
     */
    public static NormalizedLabel fromStructuredData(StructuredLabelData data, Map<String, String> evidence, Map<String, Double> confidence) {
        if (data == null) {
            return new NormalizedLabel(
                    null, null, null, null, null, null,
                    null, null, false, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, "NOT_DETECTED",
                    evidence != null ? evidence : Map.of(),
                    confidence != null ? confidence : Map.of(),
                    "", false
            );
        }

        Double mrpVal = parseDoubleSafe(data.mrp());
        Double qtyVal = parseQuantityNumeric(data.netQuantity());
        String qtyUnit = parseQuantityUnit(data.netQuantity());

        return new NormalizedLabel(
                data.productName(),
                inferGenericName(data.productName()),
                data.brand(),
                data.netQuantity(),
                qtyVal,
                qtyUnit,
                data.mrp(),
                mrpVal,
                Boolean.TRUE.equals(data.mrpInclusiveOfTaxes()),
                data.unitSalePrice(),
                parseUspNumeric(data.unitSalePrice()),
                parseUspUnit(data.unitSalePrice()),
                data.manufacturerName(),
                data.manufacturerAddress(),
                null, null, null, null,
                data.importerName(),
                data.importerAddress(),
                data.countryOfOrigin(),
                data.manufactureOrPackingDate(),
                data.bestBeforeOrExpiry(),
                data.batchNumber(),
                data.customerCarePhone(),
                data.customerCareEmail(),
                data.customerCareAddress(),
                data.fssaiLicenseNumber(),
                data.fssaiStatus(),
                evidence != null ? evidence : Map.of(),
                confidence != null ? confidence : Map.of(),
                data.rawOcrText() != null ? data.rawOcrText() : "",
                data.countDetectedFields() > 0
        );
    }

    private static Double parseDoubleSafe(String s) {
        if (s == null) return null;
        try {
            String clean = s.replaceAll("[^0-9.]", "").trim();
            if (clean.isEmpty()) return null;
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseQuantityNumeric(String s) {
        if (s == null) return null;
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(s);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String parseQuantityUnit(String s) {
        if (s == null) return null;
        String lower = s.toLowerCase();
        if (lower.contains("kg") || lower.contains("kilogram")) return "kg";
        if (lower.contains("gm") || lower.contains("gram") || lower.matches(".*\\d\\s*g\\b.*")) return "g";
        if (lower.contains("ml") || lower.contains("millilitre")) return "ml";
        if (lower.contains("ltr") || lower.contains("litre") || lower.matches(".*\\d\\s*l\\b.*")) return "L";
        if (lower.contains("piece") || lower.contains("unit") || lower.contains("n") || lower.contains("u")) return "N";
        if (lower.contains("metre") || lower.contains("meter") || lower.contains("m")) return "m";
        if (lower.contains("cm") || lower.contains("centimetre")) return "cm";
        return null;
    }

    private static Double parseUspNumeric(String s) {
        if (s == null) return null;
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(s);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String parseUspUnit(String s) {
        if (s == null) return null;
        String lower = s.toLowerCase();
        if (lower.contains("/g") || lower.contains("per g")) return "g";
        if (lower.contains("/kg") || lower.contains("per kg")) return "kg";
        if (lower.contains("/ml") || lower.contains("per ml")) return "ml";
        if (lower.contains("/l") || lower.contains("per l") || lower.contains("per litre")) return "L";
        if (lower.contains("/n") || lower.contains("per piece") || lower.contains("per unit")) return "N";
        return null;
    }

    private static String inferGenericName(String productName) {
        if (productName == null || productName.isBlank()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(?i)\\b(oil|biscuit|cookie|wafer|water|juice|snack|atta|flour|rice|dal|pulse|tea|coffee|masala|spice|noodle|pasta|milk|butter|ghee|paneer|curd|shampoo|soap|detergent|cleaner|cream|lotion|paste|sugar|salt|cereal|chips|chocolate|sauce|jam|drink|beverage|ice\\s*cream|apple\\s*slice|sliced\\s*apple)\\b"
        ).matcher(productName);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
