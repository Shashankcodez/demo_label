package com.labelcheck.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceResult;
import com.labelcheck.compliance.ComplianceRuleEngine;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.config.AiProperties;
import com.labelcheck.dto.OcrResult;
import com.labelcheck.dto.PageResponse;
import com.labelcheck.dto.ScanResponse;
import com.labelcheck.dto.ScanSummaryResponse;
import com.labelcheck.dto.StructuredLabelData;
import com.labelcheck.dto.ai.AiExtractionStatus;
import com.labelcheck.dto.ai.AiLabelExtractionResult;
import com.labelcheck.entity.ScanEntity;
import com.labelcheck.exception.FileStorageException;
import com.labelcheck.exception.InvalidImageException;
import com.labelcheck.exception.ResourceNotFoundException;
import com.labelcheck.repository.ScanRepository;
import com.labelcheck.service.ai.ExtractionMergeService;
import com.labelcheck.service.ai.OpenAiCompatibleVisionExtractor;
import com.labelcheck.service.ai.VisionLabelExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Core orchestrator for the LabelCheck pipeline:
 * Ingestion → Format/Magic Byte Validation → Storage → Image Quality Assessment →
 * Vision AI Extraction (Primary) → Local Tesseract OCR (Fallback & Transparency Evidence) →
 * Extraction Merge → Deterministic Regulatory Compliance Evaluation → Persistence.
 */
