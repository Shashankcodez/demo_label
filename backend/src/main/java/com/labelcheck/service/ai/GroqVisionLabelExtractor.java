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
import org.springframework.util.StringUtils;

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
 * Production Groq Vision AI extractor powered by qwen/qwen3.6-27b.
 * Connects directly to Groq's OpenAI-compatible multimodal endpoint:
 * https://api.groq.com/openai/v1/chat/completions
 *
 * Configured with:
 * - max_completion_tokens = 2500
 * - reasoning_effort = none (Qwen 3.6 non-thinking fast extraction mode)
 * - temperature = 0.2
 * - stream = false
 * - response_format = { "type": "json_object" }
 *
 * The API key is stored only on the backend (GROQ_API_KEY) and is NEVER logged or exposed to the client.
 */
@Service
@Primary
public class GroqVisionLabelExtractor implements VisionLabelExtractor {

    private static final Logger log = LoggerFactory.getLogger(GroqVisionLabelExtractor.class);

    private static final String SYSTEM_PROMPT = """
            You are a food packaging label information extraction engine.
            Your job is ONLY to inspect the supplied package-label image and extract information that is visibly supported by the image.

            VISIBLE EVIDENCE > ASSUMPTION.
            If a field is not clearly visible: value = null.
            Do NOT determine legal compliance.
            Do NOT invent missing values.
            Do NOT infer a value merely because it is common on food labels.
            Do NOT fill a missing field with an educated guess.
            Do NOT convert uncertain visual interpretation into a definite value.
            Return ONLY a single valid JSON object. No explanations, no analysis, no natural-language prose, no markdown code blocks.

            For every field provide:
            - "value": the exact visible value (string), or null if not clearly visible
            - "confidence": number between 0.0 and 1.0 (0.0 if null)
            - "evidence": brief quote or location of visible text in the image (or null)

            CRITICAL EXTRACTION GUIDELINES:
            1. PRODUCT NAME & BRAND:
               - productName: Prominent product title text (e.g., "Apple Slice"). Do NOT confuse with slogans, marketing blurbs, or nutrition headers.
               - brand: Brand name or logo brand text.
            2. NET QUANTITY:
               - Look for Net Qty, Net Weight, Net Wt, Net Volume, and units (g, gm, kg, ml, l, pcs).
            3. MRP & TAXES:
               - Look for semantic context: MRP, MAX RETAIL PRICE, MAXIMUM RETAIL PRICE, INCL ALL TAXES.
               - mrp: Extract the numeric price only (e.g., "150"). NEVER confuse MRP with calories, protein, carbs, fat, sugar, batch numbers, phone numbers, or dates.
               - If MRP is unclear or not present, set mrp.value = null.
               - mrpIncludesTaxes: "true" if text indicates inclusive of taxes (e.g. "Incl. of all taxes"), "false" if exclusive, or null.
               - unitSalePrice: Unit sale price if explicitly declared (e.g., "₹0.60/g").
            4. DATES (Contextual interpretation required):
               - manufacturedOrPackedDate: Date with context MFD, MFG, PKD, PACKED, PACKED ON, MANUFACTURED.
               - bestBeforeOrExpiry: Date with context BEST BEFORE, BB, EXP, EXPIRY, USE BY.
               - NEVER confuse nutrition lines (e.g. "Protein 0.5 Gms") with a date. If meaning is ambiguous, set to null.
            5. BATCH NUMBER:
               - batchNumber: Batch, Lot, B.No code.
            6. FSSAI:
               - fssaiLicenseNumber: 14-digit license number if visible; or "Applied For" if those words appear; otherwise null.
               - fssaiStatus: "NUMBER_DETECTED" (if 14-digit number), "APPLIED_FOR" (if words "Applied For"), "TEXT_PRESENT_NUMBER_NOT_DETECTED", or "NOT_DETECTED".
            7. MANUFACTURER / PACKER / ADDRESS:
               - Support explicit labels: Manufactured by, Packed by, Marketed by, Imported by.
               - Do not invent entity relationships.
               - address: Complete visible physical postal address with PIN code if present. Preserve full multiline text.
            8. CONSUMER CARE:
               - phone: Recognize Indian phone formats (+91 8888 720 520, 8888720520, 08888 720 520) with labels Phone, Ph, Mobile, Contact, Call, Customer Care.
               - email: Customer care email address.
            9. INGREDIENTS & NUTRITION:
               - ingredients: Full visible ingredients list.
               - allergens: Explicit allergen statements (e.g., "Contains Wheat, Milk").
               - vegetarianSymbol: "vegetarian", "nonVegetarian", or "unknown".
               - nutrition: Array of visible nutrition items with nutrient, amountPerServing, amountPer100g, unit.
               - Do NOT allow nutrition rows to contaminate MRP, dates, or batch extraction.

            Return JSON matching this exact structure:
            {
              "overallConfidence": 0.95,
              "productName": { "value": "...", "confidence": 0.95, "evidence": "..." },
              "brand": { "value": "...", "confidence": 0.90, "evidence": "..." },
              "netQuantity": { "value": "...", "confidence": 0.95, "evidence": "..." },
              "mrp": { "value": "...", "confidence": 0.95, "evidence": "..." },
              "mrpIncludesTaxes": { "value": "true/false", "confidence": 0.90, "evidence": "..." },
              "unitSalePrice": { "value": "...", "confidence": 0.0, "evidence": null },
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
              "ingredients": { "value": "...", "confidence": 0.85, "evidence": "..." },
              "allergens": { "value": "...", "confidence": 0.85, "evidence": "..." },
              "vegetarianSymbol": { "value": "vegetarian/nonVegetarian/unknown", "confidence": 0.90, "evidence": "..." },
              "storageInstructions": { "value": "...", "confidence": 0.85, "evidence": "..." },
              "nutrition": [
                { "nutrient": "Energy", "amountPerServing": "...", "amountPer100g": "...", "unit": "kcal" }
              ],
              "otherDeclarations": []
            }
            """;

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GroqVisionLabelExtractor(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public boolean isEnabled() {
        return aiProperties.isEnabled() && StringUtils.hasText(aiProperties.getApiKey());
    }

    @Override
    public AiLabelExtractionResult extract(Path imagePath, String contentType) {
        if (!isEnabled()) {
            log.info("Vision AI extraction requested but disabled or unconfigured (missing GROQ_API_KEY). Falling back to local OCR.");
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.OCR_AVAILABLE_EXTRACTION_LIMITED,
                    aiProperties.getModel(),
                    "Vision AI is disabled or GROQ_API_KEY is not configured"
            );
        }

        if (imagePath == null || !Files.exists(imagePath)) {
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.TOTAL_EXTRACTION_FAILURE,
                    aiProperties.getModel(),
                    "Image file does not exist on disk"
            );
        }

