package com.labelcheck.dto;

/**
 * Structured model representing statutory information extracted from a packaged commodity label.
 * Fields are nullable when the corresponding declaration was not detected in the scanned photograph.
 *
 * @param productName              common/generic product name
 * @param brand                    brand name
 * @param netQuantity              net quantity / weight / volume (e.g. "150 g", "1 L")
 * @param mrp                      Maximum Retail Price declaration (e.g. "50.00")
 * @param mrpInclusiveOfTaxes      true if statutory "inclusive of all taxes" statement was detected
 * @param unitSalePrice            unit sale price if present (e.g. "Rs 0.33 per g")
 * @param manufacturerName         name of manufacturer or packer
 * @param manufacturerAddress      address/premises of manufacturer or packer
 * @param importerName             name of importer (for imported commodities)
 * @param importerAddress          address of importer
 * @param countryOfOrigin          country of origin or manufacture
 * @param manufactureOrPackingDate date of manufacture, packing, or import (MFD/MFG/PKD)
 * @param bestBeforeOrExpiry       expiry, best before, or use by date declaration
 * @param fssaiLicenseNumber       14-digit FSSAI registration/license number
 * @param customerCarePhone        consumer helpline / telephone number
 * @param customerCareEmail        consumer support email address
 * @param customerCareAddress      consumer care contact postal address
 * @param rawOcrText               unmodified raw text extracted by the OCR engine
 * @param batchNumber              batch, lot, or identification code
 * @param fssaiStatus              FSSAI declaration state: NUMBER_DETECTED, APPLIED_FOR, TEXT_PRESENT_NUMBER_NOT_DETECTED, NOT_DETECTED
 */
