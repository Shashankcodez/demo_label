package com.labelcheck.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelcheck.config.AiProperties;
import com.labelcheck.dto.ai.AiExtractionStatus;
import com.labelcheck.dto.ai.AiLabelExtractionResult;
import com.labelcheck.dto.ai.AiNutritionItem;
import com.labelcheck.dto.ai.FieldExtraction;
import com.labelcheck.dto.ai.StructuredAiLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Native Gemini 3.8 Flash Vision extraction service.
 * Connects directly to the Google Generative Language API (generateContent) using
 * multimodal image input and structured JSON generation.
 *
 * All Google Gemini vendor-specific request/response logic is strictly encapsulated here.
 * The API key is stored only on the backend and is NEVER logged or returned to the client.
 */
@org.springframework.stereotype.Component("geminiVisionExtractor")
public class GeminiVisionLabelExtractor implements VisionLabelExtractor {

    private static final Logger log = LoggerFactory.getLogger(GeminiVisionLabelExtractor.class);

    private static final String GEMINI_SYSTEM_INSTRUCTION = """
            You are LabelCheck's visual food-package declaration extraction engine.

            Inspect the COMPLETE supplied food-label image.

            Your ONLY responsibility is to extract information that is visibly supported by the image.

            You are NOT a legal compliance engine.
            Do NOT decide whether the package is compliant.
            Do NOT invent values.
            Do NOT guess missing information.
            Do NOT infer values from what a typical food package normally contains.

            Read the whole image before declaring a field missing.

            Packaging may use unusual layouts, multiple columns, tables, rotated sections, colored boxes, tiny declarations, multilingual text, symbols, or unconventional placement.

            Identify information by visual context and semantic headings rather than fixed coordinates.

            For every field:
            - return the visibly supported value
            - return null if it cannot be established
            - provide confidence (between 0.0 and 1.0)
            - provide concise visual evidence describing where/how the field is visible

            Critical anti-hallucination rule:
            VISIBLE EVIDENCE > ASSUMPTION
            If uncertain: return null.

            Do not force a value into every field.
            Do not confuse nutrition values with MRP, dates, quantity, batch, or phone numbers.
            Do not confuse marketing text with mandatory declarations.

            For MRP:
            look specifically for MRP / MAX RETAIL PRICE / INCL OF ALL TAXES and nearby price text.
            Do not assume the largest number is MRP.
            Do not confuse calories, protein, carbohydrates, etc. with MRP.

            For dates:
            only return a date when clearly associated with a declaration such as:
            MFD, MFG, MANUFACTURED, PACKED, PKD, PACKED ON, BEST BEFORE, BB, EXP, EXPIRY, USE BY.
            Do not turn arbitrary number sequences into dates.

            For batch:
            look for BATCH / LOT / B.NO / BATCH NO and nearby identifier.

            For net quantity:
            look for NET QTY / NET QUANTITY / NET WT / NET WEIGHT / NET VOLUME / CONTENTS and nearby units (g, kg, ml, l, pcs).

            For FSSAI:
            if an FSSAI license number is visibly printed, return it.
            if the label says Applied For, return Applied For.
            if FSSAI text exists but no number can be established, return the text/status appropriately.
            Do not claim official license verification.

            For manufacturer/packer/marketer/importer:
            use explicitly labelled declarations when available.
            Do not invent corporate roles.

            For address:
            preserve the visible address as faithfully as possible.

            For phone:
            recognize Indian formats including +91, 10-digit mobile, and STD landline spacing.

            For email:
            return only a visibly supported email.

            For ingredients:
            return visible ingredient text.
            Do not invent ingredients.

            For allergens:
            return visible allergen declarations.
            Do not infer allergens merely from ingredient knowledge.

            For nutrition:
            extract visible nutrition rows but do not let them interfere with other fields.

            For vegetarian/non-vegetarian:
            only classify when the visual symbol (green dot in green square / brown triangle) is sufficiently clear.
            Otherwise return unknown/null.

            For country of origin:
            only return if the label visibly states it or clearly supports it (e.g. India, Made in India).

            Always inspect the entire image.
            Return concise evidence.
            Do not return long explanations.

            You MUST return a valid JSON object matching this schema:
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
              "fssaiStatus": { "value": "NUMBER_DETECTED / APPLIED_FOR / NOT_DETECTED", "confidence": 0.95, "evidence": "..." },
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

    public GeminiVisionLabelExtractor(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties != null ? aiProperties : new AiProperties();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public boolean isEnabled() {
        return aiProperties.isEnabled()
                && aiProperties.getApiKey() != null
                && !aiProperties.getApiKey().trim().isEmpty();
    }

    @Override
    public AiLabelExtractionResult extract(Path imagePath, String contentType) {
        if (!isEnabled()) {
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.OCR_AVAILABLE_EXTRACTION_LIMITED,
                    aiProperties.getModel(),
                    "AI extraction is not configured. Using local OCR fallback."
            );
        }

        if (imagePath == null || !Files.exists(imagePath)) {
            log.warn("Gemini extraction aborted: image file does not exist at [{}]", imagePath);
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                    aiProperties.getModel(),
                    "Image file not found for Vision AI extraction"
            );
        }

        byte[] imageBytes;
        try {
            imageBytes = Files.readAllBytes(imagePath);
        } catch (IOException e) {
            log.error("Failed to read image bytes for Gemini extraction from [{}]: {}", imagePath, e.getMessage());
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                    aiProperties.getModel(),
                    "Failed to read image payload: " + e.getMessage()
            );
        }

        String base64Data = Base64.getEncoder().encodeToString(imageBytes);
        String resolvedMime = (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg";

        int maxAttempts = 1 + Math.max(0, aiProperties.getMaxRetries());
        boolean includeThinking = aiProperties.getThinkingBudget() > 0;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Gemini extraction started for image [{}] using model [{}] (attempt {}/{})",
                        imagePath.getFileName(), aiProperties.getModel(), attempt, maxAttempts);

                String requestBody = buildGeminiRequestBody(resolvedMime, base64Data, includeThinking);
                String endpointUrl = buildEndpointUrl();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpointUrl))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", aiProperties.getApiKey().trim())
                        .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                log.info("Gemini response received with HTTP status [{}]", statusCode);

                if (statusCode == 200) {
                    AiLabelExtractionResult result = parseGeminiResponse(response.body());
                    if (result != null && result.status() != AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK) {
                        log.info("Gemini extraction completed successfully: fieldsDetected=[{}]",
                                result.label() != null ? result.label().countDetectedFields() : 0);
                        return result;
                    }
                    log.warn("Gemini 200 response could not be parsed into valid structured label; attempt {}/{}", attempt, maxAttempts);
                } else if (statusCode == 400 && includeThinking && response.body() != null && response.body().toLowerCase().contains("thinking")) {
                    log.warn("Gemini rejected thinkingConfig (400), retrying immediately without thinkingConfig");
                    includeThinking = false;
                    continue;
                } else if (statusCode == 401 || statusCode == 403) {
                    log.warn("Gemini response received with HTTP status [{}] (authentication failure). Fallback OCR used.", statusCode);
                    return AiLabelExtractionResult.failed(
                            AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                            aiProperties.getModel(),
                            "AI authentication unavailable. Using local OCR fallback."
                    );
                } else if (statusCode == 429) {
                    log.warn("Gemini response received with HTTP status [429] (rate limited). Attempt {}/{}", attempt, maxAttempts);
                    if (attempt < maxAttempts) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                    return AiLabelExtractionResult.failed(
                            AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                            aiProperties.getModel(),
                            "AI request limit reached. Using local OCR fallback."
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
                            aiProperties.getModel(),
                            "AI service temporarily unavailable. Using local OCR fallback."
                    );
                } else {
                    log.warn("Gemini returned unexpected status [{}]: {}", statusCode, response.body());
                    return AiLabelExtractionResult.failed(
                            AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                            aiProperties.getModel(),
                            "AI extraction returned status " + statusCode + ". Using local OCR fallback."
                    );
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Gemini request interrupted");
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        aiProperties.getModel(),
                        "AI request interrupted. Using local OCR fallback."
                );
            } catch (java.net.http.HttpTimeoutException te) {
                log.warn("Gemini request timed out on attempt {}/{}: {}", attempt, maxAttempts, te.getMessage());
                if (attempt < maxAttempts) {
                    continue;
                }
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        aiProperties.getModel(),
                        "AI request timed out. Using local OCR fallback."
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
                        aiProperties.getModel(),
                        "AI network error: " + ex.getClass().getSimpleName() + ". Using local OCR fallback."
                );
            }
        }

        return AiLabelExtractionResult.failed(
                AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                aiProperties.getModel(),
                "Gemini extraction could not be completed. Local OCR fallback was used."
        );
    }

    private String buildEndpointUrl() {
        String base = aiProperties.getBaseUrl().replaceAll("/+$", "");
        String model = aiProperties.getModel().trim();
        return base + "/v1beta/models/" + model + ":generateContent";
    }

    private String buildGeminiRequestBody(String mimeType, String base64Data, boolean includeThinking) throws Exception {
        Map<String, Object> root = new HashMap<>();

        // 1. contents: [ { role: "user", parts: [ inlineData, text ] } ]
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("role", "user");

        List<Map<String, Object>> parts = new ArrayList<>();

        // Image part
        Map<String, Object> imagePart = new HashMap<>();
        Map<String, String> inlineData = new HashMap<>();
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64Data);
        imagePart.put("inlineData", inlineData);
        parts.add(imagePart);

        // Instruction / Prompt part
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", GEMINI_SYSTEM_INSTRUCTION);
        parts.add(textPart);

        userContent.put("parts", parts);
        contents.add(userContent);
        root.put("contents", contents);

        // 2. generationConfig
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", 0.1);
        generationConfig.put("maxOutputTokens", 4096);

        if (includeThinking && aiProperties.getThinkingBudget() > 0) {
            Map<String, Object> thinkingConfig = new HashMap<>();
            thinkingConfig.put("thinkingBudget", aiProperties.getThinkingBudget());
            generationConfig.put("thinkingConfig", thinkingConfig);
        }

        root.put("generationConfig", generationConfig);

        return objectMapper.writeValueAsString(root);
    }

    public AiLabelExtractionResult parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                log.warn("Gemini response had no candidates");
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        aiProperties.getModel(),
                        "Gemini returned empty candidates array"
                );
            }

            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                log.warn("Gemini candidate had no content parts");
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        aiProperties.getModel(),
                        "Gemini candidate had empty content parts"
                );
            }

            String jsonText = null;
            for (JsonNode part : parts) {
                if (part.hasNonNull("text")) {
                    // Skip thinking part if designated
                    if (part.has("thought") && part.get("thought").asBoolean()) {
                        continue;
                    }
                    jsonText = part.get("text").asText();
                }
            }

            if (jsonText == null || jsonText.isBlank()) {
                // Fallback to first part text if thought filtering removed everything
                jsonText = parts.get(0).path("text").asText(null);
            }

            if (jsonText == null || jsonText.isBlank()) {
                log.warn("Gemini parts contained no text payload");
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        aiProperties.getModel(),
                        "AI extraction could not be interpreted. Using OCR fallback."
                );
            }

            // Clean markdown code blocks (```json ... ```)
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

            log.info("Successfully parsed Gemini structured label: detectedFields=[{}/12], confidence=[{}]",
                    detectedCount, String.format("%.2f", confidence));

            if (detectedCount >= 6) {
                return AiLabelExtractionResult.success(confidence, aiProperties.getModel(), structuredLabel);
            } else if (detectedCount >= 1) {
                return AiLabelExtractionResult.partial(confidence, aiProperties.getModel(), structuredLabel);
            } else {
                return new AiLabelExtractionResult(
                        AiExtractionStatus.IMAGE_QUALITY_LOW,
                        0.0,
                        "VISION_AI",
                        aiProperties.getModel(),
                        structuredLabel,
                        cleanJson,
                        "Gemini inspected the image but detected 0 legible packaging declarations"
                );
            }

        } catch (Exception e) {
            log.error("Failed to parse Gemini JSON response: {}", e.getMessage(), e);
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                    aiProperties.getModel(),
                    "AI extraction could not be interpreted. Using OCR fallback."
            );
        }
    }

    public StructuredAiLabel parseStructuredAiLabel(JsonNode node) {
        Double overallConfidence = node.hasNonNull("overallConfidence") ? node.get("overallConfidence").asDouble() : null;

        FieldExtraction productName = parseField(node, "productName");
        FieldExtraction brand = parseField(node, "brand");
        FieldExtraction netQuantity = parseField(node, "netQuantity");
        FieldExtraction mrp = parseMrpField(node);
        FieldExtraction mrpIncludesTaxes = parseField(node, "mrpIncludesTaxes");
        FieldExtraction unitSalePrice = parseField(node, "unitSalePrice");
        FieldExtraction batchNumber = parseField(node, "batchNumber");
        FieldExtraction mfd = parseField(node, "manufacturedOrPackedDate");
        FieldExtraction exp = parseField(node, "bestBeforeOrExpiry");
        FieldExtraction fssai = parseFssaiField(node);
        FieldExtraction fssaiStatus = parseField(node, "fssaiStatus");
        FieldExtraction manufacturer = parseField(node, "manufacturer");
        FieldExtraction packer = parseField(node, "packer");
        FieldExtraction marketer = parseField(node, "marketer");
        FieldExtraction importer = parseField(node, "importer");
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

    private FieldExtraction parseField(JsonNode parent, String fieldName) {
        JsonNode fieldNode = parent.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            // Check flat alternate: fieldName + "Value"
            JsonNode altVal = parent.path(fieldName + "Value");
            if (altVal.isTextual() || altVal.isNumber()) {
                Double conf = parent.hasNonNull(fieldName + "Confidence") ? parent.get(fieldName + "Confidence").asDouble() : null;
                String ev = parent.hasNonNull(fieldName + "Evidence") ? parent.get(fieldName + "Evidence").asText(null) : null;
                return FieldExtraction.of(altVal.asText(null), conf, ev);
            }
            return FieldExtraction.empty();
        }

        // If the model returned a nested object { value, confidence, evidence }
        if (fieldNode.isObject()) {
            String value = fieldNode.hasNonNull("value") ? fieldNode.get("value").asText(null) : null;
            Double confidence = fieldNode.hasNonNull("confidence") ? fieldNode.get("confidence").asDouble() : null;
            String evidence = fieldNode.hasNonNull("evidence") ? fieldNode.get("evidence").asText(null) : null;
            return FieldExtraction.of(value, confidence, evidence);
        }

        // If the model returned a primitive string/number directly
        String textVal = fieldNode.asText(null);
        Double conf = parent.hasNonNull(fieldName + "Confidence") ? parent.get(fieldName + "Confidence").asDouble() : 0.85;
        String ev = parent.hasNonNull(fieldName + "Evidence") ? parent.get(fieldName + "Evidence").asText(null) : "Visible on packaging label";
        return FieldExtraction.of(textVal, conf, ev);
    }

    private FieldExtraction parseMrpField(JsonNode parent) {
        FieldExtraction raw = parseField(parent, "mrp");
        if (!raw.isPresent()) {
            return FieldExtraction.empty();
        }

        String v = raw.value();
        // Normalize price: strip rupee symbol, currency tokens, and whitespace
        String normalized = v.replaceAll("(?i)[₹Rs\\.\\s/\\-]+", "").trim();
        normalized = normalized.replaceAll("[,;]+$", "").trim();

        // Check if numeric or decimal
        if (normalized.matches("^[0-9]+(\\.[0-9]{1,2})?$")) {
            return FieldExtraction.of(normalized, raw.confidence(), raw.evidence());
        }

        // Return clean string if valid characters
        return FieldExtraction.of(raw.value().replaceAll("^[~\\-\\s]+", "").trim(), raw.confidence(), raw.evidence());
    }

    private FieldExtraction parseFssaiField(JsonNode parent) {
        FieldExtraction raw = parseField(parent, "fssaiLicenseNumber");
        if (raw.isPresent()) {
            String clean = raw.value().replaceAll("[^0-9]", "");
            if (clean.length() == 14) {
                return FieldExtraction.of(clean, raw.confidence(), raw.evidence());
            }
            if (raw.value().toLowerCase().contains("applied")) {
                return FieldExtraction.of("Applied For", raw.confidence(), raw.evidence());
            }
            return raw;
        }

        // Check if fssaiStatus mentions Applied For
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