        byte[] imageBytes;
        try {
            imageBytes = Files.readAllBytes(imagePath);
        } catch (IOException e) {
            log.error("Failed to read image bytes for Groq Vision AI request: {}", e.getMessage());
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                    aiProperties.getModel(),
                    "Unable to read image bytes from disk"
            );
        }

        String mime = (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg";
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String dataUri = "data:" + mime + ";base64," + base64Image;

        String requestPayload;
        try {
            requestPayload = buildRequestBody(dataUri);
        } catch (Exception e) {
            log.error("Failed to construct Groq Vision AI JSON payload: {}", e.getMessage());
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                    aiProperties.getModel(),
                    "Failed to construct request payload"
            );
        }

        String endpoint = aiProperties.getBaseUrl() + "/chat/completions";
        log.info("Dispatching Groq Vision AI extraction request: endpoint=[{}], model=[{}], imageSize=[{} KB]",
                endpoint, aiProperties.getModel(), imageBytes.length / 1024);

        int maxAttempts = 1 + aiProperties.getMaxRetries();
        long startTime = System.currentTimeMillis();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                        .header("Authorization", "Bearer " + aiProperties.getApiKey())
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                long duration = System.currentTimeMillis() - startTime;

                log.info("Groq Vision AI response received: statusCode=[{}], duration=[{} ms], attempt=[{}/{}]",
                        statusCode, duration, attempt, maxAttempts);

                if (statusCode == 200) {
                    return parseSuccessfulResponse(response.body());
                }

                if (statusCode == 401 || statusCode == 403) {
                    log.warn("Groq Vision AI authentication failed (HTTP {}). Check GROQ_API_KEY.", statusCode);
                    return AiLabelExtractionResult.failed(
                            AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                            aiProperties.getModel(),
                            "Groq authorization failed (HTTP " + statusCode + "). Using local OCR fallback."
                    );
                }

                if (statusCode == 429) {
                    log.warn("Groq Vision AI rate limit reached (HTTP 429) on attempt {}/{}", attempt, maxAttempts);
                    if (attempt < maxAttempts) {
                        Thread.sleep(1500);
                        continue;
                    }
                    return AiLabelExtractionResult.failed(
                            AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                            aiProperties.getModel(),
                            "Groq rate limit reached. Local OCR fallback was used."
                    );
                }

                if (statusCode >= 500) {
                    log.warn("Groq Vision AI provider error (HTTP {}) on attempt {}/{}", statusCode, attempt, maxAttempts);
                    if (attempt < maxAttempts) {
                        Thread.sleep(1200);
                        continue;
                    }
                    return AiLabelExtractionResult.failed(
                            AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                            aiProperties.getModel(),
                            "Groq server error (HTTP " + statusCode + "). Local OCR fallback was used."
                    );
                }

                log.warn("Groq Vision AI request returned unexpected HTTP status {}: {}", statusCode, response.body());
                return AiLabelExtractionResult.failed(
                    AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                    aiProperties.getModel(),
                    "Groq returned HTTP " + statusCode + ". Local OCR fallback was used."
                );

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        aiProperties.getModel(),
                        "Groq request interrupted"
                );
            } catch (Exception ex) {
                log.warn("Groq Vision AI request exception on attempt {}/{}: {}", attempt, maxAttempts, ex.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(1200);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        aiProperties.getModel(),
                        "Groq network error: " + ex.getClass().getSimpleName() + ". Local OCR fallback was used."
                );
            }
        }

        return AiLabelExtractionResult.failed(
                AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                aiProperties.getModel(),
                "Groq Vision AI extraction could not be completed. Local OCR fallback was used."
        );
    }

    private String buildRequestBody(String dataUri) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getModel());
        body.put("temperature", aiProperties.getTemperature());
        body.put("max_completion_tokens", aiProperties.getMaxCompletionTokens());
        body.put("stream", false);

        if (StringUtils.hasText(aiProperties.getReasoningEffort())) {
            body.put("reasoning_effort", aiProperties.getReasoningEffort());
        }

        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");
        body.put("response_format", responseFormat);

        List<Map<String, Object>> messages = new ArrayList<>();

        // System message with strict extraction rules
        Map<String, Object> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", SYSTEM_PROMPT);
        messages.add(sysMsg);

        // User message with image and instruction
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");

        List<Map<String, Object>> contentList = new ArrayList<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", "Extract all visible food packaging label declarations from this image into the specified JSON structure. Visible evidence > assumption. Return null for fields not visibly present.");
        contentList.add(textPart);

        Map<String, Object> imgPart = new HashMap<>();
        imgPart.put("type", "image_url");
        Map<String, String> imgUrlObj = new HashMap<>();
        imgUrlObj.put("url", dataUri);
        imgPart.put("image_url", imgUrlObj);
        contentList.add(imgPart);

        userMsg.put("content", contentList);
        messages.add(userMsg);

        body.put("messages", messages);
        return objectMapper.writeValueAsString(body);
    }

    AiLabelExtractionResult parseSuccessfulResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // Log token usage safely without exposing secrets
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isMissingNode()) {
                int promptTokens = usageNode.path("prompt_tokens").asInt(-1);
                int completionTokens = usageNode.path("completion_tokens").asInt(-1);
                int totalTokens = usageNode.path("total_tokens").asInt(-1);
                if (totalTokens > 0) {
                    log.info("Groq Vision token usage: prompt_tokens=[{}], completion_tokens=[{}], total_tokens=[{}]",
                            promptTokens, completionTokens, totalTokens);
                }
            }

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.warn("Groq Vision AI response had empty choices array");
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        aiProperties.getModel(),
                        "Groq returned empty choices array"
                );
            }

            String content = choices.get(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                log.warn("Groq Vision AI response message content was empty");
                return AiLabelExtractionResult.failed(
                        AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                        aiProperties.getModel(),
                        "Groq returned empty message content"
                );
            }

            // Strip possible markdown code fences: ```json ... ```
            String cleanJson = content.trim();
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

            log.info("Successfully parsed Groq Vision structured label: detectedFields=[{}/12], confidence=[{}]",
                    detectedCount, String.format("%.2f", confidence));

            if (detectedCount >= 6) {
                return AiLabelExtractionResult.success(confidence, "Groq Vision", aiProperties.getModel(), structuredLabel);
            } else if (detectedCount >= 1) {
                return AiLabelExtractionResult.partial(confidence, "Groq Vision", aiProperties.getModel(), structuredLabel);
            } else {
                return new AiLabelExtractionResult(
                        AiExtractionStatus.IMAGE_QUALITY_LOW,
                        0.0,
                        "Groq Vision",
                        aiProperties.getModel(),
                        structuredLabel,
                        cleanJson,
                        "Groq Vision inspected the image but detected 0 legible packaging declarations"
                );
            }

        } catch (Exception e) {
            log.error("Failed to parse Groq Vision JSON response: {}", e.getMessage(), e);
            return AiLabelExtractionResult.failed(
                    AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                    aiProperties.getModel(),
                    "Failed to deserialize Groq structured response: " + e.getMessage()
            );
        }
    }

    private StructuredAiLabel parseStructuredAiLabel(JsonNode node) {
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
        FieldExtraction phone = parseField(node, "phone");
        FieldExtraction email = parseField(node, "email");
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
            return FieldExtraction.empty();
        }

        if (fieldNode.isObject()) {
            String value = fieldNode.hasNonNull("value") ? fieldNode.get("value").asText(null) : null;
            Double confidence = fieldNode.hasNonNull("confidence") ? fieldNode.get("confidence").asDouble() : null;
            String evidence = fieldNode.hasNonNull("evidence") ? fieldNode.get("evidence").asText(null) : null;
            return FieldExtraction.of(value, confidence, evidence);
        }

        String textVal = fieldNode.asText(null);
        return FieldExtraction.of(textVal, 0.85, "Visible in label region");
    }

    private FieldExtraction parseMrpField(JsonNode parent) {
        FieldExtraction raw = parseField(parent, "mrp");
        if (!raw.isPresent()) {
            return FieldExtraction.empty();
        }

        String v = raw.value();
        String normalized = v.replaceAll("(?i)[₹Rs\\.\\s/\\-]+", "").trim();
        normalized = normalized.replaceAll("[,;]+$", "").trim();

        if (normalized.matches("^[0-9]+(\\.[0-9]{1,2})?$")) {
            return FieldExtraction.of(normalized, raw.confidence(), raw.evidence());
        }

        return FieldExtraction.of(raw.value().replaceAll("^[~\\-\\s]+", "").trim(), raw.confidence(), raw.evidence());
    }

    private FieldExtraction parseFssaiField(JsonNode parent) {
        FieldExtraction raw = parseField(parent, "fssaiLicenseNumber");
        if (raw.isPresent()) {
            String clean = raw.value().replaceAll("[^0-9]", "");
            if (clean.length() == 14) {
                return FieldExtraction.of(clean, raw.confidence(), raw.evidence());
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
}