public record StructuredLabelData(
        String productName,
        String brand,
        String netQuantity,
        String mrp,
        Boolean mrpInclusiveOfTaxes,
        String unitSalePrice,
        String manufacturerName,
        String manufacturerAddress,
        String importerName,
        String importerAddress,
        String countryOfOrigin,
        String manufactureOrPackingDate,
        String bestBeforeOrExpiry,
        String fssaiLicenseNumber,
        String customerCarePhone,
        String customerCareEmail,
        String customerCareAddress,
        String rawOcrText,
        String batchNumber,
        String fssaiStatus
) {
    /**
     * Backward-compatible 18-parameter constructor for code written prior to batchNumber/fssaiStatus addition.
     */
    public StructuredLabelData(
            String productName,
            String brand,
            String netQuantity,
            String mrp,
            Boolean mrpInclusiveOfTaxes,
            String unitSalePrice,
            String manufacturerName,
            String manufacturerAddress,
            String importerName,
            String importerAddress,
            String countryOfOrigin,
            String manufactureOrPackingDate,
            String bestBeforeOrExpiry,
            String fssaiLicenseNumber,
            String customerCarePhone,
            String customerCareEmail,
            String customerCareAddress,
            String rawOcrText
    ) {
        this(productName, brand, netQuantity, mrp, mrpInclusiveOfTaxes, unitSalePrice,
                manufacturerName, manufacturerAddress, importerName, importerAddress,
                countryOfOrigin, manufactureOrPackingDate, bestBeforeOrExpiry,
                fssaiLicenseNumber, customerCarePhone, customerCareEmail, customerCareAddress,
                rawOcrText, null,
                fssaiLicenseNumber != null && fssaiLicenseNumber.matches("^[0-9]{14}$") ? "NUMBER_DETECTED" : "NOT_DETECTED");
    }

    /**
     * Backward-compatible constructor for 17-parameter initialization without explicit mrpInclusiveOfTaxes flag.
     */
    public StructuredLabelData(
            String productName,
            String brand,
            String netQuantity,
            String mrp,
            String unitSalePrice,
            String manufacturerName,
            String manufacturerAddress,
            String importerName,
            String importerAddress,
            String countryOfOrigin,
            String manufactureOrPackingDate,
            String bestBeforeOrExpiry,
            String fssaiLicenseNumber,
            String customerCarePhone,
            String customerCareEmail,
            String customerCareAddress,
            String rawOcrText
    ) {
        this(productName, brand, netQuantity, mrp, null, unitSalePrice, manufacturerName,
                manufacturerAddress, importerName, importerAddress, countryOfOrigin,
                manufactureOrPackingDate, bestBeforeOrExpiry, fssaiLicenseNumber,
                customerCarePhone, customerCareEmail, customerCareAddress, rawOcrText, null,
                fssaiLicenseNumber != null && fssaiLicenseNumber.matches("^[0-9]{14}$") ? "NUMBER_DETECTED" : "NOT_DETECTED");
    }

    /**
     * Counts how many of the 12 core statutory packaging declarations were successfully extracted.
     * Core statutory declarations under Legal Metrology Rule 6 & FSSAI:
     * 1. Product Name
     * 2. Brand
     * 3. Net Quantity
     * 4. Maximum Retail Price (MRP)
     * 5. Unit Sale Price (USP)
     * 6. Manufacturer Name
     * 7. Manufacturer Address
     * 8. Country of Origin
     * 9. Date of Manufacture/Packing (MFD/PKD)
     * 10. Best Before / Expiry Date
     * 11. FSSAI License Number / Status
     * 12. Consumer Care Details (Phone / Email / Address)
     *
     * @return count between 0 and 12
     */
    public int countDetectedFields() {
        int count = 0;
        if (isDeclared(productName)) count++;
        if (isDeclared(brand)) count++;
        if (isDeclared(netQuantity)) count++;
        if (isDeclared(mrp)) count++;
        if (isDeclared(unitSalePrice)) count++;
        if (isDeclared(manufacturerName) || isDeclared(importerName)) count++;
        if (isDeclared(manufacturerAddress) || isDeclared(importerAddress)) count++;
        if (isDeclared(countryOfOrigin)) count++;
        if (isDeclared(manufactureOrPackingDate)) count++;
        if (isDeclared(bestBeforeOrExpiry)) count++;
        if ((isDeclared(fssaiLicenseNumber) && !fssaiLicenseNumber.equalsIgnoreCase("NOT_DETECTED"))
                || "APPLIED_FOR".equalsIgnoreCase(fssaiStatus)) count++;
        if (isDeclared(customerCarePhone) || isDeclared(customerCareEmail) || isDeclared(customerCareAddress)) count++;
        return count;
    }

    private static boolean isDeclared(String val) {
        return val != null && !val.trim().isEmpty() && !val.equalsIgnoreCase("Not detected") && !val.equalsIgnoreCase("null");
    }

    /**
     * Categorizes label image into quality tiers:
     * GOOD_LABEL: 10-12 fields detected -> Compliance
     * AVERAGE_LABEL: 6-9 fields detected -> Compliance + Needs Review
     * POOR_LABEL: 1-5 fields detected -> Partial extraction + Needs Review
     * VERY_POOR_IMAGE: 0 fields detected -> Retake image
     */
    public String getQualityTier() {
        int count = countDetectedFields();
        if (count >= 10) {
            return "GOOD_LABEL";
        } else if (count >= 6) {
            return "AVERAGE_LABEL";
        } else if (count >= 1) {
            return "POOR_LABEL";
        } else {
            return "VERY_POOR_IMAGE";
        }
    }

    public String getQualityLabel() {
        return switch (getQualityTier()) {
            case "GOOD_LABEL" -> "Good Label";
            case "AVERAGE_LABEL" -> "Average Label";
            case "POOR_LABEL" -> "Poor Label";
            default -> "Very Poor Image";
        };
    }

    public String getComplianceOutcome() {
        return switch (getQualityTier()) {
            case "GOOD_LABEL" -> "Compliance";
            case "AVERAGE_LABEL" -> "Compliance + Needs Review";
            case "POOR_LABEL" -> "Partial extraction + Needs Review";
            default -> "Retake image";
        };
    }
}