@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF_HEADER = new byte[]{0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final byte[] WEBP_HEADER = new byte[]{0x57, 0x45, 0x42, 0x50}; // "WEBP"
    private static final List<String> WEBP_CHUNKS = List.of("VP8 ", "VP8L", "VP8X");

    private final FileStorageService fileStorageService;
    private final OcrService ocrService;
    private final LabelExtractionService labelExtractionService;
    private final ComplianceRuleEngine complianceRuleEngine;
    private final ScanRepository scanRepository;
    private final ObjectMapper objectMapper;
    private final VisionLabelExtractor visionLabelExtractor;
    private final ExtractionMergeService extractionMergeService;
    private final ImagePreprocessingService imagePreprocessingService;
    private final AiProperties aiProperties;

    @Autowired
    public ScanService(
            FileStorageService fileStorageService,
            OcrService ocrService,
            LabelExtractionService labelExtractionService,
            ComplianceRuleEngine complianceRuleEngine,
            ScanRepository scanRepository,
            ObjectMapper objectMapper,
            VisionLabelExtractor visionLabelExtractor,
            ExtractionMergeService extractionMergeService,
            ImagePreprocessingService imagePreprocessingService,
            AiProperties aiProperties
    ) {
        this.fileStorageService = fileStorageService;
        this.ocrService = ocrService;
        this.labelExtractionService = labelExtractionService;
        this.complianceRuleEngine = complianceRuleEngine;
        this.scanRepository = scanRepository;
        this.objectMapper = objectMapper;
        this.visionLabelExtractor = visionLabelExtractor;
        this.extractionMergeService = extractionMergeService;
        this.imagePreprocessingService = imagePreprocessingService;
        this.aiProperties = aiProperties;
    }

    /**
     * Backward-compatible constructor for unit and integration tests.
     */
    public ScanService(
            FileStorageService fileStorageService,
            OcrService ocrService,
            LabelExtractionService labelExtractionService,
            ComplianceRuleEngine complianceRuleEngine,
            ScanRepository scanRepository,
            ObjectMapper objectMapper
    ) {
        this(
                fileStorageService,
                ocrService,
                labelExtractionService,
                complianceRuleEngine,
                scanRepository,
                objectMapper,
                new OpenAiCompatibleVisionExtractor(new AiProperties(), objectMapper),
                new ExtractionMergeService(),
                new ImagePreprocessingService(),
                new AiProperties()
        );
    }

    /**
     * Returns the set of supported language codes available on the server.
     */
    public Set<String> getSupportedLanguages() {
        return ocrService.getSupportedLanguages();
    }

    @Transactional
    public ScanResponse processUpload(MultipartFile file) {
        return processUpload(file, null);
    }

    @Transactional
    public ScanResponse processUpload(MultipartFile file, String language) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Uploaded image file is missing or empty");
        }

        String rawContentType = file.getContentType();
        String contentType = rawContentType != null ? rawContentType.toLowerCase(Locale.ROOT).trim() : "";

        if (!SUPPORTED_MIME_TYPES.contains(contentType)) {
            throw new InvalidImageException(
                    "Unsupported media type: '" + rawContentType + "'. Supported image formats are JPEG, PNG, and WebP."
            );
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read bytes from uploaded file: {}", e.getMessage(), e);
            throw new InvalidImageException("Unable to read uploaded file stream");
        }

        // Validate actual binary image magic bytes
        validateImageBytes(contentType, bytes);

        UUID scanId = UUID.randomUUID();
        String extension = determineCanonicalExtension(contentType);
        String storedFilename = fileStorageService.storeFile(file, scanId, extension);

        Path storedFilePath = fileStorageService.getUploadLocation().resolve(storedFilename);

        // 1. Non-blocking image quality assessment
        ImagePreprocessingService.QualityAssessment quality = (imagePreprocessingService != null)
                ? imagePreprocessingService.assessQuality(storedFilePath)
                : new ImagePreprocessingService.QualityAssessment(false, "OK", 0, 0);

        // 2. Run Vision AI extraction (Primary Source for ultra-fast, high-precision detection)
        AiLabelExtractionResult aiResult = null;
        if (visionLabelExtractor != null && visionLabelExtractor.isEnabled()) {
            try {
                aiResult = visionLabelExtractor.extract(storedFilePath, contentType);
            } catch (Exception e) {
                log.warn("Primary Vision AI extraction encountered an error: {}", e.getMessage());
            }
        }

        // 3. Check if Vision AI succeeded with statutory declarations
        boolean aiSucceeded = aiResult != null
                && (aiResult.status() == AiExtractionStatus.AI_SUCCESS || aiResult.status() == AiExtractionStatus.AI_PARTIAL)
                && aiResult.label() != null
                && aiResult.label().countDetectedFields() > 0;

        // Run local Tesseract OCR as fallback if Vision AI did not succeed or is disabled
        OcrResult ocrResult = null;
        StructuredLabelData ocrData = null;
        if (!aiSucceeded && ocrService != null && ocrService.isAvailable()) {
            try {
                ocrResult = (language != null && !language.isBlank())
                        ? ocrService.extractText(storedFilePath, language)
                        : ocrService.extractText(storedFilePath);

                if (ocrResult != null && ocrResult.text() != null && !ocrResult.text().isBlank()) {
                    ocrData = labelExtractionService.extract(ocrResult.text());
                }
            } catch (Exception e) {
                log.warn("Local OCR fallback encountered an error: {}", e.getMessage());
            }
        }

        // 4. Merge Vision AI primary extraction with local OCR fallback
        String fallbackRawText = ocrResult != null ? ocrResult.text() : "";
        ExtractionMergeService.MergedResult merged = (extractionMergeService != null)
                ? extractionMergeService.merge(aiResult, ocrData, fallbackRawText, quality.isSuspect())
                : new ExtractionMergeService.MergedResult(
                        ocrData,
                        "TESSERACT_FALLBACK",
                        "OCR_AVAILABLE_EXTRACTION_LIMITED",
                        0.70,
                        Map.of(),
                        Map.of(),
                        "Local OCR extraction",
                        fallbackRawText,
                        Map.of()
                );

        StructuredLabelData labelData = merged.labelData();
        int detectedFieldsCount = labelData != null ? labelData.countDetectedFields() : 0;
        String qualityTier = labelData != null ? labelData.getQualityTier() : "VERY_POOR_IMAGE";
        String complianceOutcome = labelData != null ? labelData.getComplianceOutcome() : "Retake image";

        ComplianceResult complianceResult;
        String status;
        String message;
        String qualityMessage;

        // 5. Deterministic Compliance Rule Evaluation (AI NEVER DECIDES LEGAL COMPLIANCE)
        if (detectedFieldsCount == 0 || "IMAGE_QUALITY_LOW".equals(merged.extractionStatus()) || "TOTAL_EXTRACTION_FAILURE".equals(merged.extractionStatus())) {
            status = "VERY_POOR_IMAGE";
            qualityTier = "VERY_POOR_IMAGE";
            complianceOutcome = "Retake image";
            message = "Image quality is too low or unreadable to detect statutory declarations (0 fields detected). Retake image required.";
            qualityMessage = "Very Poor Image: No legible packaging text detected. Please retake the photograph.";
            complianceResult = new ComplianceResult(
                    RuleStatus.WARNING,
                    0,
                    List.of(new ComplianceCheck(
                            "rule-retake-image",
                            "Statutory Photographic Legibility",
                            "Rule 6 Legibility & Image Quality",
                            RuleStatus.WARNING,
                            "0 mandatory statutory packaging declarations detected in this photograph.",
                            "Under Legal Metrology (Packaged Commodities) Rules 2011 Rule 6, all statutory declarations must be clear, unambiguous, and legible.",
                            "Retake photograph: ensure adequate ambient lighting, eliminate bright reflections/glare, ensure sharp focus, and keep wrapper flat.",
                            RuleSeverity.HIGH
                    )),
                    "0 fields detected — automated compliance screening halted. Image quality is insufficient. Please retake the product photograph."
            );
        } else {
            // Evaluate deterministic statutory packaging rules with visual evidence & field confidences
            complianceResult = complianceRuleEngine.evaluate(labelData, merged.fieldEvidence(), merged.fieldConfidence());
            status = "ANALYSIS_COMPLETE";

            if (detectedFieldsCount >= 10) {
                message = String.format("Good Label (%s): %d statutory declarations detected. Full compliance screening completed.",
                        merged.extractionSource(), detectedFieldsCount);
                qualityMessage = String.format("Good Label (%d/12 fields detected) → Compliance", detectedFieldsCount);
            } else if (detectedFieldsCount >= 6) {
                message = String.format("Average Label (%s): %d statutory declarations detected. Compliance evaluated with physical review recommended.",
                        merged.extractionSource(), detectedFieldsCount);
                qualityMessage = String.format("Average Label (%d/12 fields detected) → Compliance + Needs Review", detectedFieldsCount);
            } else {
                message = String.format("Poor Label (%s): Only %d statutory declaration%s detected. Partial extraction completed; thorough review required.",
                        merged.extractionSource(), detectedFieldsCount, detectedFieldsCount == 1 ? "" : "s");
                qualityMessage = String.format("Poor Label (%d/12 fields detected) → Partial extraction + Needs Review", detectedFieldsCount);
            }
        }

        boolean isAiEnabled = aiProperties != null && aiProperties.isEnabled();
        String aiModel = (aiResult != null && aiResult.modelName() != null && !aiResult.modelName().isBlank())
                ? aiResult.modelName()
                : (isAiEnabled ? aiProperties.getModel() : null);

        String effectiveFullText = (merged.fullTranscribedText() != null && !merged.fullTranscribedText().isBlank())
                ? merged.fullTranscribedText()
                : (ocrResult != null ? ocrResult.text() : "");

        // 6. Persist completed scan record to database
        Instant createdAt = persistScanRecord(
                scanId,
                storedFilename,
                contentType,
                file.getSize(),
                status,
                effectiveFullText,
                labelData,
                complianceResult,
                merged.extractionSource(),
                merged.extractionStatus(),
                merged.overallConfidence(),
                aiModel,
                merged.fieldEvidence(),
                merged.fieldConfidence()
        );

        log.info("Completed scan analysis for scanId=[{}]: source=[{}], status=[{}], qualityTier=[{}], fieldsDetected=[{}], compliance=[{}] with score [{}]",
                scanId, merged.extractionSource(), merged.extractionStatus(), qualityTier, detectedFieldsCount, complianceResult.overallStatus(), complianceResult.overallScore());

        return new ScanResponse(
                scanId,
                storedFilename,
                contentType,
                file.getSize(),
                status,
                effectiveFullText,
                effectiveFullText,
                ocrResult != null ? ocrResult.language() : (language != null ? language : "eng"),
                message,
                labelData,
                complianceResult,
                createdAt,
                detectedFieldsCount,
                qualityTier,
                complianceOutcome,
                qualityMessage,
                merged.extractionSource(),
                merged.extractionStatus(),
                merged.overallConfidence(),
                isAiEnabled,
                aiModel,
                merged.fieldEvidence(),
                merged.fieldConfidence(),
                merged.fieldBoundingBoxes()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ScanSummaryResponse> getScanHistory(int page, int size) {
        int clampedPage = Math.max(0, page);
        int clampedSize = Math.max(1, Math.min(100, size));

        Pageable pageable = PageRequest.of(clampedPage, clampedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ScanEntity> entityPage = scanRepository.findAll(pageable);

        Page<ScanSummaryResponse> summaryPage = entityPage.map(e -> new ScanSummaryResponse(
                e.getScanId(),
                e.getFilename(),
                e.getProductName() != null ? e.getProductName() : "Unknown / Product not detected",
                e.getBrand(),
                e.getStatus(),
                e.getOverallStatus(),
                e.getOverallScore(),
                e.getSummary(),
                e.getCreatedAt(),
                e.getExtractionStatus() != null ? e.getExtractionStatus() : "AI_SUCCESS",
                e.getExtractionSource() != null ? e.getExtractionSource() : "VISION_AI"
        ));

        return new PageResponse<>(
                summaryPage.getContent(),
                summaryPage.getNumber(),
                summaryPage.getSize(),
                summaryPage.getTotalElements(),
                summaryPage.getTotalPages(),
                summaryPage.isFirst(),
                summaryPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public ScanResponse getScanByScanId(UUID scanId) {
        if (scanId == null) {
            throw new InvalidImageException("Invalid scan ID parameter");
        }

        ScanEntity entity = scanRepository.findByScanId(scanId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan analysis record not found for ID: " + scanId));

        StructuredLabelData labelData = deserializeLabel(entity.getExtractedLabelJson(), entity.getOcrText());
        ComplianceResult complianceResult = deserializeCompliance(entity.getComplianceResultJson(), entity.getOverallStatus(), entity.getOverallScore(), entity.getSummary());

        String message = "ANALYSIS_COMPLETE".equals(entity.getStatus())
                ? "Stored product label analysis retrieved successfully."
                : "Stored scan record retrieved (insufficient text detected in original scan).";

        int detectedFieldsCount = labelData != null ? labelData.countDetectedFields() : 0;
        String qualityTier = labelData != null ? labelData.getQualityTier() : "VERY_POOR_IMAGE";
        String complianceOutcome = labelData != null ? labelData.getComplianceOutcome() : "Retake image";
        String qualityMessage = String.format("%s (%d/12 fields detected) → %s",
                labelData != null ? labelData.getQualityLabel() : "Very Poor Image",
                detectedFieldsCount,
                complianceOutcome);

        Map<String, String> evidenceMap = deserializeMap(entity.getFieldEvidenceJson());
        Map<String, Double> confidenceMap = deserializeDoubleMap(entity.getFieldConfidenceJson());

        boolean isAiEnabled = aiProperties != null && aiProperties.isEnabled();

        return new ScanResponse(
                entity.getScanId(),
                entity.getFilename(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getStatus(),
                entity.getOcrText(),
                entity.getOcrText(),
                "eng",
                message,
                labelData,
                complianceResult,
                entity.getCreatedAt(),
                detectedFieldsCount,
                qualityTier,
                complianceOutcome,
                qualityMessage,
                entity.getExtractionSource() != null ? entity.getExtractionSource() : "VISION_AI",
                entity.getExtractionStatus() != null ? entity.getExtractionStatus() : "AI_SUCCESS",
                entity.getExtractionConfidence() != null ? entity.getExtractionConfidence() : 0.85,
                isAiEnabled,
                entity.getAiModel(),
                evidenceMap,
                confidenceMap
        );
    }

    private Instant persistScanRecord(
            UUID scanId,
            String filename,
            String contentType,
            long sizeBytes,
            String status,
            String ocrText,
            StructuredLabelData labelData,
            ComplianceResult complianceResult,
            String extractionSource,
            String extractionStatus,
            Double extractionConfidence,
            String aiModel,
            Map<String, String> fieldEvidence,
            Map<String, Double> fieldConfidence
    ) {
        String labelJson;
        String complianceJson;
        String evidenceJson = null;
        String confidenceJson = null;
        try {
            labelJson = objectMapper.writeValueAsString(labelData);
            complianceJson = objectMapper.writeValueAsString(complianceResult);
            if (fieldEvidence != null && !fieldEvidence.isEmpty()) {
                evidenceJson = objectMapper.writeValueAsString(fieldEvidence);
            }
            if (fieldConfidence != null && !fieldConfidence.isEmpty()) {
                confidenceJson = objectMapper.writeValueAsString(fieldConfidence);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize scan result to JSON for scanId=[{}]: {}", scanId, e.getMessage(), e);
            throw new FileStorageException("Unable to serialize scan analysis data for storage");
        }

        ScanEntity entity = new ScanEntity(
                scanId,
                filename,
                contentType,
                sizeBytes,
                status,
                ocrText,
                complianceResult.overallStatus(),
                complianceResult.overallScore(),
                complianceResult.summary(),
                labelData != null ? labelData.productName() : null,
                labelData != null ? labelData.brand() : null,
                labelData != null ? labelData.netQuantity() : null,
                labelData != null ? labelData.mrp() : null,
                labelJson,
                complianceJson
        );

        entity.setExtractionSource(extractionSource);
        entity.setExtractionStatus(extractionStatus);
        entity.setExtractionConfidence(extractionConfidence);
        entity.setAiModel(aiModel);
        entity.setFieldEvidenceJson(evidenceJson);
        entity.setFieldConfidenceJson(confidenceJson);

        try {
            ScanEntity saved = scanRepository.save(entity);
            return saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now();
        } catch (Exception e) {
            log.error("Failed to persist ScanEntity for scanId=[{}]: {}", scanId, e.getMessage(), e);
            throw new FileStorageException("Database error saving scan analysis record");
        }
    }

    private StructuredLabelData deserializeLabel(String json, String ocrText) {
        if (json == null || json.isBlank()) {
            return new StructuredLabelData(
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    ocrText, null, "NOT_DETECTED"
            );
        }
        try {
            return objectMapper.readValue(json, StructuredLabelData.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize ExtractedLabel JSON: {}", e.getMessage());
            return new StructuredLabelData(
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    ocrText, null, "NOT_DETECTED"
            );
        }
    }

    private ComplianceResult deserializeCompliance(String json, RuleStatus overallStatus, int overallScore, String summary) {
        if (json == null || json.isBlank()) {
            return new ComplianceResult(overallStatus != null ? overallStatus : RuleStatus.WARNING, overallScore, List.of(), summary != null ? summary : "");
        }
        try {
            return objectMapper.readValue(json, ComplianceResult.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize ComplianceResult JSON: {}", e.getMessage());
            return new ComplianceResult(overallStatus != null ? overallStatus : RuleStatus.WARNING, overallScore, List.of(), summary != null ? summary : "");
        }
    }

    private Map<String, String> deserializeMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Double> deserializeDoubleMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private void validateImageBytes(String contentType, byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            throw new InvalidImageException("Image file is too small or truncated to be a valid image");
        }

        switch (contentType) {
            case "image/jpeg" -> {
                if (bytes.length < JPEG_MAGIC.length) {
                    throw new InvalidImageException("JPEG image payload is too small or truncated");
                }
                if (!startsWith(bytes, JPEG_MAGIC)) {
                    throw new InvalidImageException("File content does not match JPEG signature");
                }
            }
            case "image/png" -> {
                if (bytes.length < PNG_MAGIC.length) {
                    throw new InvalidImageException("PNG image payload is too small or truncated");
                }
                if (!startsWith(bytes, PNG_MAGIC)) {
                    throw new InvalidImageException("File content does not match PNG signature");
                }
            }
            case "image/webp" -> {
                if (bytes.length < 12) {
                    throw new InvalidImageException("File is too small to be a valid WebP image");
                }
                if (!startsWith(bytes, RIFF_HEADER)) {
                    throw new InvalidImageException("File content does not match WebP RIFF container header");
                }
                byte[] webpCheck = Arrays.copyOfRange(bytes, 8, 12);
                if (!Arrays.equals(webpCheck, WEBP_HEADER)) {
                    throw new InvalidImageException("WebP missing WEBP signature at offset 8");
                }
                if (bytes.length >= 16) {
                    String chunkType = new String(Arrays.copyOfRange(bytes, 12, 16), java.nio.charset.StandardCharsets.US_ASCII);
                    if (!WEBP_CHUNKS.contains(chunkType)) {
                        throw new InvalidImageException("WebP contains unsupported chunk: " + chunkType);
                    }
                }
            }
            default -> throw new InvalidImageException("Unsupported content type for byte validation: " + contentType);
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private String determineCanonicalExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
    }
}
