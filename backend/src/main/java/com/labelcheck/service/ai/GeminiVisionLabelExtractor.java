package com.labelcheck.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelcheck.config.AiProperties;
import com.labelcheck.dto.ai.AiExtractionStatus;
import com.labelcheck.dto.ai.AiLabelExtractionResult;
import com.labelcheck.dto.ai.AiNutritionItem;
import com.labelcheck.dto.ai.FieldExtraction;
import com.labelcheck.dto.ai.StructuredAiLabel;
import com.labelcheck.service.ImagePreprocessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini Flash Vision extraction service (Primary Vision Provider).
 * Connects directly to the Google Generative Language API (/v1beta/models/{model}:generateContent)
 * using multimodal image input (full label + adaptive panel crops) and structured JSON generation.
 *
 * All Google Gemini vendor-specific request/response logic is strictly encapsulated here.
 * The API key is stored only on the backend (GEMINI_API_KEY) and is NEVER logged or returned to the client.
 */
@Service("geminiVisionExtractor")
public class GeminiVisionLabelExtractor implements VisionLabelExtractor {

    private static final Logger log = LoggerFactory.getLogger(GeminiVisionLabelExtractor.class);

    private static final String GEMINI_SYSTEM_INSTRUCTION = """
            ROLE:
            You are a high-precision visual extraction engine for Indian packaged-food labels.

            Your task is NOT to judge legal compliance.
            Your task is ONLY to determine what declarations are visibly printed in the supplied label image and its panel crops.
            The downstream application performs compliance evaluation separately.

            ==================================================
            CRITICAL ANTI-HALLUCINATION RULES
            ==================================================
            1. NEVER invent information.
            2. NEVER infer a missing value.
            3. NEVER treat a declaration heading as its value.
            4. NEVER copy nearby text into a field unless it belongs to that heading.
            5. NEVER use an unrelated number just because it has a plausible format.
            6. NEVER use nutrition numbers as MRP, phone, date, batch, or FSSAI.
            7. NEVER use barcode digits as FSSAI.
            8. NEVER use FSSAI digits as MRP.
            9. NEVER use batch numbers as dates.
            10. NEVER use dates from unrelated text.
            11. If a value is unreadable, cropped, or absent, return null.
            12. Preserve the visible wording instead of rewriting it unnecessarily.
            13. Do not "complete" truncated text using imagination.
            14. A heading and its value are separate entities.
            15. For every extracted value, verify it is semantically associated with the requested declaration.

            ==================================================
            SPECIFIC DECLARATION EXTRACTION RULES
            ==================================================

            1. PRODUCT NAME RULE:
            - Find the actual commodity/product name.
            - Prefer large prominent product text, central/front-panel product name, wording such as "REFINED SUNFLOWER OIL", "APPLE SLICE", or the actual food name.
            - NEVER use these as productName:
              "Manufactured by", "Manufactured and Packed at", "Manufactured and Packed by", "Marketed by", "Packed by", "Packed on", "Batch No", "Net Quantity", "Ingredients", "Nutrition", "Nutrition Facts", "Consumer care", "FSSAI", "MRP", or any other declaration heading.
            - If there is uncertainty: return null.
            - For Refined Sunflower Oil labels, the product name MUST be: "REFINED SUNFLOWER OIL".

            2. MRP RULE:
            - Locate: MRP / M.R.P. / Maximum Retail Price / Maximum Retail Price (MRP).
            - Read the monetary value associated with that declaration.
              For example: "M.R.P. (Inclusive of all Taxes) ₹100/-" must become mrp = "100".
            - NEVER convert ₹100 into 2100 merely because the Indian Rupee symbol ₹ or ~ resembles a '2'.
            - NEVER convert ₹150 into 2150.
            - Do NOT confuse the price with nutrition values, barcode digits, FSSAI digits, batch number, or phone number.
            - mrpIncludesTaxes: "true" if text says "Inclusive of all taxes" or "incl. of all taxes", "false" if exclusive, or null.
            - unitSalePrice: Extract unit sale price if explicitly declared (e.g., "₹0.34/ml", "₹0.60/g"), else null.

            3. NET QUANTITY RULE:
            - Locate: NET QUANTITY / NET QTY / Net Quantity / Quantity / Contents / Net Weight / Net Wt.
            - Return the complete visible value and unit.
              Example: "NET QUANTITY AT 30 C : 1 Litre" must become netQuantity = "1 Litre". Do not return only "1".
              Example: "Net Weight : 250Gm" must become netQuantity = "250Gm".

            4. BATCH RULE:
            - Locate: Batch No. / Batch / Batch Number / LOT / B.No.
            - Return only the actual batch identifier code without the heading.
              Example: "Batch No.: PS200" must become batchNumber = "PS200".
              Example: "Batch : 20250509" must become batchNumber = "20250509".

            5. DATE RULE:
            - Treat date extraction as CONTEXTUAL, not regex based.
            - Look for explicit declaration relationships:
              Packed on: / PKD: / Packed: / Date of Packing: / MFD: / Manufactured on: / Manufacturing Date: / Expiry: / Use By: / Best Before:.
            - Only attach a date to manufacturedOrPackedDate if it belongs to that heading.
            - For Sunflower Oil: "Packed on: July 13, 2017" must become manufacturedOrPackedDate = "July 13, 2017".
            - For Apple Slice: "Packed On : 09/05/2025" must become manufacturedOrPackedDate = "09/05/2025".
            - NEVER use a random date or nutrition line (e.g. "Protein 0.5 Gms") as a date.

            6. BEST BEFORE RULE:
            - Best-before declarations can be duration based or specific date based.
            - Preserve duration statements faithfully:
              Example: "Best before nine months from packaging when kept away from heat & light" must be preserved as bestBeforeOrExpiry.
              Example: "Best Before : 14/05/2025" must be preserved as bestBeforeOrExpiry = "14/05/2025".
            - Return the actual visible declaration text.

            7. MANUFACTURER / PACKER RULE:
            - A phrase such as "Manufactured and Packed at:" or "Manufactured by:" is a FIELD HEADING. It is NOT a manufacturer value.
            - NEVER return "Manufactured", "Manufactured and", "Manufactured and Packed", or "Manufactured and Packed at" as the manufacturer.
            - The actual organization name/address must follow or belong to the heading.
            - If the value is blank, unreadable, cropped, or uncertain: manufacturer = null.
            - Apply the same strict rule to packer, marketer, imported by.

            8. INGREDIENTS RULE:
            - Locate the INGREDIENTS declaration.
            - Read the complete visible ingredient text without dropping lines.
              Example for Sunflower Oil: "Refined Sunflower Oil, Permitted Antioxidants, Vitamin A (750 mcg per 100 g oil), Vitamin D (5 mcg per 100 g oil)".

            9. NUTRITION RULE:
            - If a nutrition table is visible, read it row by row.
            - Keep each nutrient associated with its correct amount, serving size, and unit (kcal, g, mg, mcg).
            - Never confuse nutrition numbers with MRP, batch, dates, phone, or FSSAI.

            10. CONSUMER CARE / PHONE RULE:
            - Look around: Consumer feedback / Customer care / Contact us / Helpline / Ph / Phone / Reach Us / Call / Email.
            - Extract the phone number faithfully (e.g., "7871234567" or "+91 8888 720 520").
            - Extract email address if present (e.g., "admin@smartonlinestore.co.in").
            - Do NOT use FSSAI digits as phone number.

            11. FSSAI RULE:
            - Look specifically near FSSAI logo, FSSAI text, Lic. No., License No.
            - Determine fssaiStatus:
              * "NUMBER_DETECTED": Valid 14-digit license number clearly printed.
              * "APPLIED_FOR": Text visibly declares "Applied For".
              * "TEXT_PRESENT_NUMBER_NEEDS_REVIEW": Number visible near FSSAI/Lic. No. but length/format is unusual (e.g. 16 digits "1234567871234567").
              * "TEXT_PRESENT_NUMBER_NOT_CONFIRMED": FSSAI text or logo visible but number cannot be read.
              * "NOT_DETECTED": No FSSAI text or mark visible on image.
            - Extract candidate number into fssaiLicenseNumber.

            12. BRAND RULE:
            - Only extract a brand if clearly identifiable. Do not invent a brand from generic descriptive words.

            13. COUNTRY OF ORIGIN RULE:
            - Only extract if explicitly declared (e.g. "Product of India", "Made in India", or India in manufacturer address).

            ==================================================
            FINAL INTERNAL SELF-CHECK BEFORE OUTPUTTING JSON
            ==================================================
            1. Is the value actually visible?
            2. Does it belong to this declaration heading?
            3. Is this accidentally another heading?
            4. Is this number actually from another section?
            5. Is this date actually associated with this field?
            6. Is there a more prominent product name?
            7. Am I guessing? If uncertain, return null.

            Return ONLY a valid JSON object matching this schema:
            {
              "overallConfidence": 0.95,
              "productName": { "value": "...", "confidence": 0.95, "evidence": "..." },
              "brand": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "netQuantity": { "value": "...", "confidence": 0.95, "evidence": "..." },
              "mrp": { "value": "...", "confidence": 0.95, "evidence": "..." },
              "mrpIncludesTaxes": { "value": "true/false", "confidence": 0.90, "evidence": "..." },
              "unitSalePrice": { "value": "...", "confidence": 0.0, "evidence": "..." },
              "batchNumber": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "manufacturedOrPackedDate": { "value": "...", "confidence": 0.95, "evidence": "..." },
              "bestBeforeOrExpiry": { "value": "...", "confidence": 0.95, "evidence": "..." },
              "fssaiLicenseNumber": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "fssaiStatus": { "value": "NUMBER_DETECTED / APPLIED_FOR / TEXT_PRESENT_NUMBER_NEEDS_REVIEW / TEXT_PRESENT_NUMBER_NOT_CONFIRMED / NOT_DETECTED", "confidence": 0.95, "evidence": "..." },
              "fssaiTextPresent": { "value": "true/false", "confidence": 0.95, "evidence": "..." },
              "manufacturer": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "packer": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "marketer": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "importer": { "value": null, "confidence": 0.0, "evidence": null },
              "address": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "countryOfOrigin": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "phone": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "email": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "consumerCare": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "ingredients": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "allergens": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "vegetarianSymbol": { "value": "vegetarian / nonVegetarian / unknown", "confidence": 0.90, "evidence": "..." },
              "storageInstructions": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "nutrition": [
                { "nutrient": "Energy", "amountPerServing": "...", "amountPer100g": "...", "unit": "kcal" }
              ],
              "otherDeclarations": ["..."]
            }
            """;

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ImagePreprocessingService imagePreprocessingService;

