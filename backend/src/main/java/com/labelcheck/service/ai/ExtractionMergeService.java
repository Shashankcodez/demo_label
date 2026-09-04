package com.labelcheck.service.ai;

import com.labelcheck.dto.StructuredLabelData;
import com.labelcheck.dto.ai.AiExtractionStatus;
import com.labelcheck.dto.ai.AiLabelExtractionResult;
import com.labelcheck.dto.ai.FieldExtraction;
import com.labelcheck.dto.ai.StructuredAiLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Merges primary Vision AI extraction results with secondary Tesseract OCR evidence.
 * Ensures the system never produces blank / useless results:
 * - AI takes precedence when visible evidence is provided.
 * - Local deterministic OCR fills gaps when AI leaves fields null.
 * - Raw OCR text is preserved for inspector transparency and debugging.
 */
@Service
public class ExtractionMergeService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionMergeService.class);

    public record MergedResult(
            StructuredLabelData labelData,
            String extractionSource,
            String extractionStatus,
            Double overallConfidence,
            Map<String, String> fieldEvidence,
            Map<String, Double> fieldConfidence,
            String summaryMessage
    ) {}

    public MergedResult merge(
            AiLabelExtractionResult aiResult,
            StructuredLabelData ocrData,
            String rawOcrText,
            boolean isImageSuspectQuality
    ) {
        Map<String, String> evidenceMap = new HashMap<>();
        Map<String, Double> confidenceMap = new HashMap<>();

        boolean aiAvailable = aiResult != null
                && (aiResult.status() == AiExtractionStatus.AI_SUCCESS || aiResult.status() == AiExtractionStatus.AI_PARTIAL)
                && aiResult.label() != null;

        if (aiAvailable) {
            StructuredAiLabel ai = aiResult.label();

            String productName = resolveField(ai.productName(), ocrData != null ? ocrData.productName() : null, "productName", evidenceMap, confidenceMap);
            String brand = resolveField(ai.brand(), ocrData != null ? ocrData.brand() : null, "brand", evidenceMap, confidenceMap);
            String netQuantity = resolveField(ai.netQuantity(), ocrData != null ? ocrData.netQuantity() : null, "netQuantity", evidenceMap, confidenceMap);
            String mrp = resolveField(ai.mrp(), ocrData != null ? ocrData.mrp() : null, "mrp", evidenceMap, confidenceMap);

            Boolean mrpInclusiveOfTaxes = null;
            if (ai.mrpIncludesTaxes() != null && ai.mrpIncludesTaxes().isPresent()) {
                mrpInclusiveOfTaxes = "true".equalsIgnoreCase(ai.mrpIncludesTaxes().value());
                evidenceMap.put("mrpInclusiveOfTaxes", ai.mrpIncludesTaxes().evidence());
            } else if (ocrData != null) {
                mrpInclusiveOfTaxes = ocrData.mrpInclusiveOfTaxes();
            }

            String unitSalePrice = resolveField(ai.unitSalePrice(), ocrData != null ? ocrData.unitSalePrice() : null, "unitSalePrice", evidenceMap, confidenceMap);
            String manufacturerName = resolveManufacturer(ai, ocrData, evidenceMap, confidenceMap);
            String manufacturerAddress = resolveField(ai.address(), ocrData != null ? ocrData.manufacturerAddress() : null, "manufacturerAddress", evidenceMap, confidenceMap);
            String importerName = resolveField(ai.importer(), ocrData != null ? ocrData.importerName() : null, "importerName", evidenceMap, confidenceMap);
            String importerAddress = ocrData != null ? ocrData.importerAddress() : null;
            String countryOfOrigin = resolveField(ai.countryOfOrigin(), ocrData != null ? ocrData.countryOfOrigin() : null, "countryOfOrigin", evidenceMap, confidenceMap);
            String mfd = resolveField(ai.manufacturedOrPackedDate(), ocrData != null ? ocrData.manufactureOrPackingDate() : null, "manufactureOrPackingDate", evidenceMap, confidenceMap);
            String expiry = resolveField(ai.bestBeforeOrExpiry(), ocrData != null ? ocrData.bestBeforeOrExpiry() : null, "bestBeforeOrExpiry", evidenceMap, confidenceMap);

            String fssaiLicense = resolveFssaiNumber(ai, ocrData, evidenceMap, confidenceMap);
            String fssaiStatus = resolveFssaiStatus(ai, ocrData, fssaiLicense);

            String phone = resolveField(ai.phone(), ocrData != null ? ocrData.customerCarePhone() : null, "customerCarePhone", evidenceMap, confidenceMap);
            String email = resolveField(ai.email(), ocrData != null ? ocrData.customerCareEmail() : null, "customerCareEmail", evidenceMap, confidenceMap);
            String careAddress = ocrData != null ? ocrData.customerCareAddress() : null;
            String batchNumber = resolveField(ai.batchNumber(), ocrData != null ? ocrData.batchNumber() : null, "batchNumber", evidenceMap, confidenceMap);

            StructuredLabelData mergedData = new StructuredLabelData(
                    productName,
                    brand,
                    netQuantity,
                    mrp,
                    mrpInclusiveOfTaxes,
                    unitSalePrice,
                    manufacturerName,
                    manufacturerAddress,
                    importerName,
                    importerAddress,
                    countryOfOrigin,
                    mfd,
                    expiry,
                    fssaiLicense,
                    phone,
                    email,
                    careAddress,
                    rawOcrText,
                    batchNumber,
                    fssaiStatus
            );

            int fieldsCount = mergedData.countDetectedFields();
            String status = (fieldsCount >= 6) ? "AI_SUCCESS" : "AI_PARTIAL";
            double overallConf = aiResult.overallConfidence() != null ? aiResult.overallConfidence() : 0.85;

            String summary = String.format("Vision AI extracted %d statutory declarations with %.0f%% overall confidence.",
                    fieldsCount, overallConf * 100);

            String source = (aiResult != null && aiResult.extractionSource() != null && !aiResult.extractionSource().isBlank())
                    ? aiResult.extractionSource()
                    : "Groq Vision";

            return new MergedResult(
                    mergedData,
                    source,
                    status,
                    overallConf,
                    evidenceMap,
                    confidenceMap,
                    summary
            );
        }

        // AI was unavailable, disabled, or failed — fall back gracefully to local Tesseract OCR
        StructuredLabelData fallbackData = ocrData != null ? ocrData : createEmptyData(rawOcrText);
        int fallbackFieldsCount = fallbackData.countDetectedFields();

        String fallbackStatus;
        if (isImageSuspectQuality && fallbackFieldsCount == 0) {
            fallbackStatus = "IMAGE_QUALITY_LOW";
        } else if (aiResult != null && aiResult.status() == AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK) {
            fallbackStatus = "AI_FAILED_TESSERACT_FALLBACK";
        } else if (fallbackFieldsCount > 0) {
            fallbackStatus = "OCR_AVAILABLE_EXTRACTION_LIMITED";
        } else {
            fallbackStatus = "TOTAL_EXTRACTION_FAILURE";
        }

        double fallbackConf = fallbackFieldsCount >= 6 ? 0.70 : (fallbackFieldsCount > 0 ? 0.50 : 0.0);
        String summary = (aiResult != null && aiResult.errorMessage() != null)
                ? aiResult.errorMessage()
                : (fallbackFieldsCount > 0
                ? String.format("Local OCR fallback extracted %d declarations from image.", fallbackFieldsCount)
                : "Unable to detect statutory declarations from this photograph.");

        return new MergedResult(
                fallbackData,
                "TESSERACT_FALLBACK",
                fallbackStatus,
                fallbackConf,
                evidenceMap,
                confidenceMap,
                summary
        );
    }

    private String resolveField(FieldExtraction aiField, String ocrValue, String fieldName,
                                Map<String, String> evidenceMap, Map<String, Double> confidenceMap) {
        if (aiField != null && aiField.isPresent()) {
            if (aiField.evidence() != null && !aiField.evidence().isBlank()) {
                evidenceMap.put(fieldName, aiField.evidence());
            }
            if (aiField.confidence() != null) {
                confidenceMap.put(fieldName, aiField.safeConfidence());
            }
            return aiField.value();
        }
        if (ocrValue != null && !ocrValue.trim().isEmpty() && !"null".equalsIgnoreCase(ocrValue)) {
            evidenceMap.put(fieldName, "Extracted via local OCR heuristics");
            confidenceMap.put(fieldName, 0.65);
            return ocrValue.trim();
        }
        return null;
    }

    private String resolveManufacturer(StructuredAiLabel ai, StructuredLabelData ocrData,
                                       Map<String, String> evidenceMap, Map<String, Double> confidenceMap) {
        if (ai.manufacturer() != null && ai.manufacturer().isPresent()) {
            evidenceMap.put("manufacturerName", ai.manufacturer().evidence());
            confidenceMap.put("manufacturerName", ai.manufacturer().safeConfidence());
            return ai.manufacturer().value();
        }
        if (ai.packer() != null && ai.packer().isPresent()) {
            evidenceMap.put("manufacturerName", ai.packer().evidence());
            confidenceMap.put("manufacturerName", ai.packer().safeConfidence());
            return ai.packer().value();
        }
        if (ai.marketer() != null && ai.marketer().isPresent()) {
            evidenceMap.put("manufacturerName", ai.marketer().evidence());
            confidenceMap.put("manufacturerName", ai.marketer().safeConfidence());
            return ai.marketer().value();
        }
        if (ocrData != null && ocrData.manufacturerName() != null) {
            evidenceMap.put("manufacturerName", "Extracted via local OCR heuristics");
            confidenceMap.put("manufacturerName", 0.60);
            return ocrData.manufacturerName();
        }
        return null;
    }

    private String resolveFssaiNumber(StructuredAiLabel ai, StructuredLabelData ocrData,
                                      Map<String, String> evidenceMap, Map<String, Double> confidenceMap) {
        if (ai.fssaiLicenseNumber() != null && ai.fssaiLicenseNumber().isPresent()) {
            evidenceMap.put("fssaiLicenseNumber", ai.fssaiLicenseNumber().evidence());
            confidenceMap.put("fssaiLicenseNumber", ai.fssaiLicenseNumber().safeConfidence());
            return ai.fssaiLicenseNumber().value();
        }
        if (ocrData != null && ocrData.fssaiLicenseNumber() != null && !"NOT_DETECTED".equalsIgnoreCase(ocrData.fssaiLicenseNumber())) {
            evidenceMap.put("fssaiLicenseNumber", "Extracted via local OCR heuristics");
            confidenceMap.put("fssaiLicenseNumber", 0.70);
            return ocrData.fssaiLicenseNumber();
        }
        return null;
    }

    private String resolveFssaiStatus(StructuredAiLabel ai, StructuredLabelData ocrData, String fssaiLicense) {
        if (ai.fssaiStatus() != null && ai.fssaiStatus().isPresent()) {
            String status = ai.fssaiStatus().value().toUpperCase();
            if (status.contains("APPLIED")) return "APPLIED_FOR";
            if (status.contains("NUMBER") || status.contains("DETECTED")) return "NUMBER_DETECTED";
        }
        if (fssaiLicense != null) {
            if ("Applied For".equalsIgnoreCase(fssaiLicense)) return "APPLIED_FOR";
            if (fssaiLicense.matches("^[0-9]{14}$")) return "NUMBER_DETECTED";
        }
        if (ocrData != null && ocrData.fssaiStatus() != null) {
            return ocrData.fssaiStatus();
        }
        return "NOT_DETECTED";
    }

    private StructuredLabelData createEmptyData(String rawOcrText) {
        return new StructuredLabelData(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                rawOcrText, null, "NOT_DETECTED"
        );
    }
}
