package com.labelcheck.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelcheck.config.AiProperties;
import com.labelcheck.dto.StructuredLabelData;
import com.labelcheck.dto.ai.AiLabelExtractionResult;
import com.labelcheck.dto.ai.FieldExtraction;
import com.labelcheck.dto.ai.StructuredAiLabel;
import com.labelcheck.service.ai.ExtractionMergeService;
import com.labelcheck.service.ai.GeminiVisionLabelExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mandatory Phase 11 Refined Sunflower Oil Regression Test.
 * Validates that:
 * - productName = "REFINED SUNFLOWER OIL" (never "Manufactured and Packed at")
 * - mrp = "100" (never 2100)
 * - netQuantity = "1 Litre"
 * - batchNumber = "PS200"
 * - packedDate = "July 13, 2017"
 * - bestBefore contains "nine months"
 * - phone = "7871234567"
 * - manufacturer is null rather than heading fragments ("Manufactured and")
 * - FSSAI license is preserved as candidate with status TEXT_PRESENT_NUMBER_NEEDS_REVIEW
 */
class SunflowerOilRegressionTest {

    private ExtractionMergeService mergeService;
    private GeminiVisionLabelExtractor geminiExtractor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AiProperties aiProperties = new AiProperties();
        objectMapper = new ObjectMapper();
        mergeService = new ExtractionMergeService();
        geminiExtractor = new GeminiVisionLabelExtractor(aiProperties, objectMapper, new ImagePreprocessingService());
    }

    @Test
    @DisplayName("Phase 11: Sunflower Oil structured JSON parsing by GeminiVisionLabelExtractor")
    void testSunflowerOil_geminiResponseParsing() {
        String mockGeminiResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\n  \\"overallConfidence\\": 0.95,\\n  \\"productName\\": { \\"value\\": \\"REFINED SUNFLOWER OIL\\", \\"confidence\\": 0.98, \\"evidence\\": \\"Large central front-panel text\\" },\\n  \\"brand\\": { \\"value\\": \\"SUNFLOWER\\", \\"confidence\\": 0.90, \\"evidence\\": \\"Front branding\\" },\\n  \\"netQuantity\\": { \\"value\\": \\"1 Litre\\", \\"confidence\\": 0.96, \\"evidence\\": \\"NET QUANTITY AT 30 C : 1 Litre\\" },\\n  \\"mrp\\": { \\"value\\": \\"₹100/-\\", \\"confidence\\": 0.97, \\"evidence\\": \\"M.R.P. 2100/- (Inclusive of all Taxes)\\" },\\n  \\"mrpIncludesTaxes\\": { \\"value\\": \\"true\\", \\"confidence\\": 0.95, \\"evidence\\": \\"Inclusive of all Taxes\\" },\\n  \\"batchNumber\\": { \\"value\\": \\"PS200\\", \\"confidence\\": 0.95, \\"evidence\\": \\"Batch No. : PS200\\" },\\n  \\"manufacturedOrPackedDate\\": { \\"value\\": \\"July 13, 2017\\", \\"confidence\\": 0.95, \\"evidence\\": \\"Packed on: July 13, 2017\\" },\\n  \\"bestBeforeOrExpiry\\": { \\"value\\": \\"nine months from packaging when kept away from heat & light\\", \\"confidence\\": 0.95, \\"evidence\\": \\"Best before statement\\" },\\n  \\"fssaiLicenseNumber\\": { \\"value\\": \\"1234567871234567\\", \\"confidence\\": 0.85, \\"evidence\\": \\"LIC. No. 1234567871234567\\" },\\n  \\"fssaiStatus\\": { \\"value\\": \\"TEXT_PRESENT_NUMBER_NEEDS_REVIEW\\", \\"confidence\\": 0.90, \\"evidence\\": \\"16-digit license text on right panel\\" },\\n  \\"fssaiTextPresent\\": { \\"value\\": \\"true\\", \\"confidence\\": 0.95, \\"evidence\\": \\"FSSAI LIC. No. visible\\" },\\n  \\"manufacturer\\": { \\"value\\": null, \\"confidence\\": 0.0, \\"evidence\\": \\"Heading 'Manufactured and Packed at:' present but location blank\\" },\\n  \\"phone\\": { \\"value\\": \\"7871234567\\", \\"confidence\\": 0.92, \\"evidence\\": \\"Consumer feedback contact\\" },\\n  \\"ingredients\\": { \\"value\\": \\"Refined Sunflower Oil, Permitted Antioxidants, Vitamin A (750 mcg per 100 g oil), Vitamin D (5 mcg per 100 g oil)\\", \\"confidence\\": 0.94, \\"evidence\\": \\"INGREDIENTS statement\\" },\\n  \\"nutrition\\": [\\n    { \\"nutrient\\": \\"Energy\\", \\"amountPer100g\\": \\"900\\", \\"unit\\": \\"Kcal\\" },\\n    { \\"nutrient\\": \\"Protein\\", \\"amountPer100g\\": \\"0\\", \\"unit\\": \\"g\\" },\\n    { \\"nutrient\\": \\"Carbohydrates\\", \\"amountPer100g\\": \\"0\\", \\"unit\\": \\"g\\" },\\n    { \\"nutrient\\": \\"Sugar\\", \\"amountPer100g\\": \\"0\\", \\"unit\\": \\"g\\" },\\n    { \\"nutrient\\": \\"Fat\\", \\"amountPer100g\\": \\"100\\", \\"unit\\": \\"g\\" }\\n  ]\\n}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        AiLabelExtractionResult result = geminiExtractor.parseGeminiResponse(mockGeminiResponse);

        assertThat(result).isNotNull();
        StructuredAiLabel label = result.label();
        assertThat(label.productName().value()).isEqualTo("REFINED SUNFLOWER OIL");
        assertThat(label.mrp().value()).isEqualTo("100");
        assertThat(label.netQuantity().value()).isEqualTo("1 Litre");
        assertThat(label.batchNumber().value()).isEqualTo("PS200");
        assertThat(label.manufacturedOrPackedDate().value()).isEqualTo("July 13, 2017");
        assertThat(label.bestBeforeOrExpiry().value()).contains("nine months");
        assertThat(label.phone().value()).isEqualTo("7871234567");
        assertThat(label.manufacturer().value()).isNull();
        assertThat(label.fssaiLicenseNumber().value()).isEqualTo("1234567871234567");
        assertThat(label.fssaiStatus().value()).isEqualTo("TEXT_PRESENT_NUMBER_NEEDS_REVIEW");
        assertThat(label.nutrition()).hasSize(5);
    }

    @Test
    @DisplayName("Phase 11: Sunflower Oil Merge prevents Tesseract OCR from corrupting Product Name or MRP")
    void testSunflowerOil_mergeProtection() {
        StructuredAiLabel aiLabel = new StructuredAiLabel(
                0.95,
                FieldExtraction.of("REFINED SUNFLOWER OIL", 0.98, "large central front-panel text"),
                FieldExtraction.of("SUNFLOWER", 0.90, "Brand mark"),
                FieldExtraction.of("1 Litre", 0.96, "NET QUANTITY AT 30 C : 1 Litre"),
                FieldExtraction.of("100", 0.97, "M.R.P. 2100/-"),
                FieldExtraction.of("true", 0.95, "Inclusive of all Taxes"),
                FieldExtraction.empty(),
                FieldExtraction.of("PS200", 0.95, "Batch No. : PS200"),
                FieldExtraction.of("July 13, 2017", 0.95, "Packed on: July 13, 2017"),
                FieldExtraction.of("nine months from packaging when kept away from heat & light", 0.95, "Best before statement"),
                FieldExtraction.of("1234567871234567", 0.85, "LIC. No. 1234567871234567"),
                FieldExtraction.of("TEXT_PRESENT_NUMBER_NEEDS_REVIEW", 0.90, "16-digit license number"),
                FieldExtraction.empty(), // Manufacturer is null
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.of("7871234567", 0.92, "Helpline phone"),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.of("Refined Sunflower Oil, Permitted Antioxidants", 0.95, "Ingredients"),
                FieldExtraction.empty(),
                FieldExtraction.of("vegetarian", 0.90, "Green symbol"),
                FieldExtraction.empty(),
                List.of(),
                List.of()
        );

        AiLabelExtractionResult aiResult = AiLabelExtractionResult.success(0.95, "Gemini Vision", "gemini-3.6-flash", aiLabel);

        // Simulated noisy Tesseract OCR output (as seen in baseline OCR runs)
        StructuredLabelData noisyOcr = new StructuredLabelData(
                "Manufactured And Packed At", // Corrupt product name from OCR column header
                null,
                "1 Litre",
                "2100", // Corrupt price from ₹ glyph misread
                null,
                null,
                "For consumer feedback, please contact us REFINED SUNFLOWER 0 AG Fortified with q i Packed on: July 13, 2017", // Corrupt manufacturer blob
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Raw OCR text",
                "PS200",
                "NOT_DETECTED"
        );

        ExtractionMergeService.MergedResult merged = mergeService.merge(aiResult, noisyOcr, "Raw OCR text", false);
        StructuredLabelData finalData = merged.labelData();

        // 1. Product Name must remain REFINED SUNFLOWER OIL
        assertThat(finalData.productName()).isEqualTo("REFINED SUNFLOWER OIL");
        assertThat(finalData.productName()).isNotEqualTo("Manufactured And Packed At");

        // 2. MRP must remain 100, not 2100
        assertThat(finalData.mrp()).isEqualTo("100");
        assertThat(finalData.mrp()).isNotEqualTo("2100");

        // 3. Net Quantity must contain 1 Litre
        assertThat(finalData.netQuantity()).contains("1 Litre");

        // 4. Batch Number must be PS200
        assertThat(finalData.batchNumber()).isEqualTo("PS200");

        // 5. Packed Date must be July 13, 2017
        assertThat(finalData.manufactureOrPackingDate()).contains("July 13, 2017");

        // 6. Best Before must contain nine months
        assertThat(finalData.bestBeforeOrExpiry()).contains("nine months");

        // 7. Phone must be 7871234567
        assertThat(finalData.customerCarePhone()).isEqualTo("7871234567");

        // 8. Manufacturer must NOT be "Manufactured and" or the noisy OCR paragraph
        assertThat(finalData.manufacturerName()).isNull();

        // 9. FSSAI status must NOT be NOT_DETECTED
        assertThat(finalData.fssaiStatus()).isEqualTo("TEXT_PRESENT_NUMBER_NEEDS_REVIEW");
        assertThat(finalData.fssaiLicenseNumber()).isEqualTo("1234567871234567");
    }
}