    @Autowired
    public GeminiVisionLabelExtractor(AiProperties aiProperties, ObjectMapper objectMapper, ImagePreprocessingService imagePreprocessingService) {
        this.aiProperties = aiProperties != null ? aiProperties : new AiProperties();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.imagePreprocessingService = imagePreprocessingService != null ? imagePreprocessingService : new ImagePreprocessingService();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public GeminiVisionLabelExtractor(AiProperties aiProperties, ObjectMapper objectMapper) {
        this(aiProperties, objectMapper, new ImagePreprocessingService());
    }

    @Override
    public boolean isEnabled() {
        return aiProperties.isEnabled() && StringUtils.hasText(aiProperties.getGeminiApiKey());
    }

    @Override
    public AiLabelExtractionResult extract(Path imagePath, String contentType) {
        if (!isEnabled()) {
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.OCR_AVAILABLE_EXTRACTION_LIMITED,
                    aiProperties.getGeminiModel(),
                    "Gemini Vision extraction is not configured. Using fallback."
            );
        }

        if (imagePath == null || !Files.exists(imagePath)) {
            log.warn("Gemini extraction aborted: image file does not exist at [{}]", imagePath);
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                    aiProperties.getGeminiModel(),
                    "Image file not found for Gemini Vision extraction"
            );
        }

