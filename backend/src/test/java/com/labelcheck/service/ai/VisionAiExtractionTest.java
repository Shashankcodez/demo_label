package com.labelcheck.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelcheck.config.AiProperties;
import com.labelcheck.dto.StructuredLabelData;
import com.labelcheck.dto.ai.AiExtractionStatus;
import com.labelcheck.dto.ai.AiLabelExtractionResult;
import com.labelcheck.dto.ai.FieldExtraction;
import com.labelcheck.dto.ai.StructuredAiLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VisionAiExtractionTest {

    private ObjectMapper objectMapper;
    private AiProperties aiProperties;
    private ExtractionMergeService mergeService;
    private GroqVisionLabelExtractor groqExtractor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        aiProperties = new AiProperties();
        aiProperties.setModel("qwen/qwen3.6-27b");
        aiProperties.setProvider("groq");
        mergeService = new ExtractionMergeService();
        groqExtractor = new GroqVisionLabelExtractor(aiProperties, objectMapper);
    }

    @Test
    @DisplayName("1. StructuredAiLabel correctly counts detected fields and calculates confidence")
    void testStructuredAiLabel_metrics() {
        StructuredAiLabel label = new StructuredAiLabel(
                0.95,
                FieldExtraction.of("Apple Slice", 0.98, "Top title"),
                FieldExtraction.of("Fresh Harvest", 0.92, "Brand mark"),
                FieldExtraction.of("250 g", 0.95, "Net Weight declaration"),
                FieldExtraction.of("150", 0.97, "MRP : ~150"),
                FieldExtraction.of("true", 0.95, "Including all taxes text"),
                FieldExtraction.empty(), // USP missing
                FieldExtraction.of("20250509", 0.94, "Batch declaration"),
                FieldExtraction.of("09/05/2025", 0.95, "Packed On declaration"),
                FieldExtraction.of("14/05/2025", 0.95, "Best Before declaration"),
                FieldExtraction.of("Applied For", 0.92, "SSA Lic No : Applied For"),
                FieldExtraction.of("APPLIED_FOR", 0.95, "Status declared"),
                FieldExtraction.of("D MARKET", 0.92, "Packed By"),
                FieldExtraction.of("D MARKET", 0.92, "Packed By"),
                FieldExtraction.of("SMART ONLINE STORE", 0.90, "Marketed By"),
                FieldExtraction.empty(), // Importer null
                FieldExtraction.of("Virar East, Maharashtra", 0.95, "Premises address"),
                FieldExtraction.of("India", 0.90, "Country declared"),
                FieldExtraction.of("+91 8888 720 520", 0.96, "Phone contact"),
                FieldExtraction.of("admin@store.co.in", 0.97, "Support email"),
                FieldExtraction.of("+91 8888 720 520", 0.96, "Consumer helpline"),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.of("vegetarian", 0.90, "Green dot"),
                FieldExtraction.empty(),
                List.of(),
                List.of()
        );

        assertThat(label.countDetectedFields()).isGreaterThanOrEqualTo(11);
        assertThat(label.calculateAverageConfidence()).isGreaterThan(0.90);
    }

    @Test
    @DisplayName("2. GroqVisionLabelExtractor parses Groq chat completion response with usage stats and markdown fences")
    void testGroqParser_parsesChatCompletionResponse() {
        String groqJson = """
                {
                  "id": "chatcmpl-test-groq-123",
                  "object": "chat.completion",
                  "created": 1725420000,
                  "model": "qwen/qwen3.6-27b",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "```json\\n{\\n  \\"overallConfidence\\": 0.95,\\n  \\"productName\\": { \\"value\\": \\"Apple Slice\\", \\"confidence\\": 0.98, \\"evidence\\": \\"Front panel heading\\" },\\n  \\"brand\\": { \\"value\\": \\"Fresh Harvest\\", \\"confidence\\": 0.90, \\"evidence\\": \\"Logo above product\\" },\\n  \\"netQuantity\\": { \\"value\\": \\"250 Gm\\", \\"confidence\\": 0.96, \\"evidence\\": \\"Right box Net Weight : 250Gm\\" },\\n  \\"mrp\\": { \\"value\\": \\"₹150/-\\", \\"confidence\\": 0.97, \\"evidence\\": \\"MRP : 150 incl taxes\\" },\\n  \\"mrpIncludesTaxes\\": { \\"value\\": \\"true\\", \\"confidence\\": 0.95, \\"evidence\\": \\"incl of all taxes\\" },\\n  \\"batchNumber\\": { \\"value\\": \\"20250509\\", \\"confidence\\": 0.92, \\"evidence\\": \\"Batch text\\" },\\n  \\"manufacturedOrPackedDate\\": { \\"value\\": \\"09/05/2025\\", \\"confidence\\": 0.95, \\"evidence\\": \\"Packed on\\" },\\n  \\"bestBeforeOrExpiry\\": { \\"value\\": \\"14/05/2025\\", \\"confidence\\": 0.95, \\"evidence\\": \\"Best before\\" },\\n  \\"fssaiLicenseNumber\\": { \\"value\\": \\"Applied For\\", \\"confidence\\": 0.90, \\"evidence\\": \\"SSA Lic No: Applied For\\" },\\n  \\"manufacturer\\": { \\"value\\": \\"D MARKET\\", \\"confidence\\": 0.92, \\"evidence\\": \\"Packed By : D MARKET\\" },\\n  \\"address\\": { \\"value\\": \\"SHOP NO 5, VIRAR EAST, MAHARASHTRA\\", \\"confidence\\": 0.93, \\"evidence\\": \\"Address block\\" },\\n  \\"phone\\": { \\"value\\": \\"+91 8888 720 520\\", \\"confidence\\": 0.95, \\"evidence\\": \\"Customer care line\\" },\\n  \\"email\\": { \\"value\\": \\"admin@smartonlinestore.co.in\\", \\"confidence\\": 0.96, \\"evidence\\": \\"Email id\\" }\\n}\\n```"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 1200,
                    "completion_tokens": 350,
                    "total_tokens": 1550
                  }
                }
                """;

        AiLabelExtractionResult result = groqExtractor.parseSuccessfulResponse(groqJson);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(AiExtractionStatus.AI_SUCCESS);
        assertThat(result.modelName()).isEqualTo("qwen/qwen3.6-27b");
        assertThat(result.extractionSource()).isEqualTo("Groq Vision");
        assertThat(result.label().productName().value()).isEqualTo("Apple Slice");
        assertThat(result.label().mrp().value()).isEqualTo("150");
        assertThat(result.label().netQuantity().value()).isEqualTo("250 Gm");
        assertThat(result.label().fssaiLicenseNumber().value()).isEqualTo("Applied For");
        assertThat(result.label().phone().value()).isEqualTo("+91 8888 720 520");
        assertThat(result.label().email().value()).isEqualTo("admin@smartonlinestore.co.in");
        assertThat(result.label().countDetectedFields()).isGreaterThanOrEqualTo(10);
    }


    @Test
    @DisplayName("3. Partial extraction with only Net Qty and MRP is valid and accepted")
    void testPartialExtraction_isValid() {
        StructuredAiLabel partialLabel = new StructuredAiLabel(
                0.88,
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.of("500 g", 0.95, "Net Wt: 500g"),
                FieldExtraction.of("120", 0.94, "MRP 120"),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.of("ABC Foods Ltd", 0.90, "Manufactured by"),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                List.of(),
                List.of()
        );

        AiLabelExtractionResult aiResult = AiLabelExtractionResult.partial(0.88, "Groq Vision", "qwen/qwen3.6-27b", partialLabel);
        ExtractionMergeService.MergedResult merged = mergeService.merge(aiResult, null, "Sample raw OCR text", false);

        assertThat(merged.extractionStatus()).isEqualTo("AI_PARTIAL");
        assertThat(merged.extractionSource()).isEqualTo("Groq Vision");
        assertThat(merged.labelData().netQuantity()).isEqualTo("500 g");
        assertThat(merged.labelData().mrp()).isEqualTo("120");
        assertThat(merged.labelData().manufacturerName()).isEqualTo("ABC Foods Ltd");
        assertThat(merged.labelData().productName()).isNull();
        assertThat(merged.labelData().rawOcrText()).isEqualTo("Sample raw OCR text");
    }

    @Test
    @DisplayName("4. Merge prioritizes Groq Vision over conflicting OCR (e.g. 150 vs 2150)")
    void testMerge_prioritizesAiOverOcrConflict() {
        StructuredAiLabel aiLabel = new StructuredAiLabel(
                0.96,
                FieldExtraction.of("Apple Slice", 0.98, "Headline"),
                FieldExtraction.empty(),
                FieldExtraction.of("250 g", 0.95, "Label"),
                FieldExtraction.of("150", 0.97, "Visible MRP 150"), // Groq sees 150
                FieldExtraction.of("true", 0.95, "Tax clause"),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.of("09/05/2025", 0.96, "Packed On"),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                List.of(),
                List.of()
        );

        // OCR erroneously read MRP as 2150 (common OCR symbol misinterpretation)
        StructuredLabelData ocrData = new StructuredLabelData(
                "Apple Slice", null, "250 g", "2150", true, null,
                null, null, null, null, null, "Protein 0.5 Gms", null, null, null, null, null, "raw text"
        );

        AiLabelExtractionResult aiResult = AiLabelExtractionResult.success(0.96, "Groq Vision", "qwen/qwen3.6-27b", aiLabel);
        ExtractionMergeService.MergedResult merged = mergeService.merge(aiResult, ocrData, "raw text", false);

        // Assert Groq's 150 is chosen over OCR's 2150
        assertThat(merged.labelData().mrp()).isEqualTo("150");
        assertThat(merged.labelData().mrp()).isNotEqualTo("2150");
        // Assert Groq's date 09/05/2025 is chosen over OCR's mistaken "Protein 0.5 Gms"
        assertThat(merged.labelData().manufactureOrPackingDate()).isEqualTo("09/05/2025");
        assertThat(merged.labelData().manufactureOrPackingDate()).doesNotContain("Protein");
    }

    @Test
    @DisplayName("5. Graceful fallback when Groq fails due to network/rate-limit")
    void testMerge_fallbackWhenAiFails() {
        AiLabelExtractionResult aiFailed = AiLabelExtractionResult.failed(
                AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                "qwen/qwen3.6-27b",
                "Groq rate limit reached. Using local OCR fallback."
        );

        StructuredLabelData ocrData = new StructuredLabelData(
                "Haldiram Bhujia", "Haldiram", "200 g", "50", true, "Rs 0.25/g",
                "Haldiram Snacks", "Bikaner, Rajasthan", null, null, "India",
                "01/01/2026", "01/06/2026", "10012011000123", "+91 11 22334455",
                "care@haldirams.com", null, "Haldiram raw text"
        );

        ExtractionMergeService.MergedResult merged = mergeService.merge(aiFailed, ocrData, "Haldiram raw text", false);

        assertThat(merged.extractionSource()).isEqualTo("TESSERACT_FALLBACK");
        assertThat(merged.extractionStatus()).isEqualTo("AI_FAILED_TESSERACT_FALLBACK");
        assertThat(merged.labelData().productName()).isEqualTo("Haldiram Bhujia");
        assertThat(merged.labelData().netQuantity()).isEqualTo("200 g");
        assertThat(merged.labelData().rawOcrText()).isEqualTo("Haldiram raw text");
    }

    @Test
    @DisplayName("6. Low quality suspect image with 0 fields yields IMAGE_QUALITY_LOW")
    void testMerge_lowQualityImage_yieldsImageQualityLow() {
        AiLabelExtractionResult aiFailed = AiLabelExtractionResult.failed(
                AiExtractionStatus.IMAGE_QUALITY_LOW,
                "qwen/qwen3.6-27b",
                "0 fields detected from blurry image"
        );

        StructuredLabelData emptyOcr = new StructuredLabelData(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                "", null, "NOT_DETECTED"
        );

        ExtractionMergeService.MergedResult merged = mergeService.merge(aiFailed, emptyOcr, "", true);


        assertThat(merged.extractionStatus()).isEqualTo("IMAGE_QUALITY_LOW");
        assertThat(merged.labelData().countDetectedFields()).isEqualTo(0);
    }
}
