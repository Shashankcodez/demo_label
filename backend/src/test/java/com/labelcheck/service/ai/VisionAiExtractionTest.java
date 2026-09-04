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

    @Test
    @DisplayName("7. GeminiVisionLabelExtractor parses generateContent candidate response with markdown codeblock")
    void testGeminiParser_parsesGenerateContentResponse() {
        GeminiVisionLabelExtractor geminiExtractor = new GeminiVisionLabelExtractor(aiProperties, objectMapper);
        String geminiJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "```json\\n{\\n  \\"overallConfidence\\": 0.96,\\n  \\"productName\\": { \\"value\\": \\"REFINED SUNFLOWER OIL\\", \\"confidence\\": 0.98, \\"evidence\\": \\"Front center label\\" },\\n  \\"brand\\": { \\"value\\": \\"SUNFLOWER\\", \\"confidence\\": 0.90, \\"evidence\\": \\"Brand heading\\" },\\n  \\"netQuantity\\": { \\"value\\": \\"1 Litre\\", \\"confidence\\": 0.96, \\"evidence\\": \\"NET QUANTITY AT 30 C : 1 Litre\\" },\\n  \\"mrp\\": { \\"value\\": \\"₹100/-\\", \\"confidence\\": 0.97, \\"evidence\\": \\"M.R.P. : ₹100/- (Inclusive of all Taxes)\\" },\\n  \\"mrpIncludesTaxes\\": { \\"value\\": \\"true\\", \\"confidence\\": 0.95, \\"evidence\\": \\"Inclusive of all Taxes\\" },\\n  \\"batchNumber\\": { \\"value\\": \\"PS200\\", \\"confidence\\": 0.95, \\"evidence\\": \\"Batch No. : PS200\\" },\\n  \\"manufacturedOrPackedDate\\": { \\"value\\": \\"July 13, 2017\\", \\"confidence\\": 0.95, \\"evidence\\": \\"Packed on : July 13, 2017\\" },\\n  \\"bestBeforeOrExpiry\\": { \\"value\\": \\"nine months from packaging\\", \\"confidence\\": 0.95, \\"evidence\\": \\"Best before nine months from packaging\\" },\\n  \\"fssaiLicenseNumber\\": { \\"value\\": \\"12345678912345\\", \\"confidence\\": 0.94, \\"evidence\\": \\"LIC. No. 12345678912345\\" },\\n  \\"fssaiStatus\\": { \\"value\\": \\"NUMBER_DETECTED\\", \\"confidence\\": 0.95, \\"evidence\\": \\"14 digit FSSAI lic\\" },\\n  \\"manufacturer\\": { \\"value\\": null, \\"confidence\\": 0.0, \\"evidence\\": null },\\n  \\"ingredients\\": { \\"value\\": \\"Refined Sunflower Oil, Permitted Antioxidants, Vitamin A, Vitamin D\\", \\"confidence\\": 0.92, \\"evidence\\": \\"INGREDIENTS block\\" }\\n}\\n```"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        AiLabelExtractionResult result = geminiExtractor.parseGeminiResponse(geminiJson);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(AiExtractionStatus.AI_SUCCESS);
        assertThat(result.extractionSource()).isEqualTo("Gemini Vision");
        assertThat(result.label().productName().value()).isEqualTo("REFINED SUNFLOWER OIL");
        assertThat(result.label().mrp().value()).isEqualTo("100");
        assertThat(result.label().batchNumber().value()).isEqualTo("PS200");
        assertThat(result.label().netQuantity().value()).isEqualTo("1 Litre");
        assertThat(result.label().manufacturedOrPackedDate().value()).isEqualTo("July 13, 2017");
        assertThat(result.label().bestBeforeOrExpiry().value()).contains("nine months");
        // Ensure manufacturer without value is null rather than "Manufactured and"
        assertThat(result.label().manufacturer().value()).isNull();
    }

    @Test
    @DisplayName("8. PrimaryFallbackVisionLabelExtractor disables Groq fallback during Gemini primary scan path")
    void testPrimaryFallback_doesNotCallGroqWhenGeminiFails(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path testImg = tempDir.resolve("test.png");
        java.nio.file.Files.write(testImg, new byte[] { (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A });

        GeminiVisionLabelExtractor mockGemini = org.mockito.Mockito.mock(GeminiVisionLabelExtractor.class);
        GroqVisionLabelExtractor mockGroq = org.mockito.Mockito.mock(GroqVisionLabelExtractor.class);

        org.mockito.Mockito.when(mockGemini.isEnabled()).thenReturn(true);
        org.mockito.Mockito.when(mockGemini.extract(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(AiLabelExtractionResult.failed(AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK, "gemini-3.6-flash", "Gemini 429 rate limit"));

        AiProperties testProps = new AiProperties();
        testProps.setProvider("gemini");

        PrimaryFallbackVisionLabelExtractor orchestrator = new PrimaryFallbackVisionLabelExtractor(
                mockGemini, mockGroq, testProps
        );

        AiLabelExtractionResult result = orchestrator.extract(testImg, "image/png");

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK);
        // Groq must NOT be invoked when provider is gemini
        org.mockito.Mockito.verify(mockGemini, org.mockito.Mockito.times(1)).extract(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(mockGroq, org.mockito.Mockito.never()).extract(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("9. PrimaryFallbackVisionLabelExtractor executes Groq when provider is explicitly groq")
    void testPrimaryFallback_executesGroqWhenProviderIsGroq(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path testImg = tempDir.resolve("test.png");
        java.nio.file.Files.write(testImg, new byte[] { (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A });

        GeminiVisionLabelExtractor mockGemini = org.mockito.Mockito.mock(GeminiVisionLabelExtractor.class);
        GroqVisionLabelExtractor mockGroq = org.mockito.Mockito.mock(GroqVisionLabelExtractor.class);

        org.mockito.Mockito.when(mockGroq.isEnabled()).thenReturn(true);
        StructuredAiLabel groqLabel = new StructuredAiLabel(
                0.92,
                FieldExtraction.of("Groq Product", 0.95, "Title"),
                FieldExtraction.empty(),
                FieldExtraction.of("500 g", 0.95, "500g"),
                FieldExtraction.of("100", 0.95, "MRP 100"),
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
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                List.of(),
                List.of()
        );
        org.mockito.Mockito.when(mockGroq.extract(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(AiLabelExtractionResult.success(0.92, "Groq Vision", "qwen/qwen3.6-27b", groqLabel));

        AiProperties testProps = new AiProperties();
        testProps.setProvider("groq");

        PrimaryFallbackVisionLabelExtractor orchestrator = new PrimaryFallbackVisionLabelExtractor(
                mockGemini, mockGroq, testProps
        );

        AiLabelExtractionResult result = orchestrator.extract(testImg, "image/png");

        assertThat(result).isNotNull();
        assertThat(result.extractionSource()).isEqualTo("Groq Vision");
        assertThat(result.label().productName().value()).isEqualTo("Groq Product");
        org.mockito.Mockito.verify(mockGroq, org.mockito.Mockito.times(1)).extract(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("10. MergeService rejects heading fragments as productName and manufacturerName")
    void testMerge_rejectsHeadingFragments() {
        StructuredAiLabel aiLabel = new StructuredAiLabel(
                0.95,
                FieldExtraction.of("Manufactured and Packed at", 0.85, "Header text"),
                FieldExtraction.empty(),
                FieldExtraction.of("1 Litre", 0.95, "Quantity"),
                FieldExtraction.of("100", 0.95, "Price"),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.of("PS200", 0.90, "Batch"),
                FieldExtraction.of("July 13, 2017", 0.95, "Packed on"),
                FieldExtraction.of("nine months from packaging", 0.95, "Best before"),
                FieldExtraction.of("1234567871234567", 0.85, "Lic No"),
                FieldExtraction.of("TEXT_PRESENT_NUMBER_NEEDS_REVIEW", 0.85, "Review needed"),
                FieldExtraction.of("Manufactured and", 0.80, "Heading"),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.of("7871234567", 0.90, "Phone"),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                List.of(),
                List.of()
        );

        AiLabelExtractionResult aiResult = AiLabelExtractionResult.success(0.95, "Gemini Vision", "gemini-3.6-flash", aiLabel);

        StructuredLabelData ocrData = new StructuredLabelData(
                "Manufactured And Packed At", null, "1 Litre", "2100", null, null,
                "Manufactured and", null, null, null, null, null, null, null, null, null, null,
                "raw text", null, "NOT_DETECTED"
        );

        ExtractionMergeService.MergedResult merged = mergeService.merge(aiResult, ocrData, "raw text", false);

        // Assert productName is NOT the heading fragment
        assertThat(merged.labelData().productName()).isNull();
        // Assert manufacturerName is NOT the heading fragment
        assertThat(merged.labelData().manufacturerName()).isNull();
        // Assert MRP preferred visual 100 over OCR 2100
        assertThat(merged.labelData().mrp()).isEqualTo("100");
        // Assert FSSAI status
        assertThat(merged.labelData().fssaiStatus()).isEqualTo("TEXT_PRESENT_NUMBER_NEEDS_REVIEW");
    }
}