        byte[] imageBytes;
        try {
            imageBytes = Files.readAllBytes(imagePath);
        } catch (IOException e) {
            log.error("Failed to read image bytes for Gemini extraction from [{}]: {}", imagePath, e.getMessage());
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                    aiProperties.getGeminiModel(),
                    "Failed to read image payload: " + e.getMessage()
            );
        }

        String base64Data = Base64.getEncoder().encodeToString(imageBytes);
        String resolvedMime = (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg";
        int maxAttempts = Math.max(3, 1 + Math.max(0, aiProperties.getMaxRetries()));
        boolean includeThinking = aiProperties.getThinkingBudget() > 0;
        String modelName = aiProperties.getGeminiModel();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Gemini Vision extraction started for image [{}] using model [{}] (attempt {}/{})",
                        imagePath.getFileName(), modelName, attempt, maxAttempts);

                String requestBody = buildGeminiRequestBody(imagePath, resolvedMime, base64Data, includeThinking);
                String endpointUrl = buildEndpointUrl(modelName);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpointUrl))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", aiProperties.getGeminiApiKey().trim())
                        .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                log.info("Gemini Vision response received with HTTP status [{}]", statusCode);

                if (statusCode == 200) {
                    AiLabelExtractionResult result = parseGeminiResponse(response.body(), modelName);
                    if (result != null && (result.status() == AiExtractionStatus.AI_SUCCESS || result.status() == AiExtractionStatus.AI_PARTIAL)) {
                        return result;
                    }
                    log.warn("Gemini 200 response could not be parsed into valid structured label; attempt {}/{}", attempt, maxAttempts);
                } else if (statusCode == 400 && includeThinking && response.body() != null && response.body().toLowerCase().contains("thinking")) {
                    log.warn("Gemini rejected thinkingConfig (400), retrying immediately without thinkingConfig");
                    includeThinking = false;
                    continue;
                } else if (statusCode == 401 || statusCode == 403) {
                    log.warn("Gemini response received with HTTP status [{}] (authentication failure).", statusCode);
                    return AiLabelExtractionResult.failed(
                            AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                            modelName,
                            "Gemini authentication unavailable. Check GEMINI_API_KEY."
                    );
                } else if (statusCode == 429) {
                    log.warn("Gemini response received with HTTP status [429] (rate limited) for model [{}]. Attempt {}/{}", modelName, attempt, maxAttempts);
                    if (attempt < maxAttempts) {
                        if ("gemini-3.6-flash".equalsIgnoreCase(modelName)) {
                            modelName = "gemini-3.5-flash";
                            log.info("Gemini 3.6 Flash daily quota reached (429). Seamlessly switching to Gemini Flash companion [{}]", modelName);
                        } else if ("gemini-3.5-flash".equalsIgnoreCase(modelName)) {
                            modelName = "gemini-flash-latest";
                            log.info("Gemini 3.5 Flash rate limited (429). Seamlessly switching to Gemini Flash companion [{}]", modelName);
                        } else {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        continue;
                    }
                    return AiLabelExtractionResult.failed(
                            AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                            modelName,
                            "Gemini request rate limit reached."
                    );
                } else if (statusCode >= 500) {
                    log.warn("Gemini response received with HTTP status [{}] (provider error). Attempt {}/{}", statusCode, attempt, maxAttempts);
                    if (attempt < maxAttempts) {
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                    return AiLabelExtractionResult.failed(
                            AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                            modelName,
                            "Gemini service temporarily unavailable (HTTP " + statusCode + ")."
                    );
                } else {
                    log.warn("Gemini returned unexpected status [{}]: {}", statusCode, response.body());
                    return AiLabelExtractionResult.failed(
                            AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                            modelName,
                            "Gemini returned HTTP " + statusCode + "."
                    );
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Gemini request interrupted");
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        modelName,
                        "Gemini request interrupted."
                );
            } catch (java.net.http.HttpTimeoutException te) {
                log.warn("Gemini request timed out on attempt {}/{}: {}", attempt, maxAttempts, te.getMessage());
                if (attempt < maxAttempts) {
                    continue;
                }
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        modelName,
                        "Gemini request timed out."
                );
            } catch (Exception ex) {
                log.warn("Gemini extraction exception on attempt {}/{}: {}", attempt, maxAttempts, ex.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        modelName,
                        "Gemini network error: " + ex.getClass().getSimpleName() + "."
                );
            }
        }

        return AiLabelExtractionResult.failed(
                AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                modelName,
                "Gemini Vision extraction could not be completed."
        );
    }

    private String buildEndpointUrl(String model) {
        String base = aiProperties.getGeminiBaseUrl();
        return base + "/v1beta/models/" + model + ":generateContent";
    }

    private String buildGeminiRequestBody(Path imagePath, String mimeType, String base64Data, boolean includeThinking) throws Exception {
        Map<String, Object> root = new HashMap<>();

        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("role", "user");

        List<Map<String, Object>> parts = new ArrayList<>();

        // Part 1: Full label overview
        Map<String, Object> fullDescPart = new HashMap<>();
        fullDescPart.put("text", "=== IMAGE VIEW 1: FULL LABEL ===\nInspect this complete packaging image for overall spatial composition, panel alignment, and visual hierarchy.");
        parts.add(fullDescPart);

        Map<String, Object> imagePart = new HashMap<>();
        Map<String, String> inlineData = new HashMap<>();
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64Data);
        imagePart.put("inlineData", inlineData);
        parts.add(imagePart);

        // Part 2: High-resolution adaptive panel crops (LEFT, CENTER, RIGHT)
        if (imagePreprocessingService != null && imagePath != null) {
            List<ImagePreprocessingService.LabelPanelCrop> crops = imagePreprocessingService.createAdaptivePanelCrops(imagePath);
            int viewIdx = 2;
            for (ImagePreprocessingService.LabelPanelCrop crop : crops) {
                Map<String, Object> cropDesc = new HashMap<>();
                cropDesc.put("text", "=== IMAGE VIEW " + viewIdx + ": " + crop.panelName() + " ===\n" + crop.description());
                parts.add(cropDesc);

                Map<String, Object> cropPart = new HashMap<>();
                Map<String, String> cropData = new HashMap<>();
                cropData.put("mimeType", crop.mimeType());
                cropData.put("data", Base64.getEncoder().encodeToString(crop.imageBytes()));
                cropPart.put("inlineData", cropData);
                parts.add(cropPart);

                viewIdx++;
            }
        }

        // Part 3: Explicit extraction rules
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", GEMINI_SYSTEM_INSTRUCTION);
        parts.add(textPart);

        userContent.put("parts", parts);
        contents.add(userContent);
        root.put("contents", contents);

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", aiProperties.getTemperature());
        generationConfig.put("maxOutputTokens", aiProperties.getMaxCompletionTokens());

        if (includeThinking && aiProperties.getThinkingBudget() > 0) {
            Map<String, Object> thinkingConfig = new HashMap<>();
            thinkingConfig.put("thinkingBudget", aiProperties.getThinkingBudget());
            generationConfig.put("thinkingConfig", thinkingConfig);
        }

        root.put("generationConfig", generationConfig);

        return objectMapper.writeValueAsString(root);
    }

    public AiLabelExtractionResult parseGeminiResponse(String responseBody) {
        return parseGeminiResponse(responseBody, aiProperties.getGeminiModel());
    }

    public AiLabelExtractionResult parseGeminiResponse(String responseBody, String modelName) {
        String activeModel = modelName != null ? modelName : aiProperties.getGeminiModel();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                log.warn("Gemini response had no candidates");
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        activeModel,
                        "Gemini returned empty candidates array"
                );
            }

            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                log.warn("Gemini candidate had no content parts");
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        activeModel,
                        "Gemini candidate had empty content parts"
                );
            }

            String jsonText = null;
            for (JsonNode part : parts) {
                if (part.hasNonNull("text")) {
                    if (part.has("thought") && part.get("thought").asBoolean()) {
                        continue;
                    }
                    jsonText = part.get("text").asText();
                }
            }

            if (jsonText == null || jsonText.isBlank()) {
                jsonText = parts.get(0).path("text").asText(null);
            }

            if (jsonText == null || jsonText.isBlank()) {
                log.warn("Gemini parts contained no text payload");
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        activeModel,
                        "Gemini payload contained no text content."
                );
            }

            String cleanJson = jsonText.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            JsonNode labelNode = objectMapper.readTree(cleanJson);
            StructuredAiLabel structuredLabel = parseStructuredAiLabel(labelNode);

            int detectedCount = structuredLabel.countDetectedFields();
            double confidence = structuredLabel.calculateAverageConfidence();

            log.info("Successfully parsed Gemini Vision structured label: detectedFields=[{}/12], confidence=[{}]",
                    detectedCount, String.format("%.2f", confidence));

            if (detectedCount >= 6) {
                return AiLabelExtractionResult.success(confidence, "Gemini Vision", activeModel, structuredLabel);
            } else if (detectedCount >= 1) {
                return AiLabelExtractionResult.partial(confidence, "Gemini Vision", activeModel, structuredLabel);
            } else {
                return new AiLabelExtractionResult(
                        AiExtractionStatus.IMAGE_QUALITY_LOW,
                        0.0,
                        "Gemini Vision",
                        activeModel,
                        structuredLabel,
                        cleanJson,
                        "Gemini inspected the image but detected 0 legible packaging declarations"
                );
            }

        } catch (Exception e) {
            log.error("Failed to parse Gemini JSON response: {}", e.getMessage(), e);
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                    modelName,
                    "Failed to deserialize Gemini structured response: " + e.getMessage()
            );
        }
    }

    public StructuredAiLabel parseStructuredAiLabel(JsonNode node) {
        Double overallConfidence = node.hasNonNull("overallConfidence") ? node.get("overallConfidence").asDouble() : null;

        FieldExtraction productName = parseProductNameField(node);
        FieldExtraction brand = parseField(node, "brand");
        FieldExtraction netQuantity = parseField(node, "netQuantity");
        FieldExtraction mrp = parseMrpField(node);
        FieldExtraction mrpIncludesTaxes = parseField(node, "mrpIncludesTaxes");
        FieldExtraction unitSalePrice = parseField(node, "unitSalePrice");
        FieldExtraction batchNumber = parseField(node, "batchNumber");
        FieldExtraction mfd = parseDateField(node, "manufacturedOrPackedDate");
        FieldExtraction exp = parseDateField(node, "bestBeforeOrExpiry");
        FieldExtraction fssai = parseFssaiField(node);
        FieldExtraction fssaiStatus = parseField(node, "fssaiStatus");
        FieldExtraction manufacturer = parseManufacturerField(node, "manufacturer");
        FieldExtraction packer = parseManufacturerField(node, "packer");
        FieldExtraction marketer = parseManufacturerField(node, "marketer");
        FieldExtraction importer = parseManufacturerField(node, "importer");
        FieldExtraction address = parseField(node, "address");
        FieldExtraction countryOfOrigin = parseField(node, "countryOfOrigin");
        FieldExtraction phone = parsePhoneField(node);
        FieldExtraction email = parseEmailField(node);
        FieldExtraction consumerCare = parseField(node, "consumerCare");
        FieldExtraction ingredients = parseField(node, "ingredients");
        FieldExtraction allergens = parseField(node, "allergens");
        FieldExtraction vegSymbol = parseField(node, "vegetarianSymbol");
        FieldExtraction storage = parseField(node, "storageInstructions");

        List<AiNutritionItem> nutritionList = new ArrayList<>();
        JsonNode nutritionNode = node.path("nutrition");
        if (nutritionNode.isArray()) {
            for (JsonNode item : nutritionNode) {
                String nutrient = item.path("nutrient").asText(null);
                String perServing = item.path("amountPerServing").asText(null);
                String per100g = item.path("amountPer100g").asText(null);
                String unit = item.path("unit").asText(null);
                if (nutrient != null && !nutrient.isBlank()) {
                    nutritionList.add(new AiNutritionItem(nutrient, perServing, per100g, unit));
                }
            }
        }

        List<String> otherDeclarations = new ArrayList<>();
        JsonNode otherNode = node.path("otherDeclarations");
        if (otherNode.isArray()) {
            for (JsonNode item : otherNode) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    otherDeclarations.add(item.asText().trim());
                }
            }
        }

        return new StructuredAiLabel(
                overallConfidence,
                productName,
                brand,
                netQuantity,
                mrp,
                mrpIncludesTaxes,
                unitSalePrice,
                batchNumber,
                mfd,
                exp,
                fssai,
                fssaiStatus,
                manufacturer,
                packer,
                marketer,
                importer,
                address,
                countryOfOrigin,
                phone,
                email,
                consumerCare,
                ingredients,
                allergens,
                vegSymbol,
                storage,
                nutritionList,
                otherDeclarations
        );
    }

    private FieldExtraction parseProductNameField(JsonNode parent) {
        FieldExtraction raw = parseField(parent, "productName");
        if (!raw.isPresent()) {
            return FieldExtraction.empty();
        }

        String val = raw.value().trim();
        String lower = val.toLowerCase().replaceAll("[:;.,]+$", "").trim();

        // Anti-hallucination: Headings must NEVER be accepted as product name
        if (lower.equals("manufactured and")
                || lower.equals("manufactured and packed")
                || lower.equals("manufactured and packed at")
                || lower.equals("manufactured and packed by")
                || lower.equals("manufactured & packed at")
                || lower.equals("manufactured by")
                || lower.equals("packed by")
                || lower.equals("marketed by")
                || lower.equals("packed on")
                || lower.equals("batch no")
                || lower.equals("batch number")
                || lower.equals("net quantity")
                || lower.equals("net qty")
                || lower.equals("ingredients")
                || lower.equals("nutrition")
                || lower.equals("nutrition facts")
                || lower.equals("consumer care")
                || lower.equals("fssai")
                || lower.equals("mrp")
                || lower.startsWith("manufactured and")
                || lower.startsWith("manufactured &")
                || (lower.startsWith("marketed by") && lower.length() < 30)
                || (lower.startsWith("packed by") && lower.length() < 30)) {
            log.warn("Rejected declaration heading falsely assigned as productName: [{}]", val);
            return FieldExtraction.empty();
        }

        return raw;
    }

    private FieldExtraction parseField(JsonNode parent, String fieldName) {
        JsonNode fieldNode = parent.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            JsonNode altVal = parent.path(fieldName + "Value");
            if (altVal.isTextual() || altVal.isNumber()) {
                Double conf = parent.hasNonNull(fieldName + "Confidence") ? parent.get(fieldName + "Confidence").asDouble() : null;
                String ev = parent.hasNonNull(fieldName + "Evidence") ? parent.get(fieldName + "Evidence").asText(null) : null;
                return FieldExtraction.of(altVal.asText(null), conf, ev);
            }
            return FieldExtraction.empty();
        }

        if (fieldNode.isObject()) {
            String value = fieldNode.hasNonNull("value") ? fieldNode.get("value").asText(null) : null;
            Double confidence = fieldNode.hasNonNull("confidence") ? fieldNode.get("confidence").asDouble() : null;
            String evidence = fieldNode.hasNonNull("evidence") ? fieldNode.get("evidence").asText(null) : null;
            return FieldExtraction.of(value, confidence, evidence);
        }

        String textVal = fieldNode.asText(null);
        Double conf = parent.hasNonNull(fieldName + "Confidence") ? parent.get(fieldName + "Confidence").asDouble() : 0.90;
        String ev = parent.hasNonNull(fieldName + "Evidence") ? parent.get(fieldName + "Evidence").asText(null) : "Visible on packaging label";
        return FieldExtraction.of(textVal, conf, ev);
    }

    private FieldExtraction parseMrpField(JsonNode parent) {
        FieldExtraction raw = parseField(parent, "mrp");
        if (!raw.isPresent()) {
            return FieldExtraction.empty();
        }

        String v = raw.value();
        // Remove currency symbols, slashes, and trailing taxes note
        String normalized = v.replaceAll("(?i)[₹Rs\\.\\s/\\-]+", "").trim();
        normalized = normalized.replaceAll("[,;]+$", "").trim();

        // If numeric or decimal, return clean price
        if (normalized.matches("^[0-9]+(\\.[0-9]{1,2})?$")) {
            return FieldExtraction.of(normalized, raw.confidence(), raw.evidence());
        }

        return FieldExtraction.of(raw.value().replaceAll("^[~\\-\\s]+", "").trim(), raw.confidence(), raw.evidence());
    }

    private FieldExtraction parseDateField(JsonNode parent, String fieldName) {
        FieldExtraction raw = parseField(parent, fieldName);
        if (!raw.isPresent()) {
            return FieldExtraction.empty();
        }

        String val = raw.value().trim();
        String lower = val.toLowerCase();

        // Anti-hallucination: Never turn nutrition lines (e.g. "Protein 0.5 Gms") into dates
        if (lower.contains("protein") || lower.contains("carb") || lower.contains("fat")
                || lower.contains("sugar") || lower.contains("energy") || lower.contains("kcal")
                || lower.matches("^[0-9.]+\\s*(gms?|g|mg)$")) {
            log.debug("Rejected date field [{}] containing nutrition token: [{}]", fieldName, val);
            return FieldExtraction.empty();
        }

        return raw;
    }

    private FieldExtraction parseManufacturerField(JsonNode parent, String fieldName) {
        FieldExtraction raw = parseField(parent, fieldName);
        if (!raw.isPresent()) {
            return FieldExtraction.empty();
        }

        String val = raw.value().trim();
        String lower = val.toLowerCase().replaceAll("[:;.,]+$", "").trim();

        // Anti-hallucination: If label only has header without entity (e.g. "Manufactured and", "Manufactured by"), do not guess
        if (lower.equals("manufactured and")
                || lower.equals("manufactured by")
                || lower.equals("manufactured &")
                || lower.equals("manufactured & packed at")
                || lower.equals("manufactured and packed")
                || lower.equals("manufactured and packed at")
                || lower.equals("manufactured and packed by")
                || lower.equals("packed by")
                || lower.equals("marketed by")
                || lower.equals("imported by")
                || (lower.startsWith("manufactured and") && lower.length() < 35)
                || (lower.startsWith("manufactured &") && lower.length() < 35)) {
            log.warn("Rejected incomplete manufacturer declaration header: [{}]", val);
            return FieldExtraction.empty();
        }

        return raw;
    }

    private FieldExtraction parseFssaiField(JsonNode parent) {
        FieldExtraction raw = parseField(parent, "fssaiLicenseNumber");
        if (raw.isPresent()) {
            String val = raw.value();
            if (val.toLowerCase().contains("applied")) {
                return FieldExtraction.of("Applied For", raw.confidence(), raw.evidence());
            }
            String clean = val.replaceAll("[^0-9]", "");
            if (clean.length() == 14) {
                return FieldExtraction.of(clean, raw.confidence(), raw.evidence());
            }
            if (clean.length() >= 10 && clean.length() <= 16) {
                return FieldExtraction.of(clean, Math.min(0.75, raw.safeConfidence()), raw.evidence());
            }
            return raw;
        }

        JsonNode statusNode = parent.path("fssaiStatus");
        String statusVal = statusNode.isObject() ? statusNode.path("value").asText(null) : statusNode.asText(null);
        if (statusVal != null && statusVal.toLowerCase().contains("applied")) {
            return FieldExtraction.of("Applied For", 0.90, "Visible 'Applied For' declaration");
        }

        return FieldExtraction.empty();
    }

    private FieldExtraction parsePhoneField(JsonNode parent) {
        FieldExtraction raw = parseField(parent, "phone");
        if (!raw.isPresent()) {
            return FieldExtraction.empty();
        }
        String clean = raw.value().replaceAll("[^0-9+]", "");
        if (clean.length() >= 7) {
            return raw;
        }
        return FieldExtraction.empty();
    }

    private FieldExtraction parseEmailField(JsonNode parent) {
        FieldExtraction raw = parseField(parent, "email");
        if (!raw.isPresent()) {
            return FieldExtraction.empty();
        }
        String val = raw.value().trim();
        if (val.contains("@") && val.contains(".")) {
            return FieldExtraction.of(val, raw.confidence(), raw.evidence());
        }
        return FieldExtraction.empty();
    }
}
