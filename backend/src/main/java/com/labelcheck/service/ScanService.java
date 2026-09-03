package com.labelcheck.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceResult;
import com.labelcheck.compliance.ComplianceRuleEngine;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.dto.OcrResult;
import com.labelcheck.dto.PageResponse;
import com.labelcheck.dto.ScanResponse;
import com.labelcheck.dto.ScanSummaryResponse;
import com.labelcheck.dto.StructuredLabelData;
import com.labelcheck.entity.ScanEntity;
import com.labelcheck.exception.FileStorageException;
import com.labelcheck.exception.InvalidImageException;
import com.labelcheck.exception.ResourceNotFoundException;
import com.labelcheck.repository.ScanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Service orchestrating label image validation, secure temporary storage,
 * local Tesseract OCR text extraction, structured entity parsing, statutory compliance evaluation,
 * and database persistence with scan history retrieval.
 */
@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // Magic bytes for standard image formats
    private static final byte[] JPEG_MAGIC_PREFIX = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
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

    public ScanService(
            FileStorageService fileStorageService,
            OcrService ocrService,
            LabelExtractionService labelExtractionService,
            ComplianceRuleEngine complianceRuleEngine,
            ScanRepository scanRepository,
            ObjectMapper objectMapper
    ) {
        this.fileStorageService = fileStorageService;
        this.ocrService = ocrService;
        this.labelExtractionService = labelExtractionService;
        this.complianceRuleEngine = complianceRuleEngine;
        this.scanRepository = scanRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the set of supported language codes available on the server.
     */
    public Set<String> getSupportedLanguages() {
        return ocrService.getSupportedLanguages();
    }

    /**
     * Executes the complete end-to-end processing pipeline using default language (English):
     * Ingestion → Validation → Storage → OCR → Entity Extraction → Compliance Engine → Database Persistence.
     *
     * @param file the multipart file received from client
     * @return ScanResponse containing scanId, extracted text, structured label data, and compliance checks
     */
    @Transactional
    public ScanResponse processUpload(MultipartFile file) {
        return processUpload(file, null);
    }

    /**
     * Executes the complete end-to-end processing pipeline with requested OCR language:
     * Ingestion → Validation → Storage → OCR → Entity Extraction → Compliance Engine → Database Persistence.
     *
     * @param file the multipart file received from client
     * @param language the requested language code or combined string (e.g. "eng+hin")
     * @return ScanResponse containing scanId, extracted text, structured label data, and compliance checks
     */
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

        // Validate actual binary image data
        validateImageBytes(contentType, bytes);

        UUID scanId = UUID.randomUUID();
        String extension = determineCanonicalExtension(contentType);
        String storedFilename = fileStorageService.storeFile(file, scanId, extension);

        Path storedFilePath = fileStorageService.getUploadLocation().resolve(storedFilename);

        // 1. Run local Tesseract OCR on the stored image (multilingual or default English)
        OcrResult ocrResult = (language != null && !language.isBlank())
                ? ocrService.extractText(storedFilePath, language)
                : ocrService.extractText(storedFilePath);

        // 2. Parse structured label declarations from OCR text
        StructuredLabelData labelData = labelExtractionService.extract(ocrResult.text());

        int detectedFieldsCount = labelData != null ? labelData.countDetectedFields() : 0;
        String qualityTier = labelData != null ? labelData.getQualityTier() : "VERY_POOR_IMAGE";
        String complianceOutcome = labelData != null ? labelData.getComplianceOutcome() : "Retake image";
        boolean hasText = "OCR_COMPLETE".equals(ocrResult.status()) && ocrResult.text() != null && !ocrResult.text().isBlank();

        ComplianceResult complianceResult;
        String status;
        String message;
        String qualityMessage;

        // 3. Quality Tier Workflow & NEVER 0 fields blank/useless result
        if (!hasText || detectedFieldsCount == 0) {
            status = "VERY_POOR_IMAGE";
            qualityTier = "VERY_POOR_IMAGE";
            complianceOutcome = "Retake image";
            message = "Image quality is too poor or unreadable to detect statutory declarations (0 fields detected). Retake image required.";
            qualityMessage = "Very Poor Image: No readable packaging text detected. Please retake the photograph.";
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
            // Evaluate statutory packaging compliance rules
            complianceResult = complianceRuleEngine.evaluate(labelData);
            status = "ANALYSIS_COMPLETE";
            if (detectedFieldsCount >= 10) {
                message = String.format("Good Label: %d statutory declarations detected. Full compliance screening completed.", detectedFieldsCount);
                qualityMessage = String.format("Good Label (%d/12 fields detected) → Compliance", detectedFieldsCount);
            } else if (detectedFieldsCount >= 6) {
                message = String.format("Average Label: %d statutory declarations detected. Compliance evaluated with physical review recommended.", detectedFieldsCount);
                qualityMessage = String.format("Average Label (%d/12 fields detected) → Compliance + Needs Review", detectedFieldsCount);
            } else {
                message = String.format("Poor Label: Only %d statutory declaration%s detected. Partial extraction completed; thorough review required.",
                        detectedFieldsCount, detectedFieldsCount == 1 ? "" : "s");
                qualityMessage = String.format("Poor Label (%d/12 fields detected) → Partial extraction + Needs Review", detectedFieldsCount);
            }
        }

        // 4. Persist completed scan result to database
        Instant createdAt = persistScanRecord(
                scanId,
                storedFilename,
                contentType,
                file.getSize(),
                status,
                ocrResult.text(),
                labelData,
                complianceResult
        );

        log.info("Completed scan analysis and persistence for scanId=[{}]: status=[{}], qualityTier=[{}], fieldsDetected=[{}], compliance=[{}] with score [{}]",
                scanId, status, qualityTier, detectedFieldsCount, complianceResult.overallStatus(), complianceResult.overallScore());

        return new ScanResponse(
                scanId,
                storedFilename,
                contentType,
                file.getSize(),
                status,
                ocrResult.text(),
                ocrResult.text(),
                ocrResult.language(),
                message,
                labelData,
                complianceResult,
                createdAt,
                detectedFieldsCount,
                qualityTier,
                complianceOutcome,
                qualityMessage
        );
    }

    /**
     * Retrieves paginated, lightweight history of previous scans ordered newest first.
     *
     * @param page zero-indexed page number (default 0)
     * @param size page size (default 20, max 100)
     * @return PageResponse of ScanSummaryResponse
     */
    @Transactional(readOnly = true)
    public PageResponse<ScanSummaryResponse> getScanHistory(int page, int size) {
        int clampedPage = Math.max(0, page);
        int clampedSize = Math.max(1, Math.min(100, size));

        Pageable pageable = PageRequest.of(clampedPage, clampedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ScanEntity> entityPage = scanRepository.findAll(pageable);

        Page<ScanSummaryResponse> summaryPage = entityPage.map(e -> new ScanSummaryResponse(
                e.getScanId(),
                e.getFilename(),
                e.getProductName(),
                e.getBrand(),
                e.getStatus(),
                e.getOverallStatus(),
                e.getOverallScore(),
                e.getSummary(),
                e.getCreatedAt()
        ));

        return PageResponse.from(summaryPage);
    }

    /**
     * Retrieves full stored scan analysis details by public scanId.
     *
     * @param scanId public UUID
     * @return complete ScanResponse
     * @throws ResourceNotFoundException if scanId does not exist
     */
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
                qualityMessage
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
            ComplianceResult complianceResult
    ) {
        String labelJson;
        String complianceJson;
        try {
            labelJson = objectMapper.writeValueAsString(labelData);
            complianceJson = objectMapper.writeValueAsString(complianceResult);
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

        try {
            ScanEntity saved = scanRepository.save(entity);
            return saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now();
        } catch (Exception e) {
            log.error("Database persistence failed for scanId=[{}]: {}", scanId, e.getMessage(), e);
            throw new FileStorageException("Database persistence failed while saving scan record");
        }
    }

    private StructuredLabelData deserializeLabel(String json, String fallbackOcr) {
        if (json != null && !json.isBlank()) {
            try {
                return objectMapper.readValue(json, StructuredLabelData.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize structured label JSON: {}", e.getMessage());
            }
        }
        return labelExtractionService.extract(fallbackOcr);
    }

    private ComplianceResult deserializeCompliance(String json, com.labelcheck.compliance.RuleStatus status, int score, String summary) {
        if (json != null && !json.isBlank()) {
            try {
                return objectMapper.readValue(json, ComplianceResult.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize compliance result JSON: {}", e.getMessage());
            }
        }
        return new ComplianceResult(status, score, List.of(), summary);
    }

    /**
     * Validates that the uploaded byte array represents a genuine, readable image of the specified MIME type.
     */
    private void validateImageBytes(String contentType, byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            throw new InvalidImageException("Image file is too small or truncated to be a valid image");
        }

        switch (contentType) {
            case "image/jpeg" -> validateJpeg(bytes);
            case "image/png" -> validatePng(bytes);
            case "image/webp" -> validateWebp(bytes);
            default -> throw new InvalidImageException("Unsupported image format: " + contentType);
        }
    }

    private void validateJpeg(byte[] bytes) {
        if (bytes.length < JPEG_MAGIC_PREFIX.length ||
                bytes[0] != JPEG_MAGIC_PREFIX[0] ||
                bytes[1] != JPEG_MAGIC_PREFIX[1] ||
                bytes[2] != JPEG_MAGIC_PREFIX[2]) {
            throw new InvalidImageException("File content does not match JPEG signature");
        }

        decodeWithImageIO(bytes, "JPEG");
    }

    private void validatePng(byte[] bytes) {
        if (bytes.length < PNG_MAGIC.length) {
            throw new InvalidImageException("File content does not match PNG signature");
        }
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (bytes[i] != PNG_MAGIC[i]) {
                throw new InvalidImageException("File content does not match PNG signature");
            }
        }

        decodeWithImageIO(bytes, "PNG");
    }

    /**
     * Validates WebP structural binary format (RIFF container, WEBP signature, and VP8 chunk header).
     */
    private void validateWebp(byte[] bytes) {
        if (bytes.length < 16) {
            throw new InvalidImageException("File content is too small to be a valid WebP image");
        }

        // Check bytes 0..3: "RIFF"
        if (bytes[0] != RIFF_HEADER[0] || bytes[1] != RIFF_HEADER[1] ||
                bytes[2] != RIFF_HEADER[2] || bytes[3] != RIFF_HEADER[3]) {
            throw new InvalidImageException("File content does not match WebP RIFF container header");
        }

        // Check bytes 8..11: "WEBP"
        if (bytes[8] != WEBP_HEADER[0] || bytes[9] != WEBP_HEADER[1] ||
                bytes[10] != WEBP_HEADER[2] || bytes[11] != WEBP_HEADER[3]) {
            throw new InvalidImageException("File content does not match WebP format identifier");
        }

        // Check bytes 12..15: Valid chunk signature ("VP8 ", "VP8L", "VP8X")
        String chunkHeader = new String(Arrays.copyOfRange(bytes, 12, 16));
        if (!WEBP_CHUNKS.contains(chunkHeader)) {
            throw new InvalidImageException("File content does not contain a recognized WebP bitstream chunk (" + chunkHeader + ")");
        }
    }

    /**
     * Decodes the byte stream using Java ImageIO to ensure the pixel raster is uncorrupted and readable.
     */
    private void decodeWithImageIO(byte[] bytes, String formatName) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new InvalidImageException("File content cannot be decoded as a readable " + formatName + " image");
            }
        } catch (IOException e) {
            log.warn("ImageIO encountered decoding error for {}: {}", formatName, e.getMessage());
            throw new InvalidImageException("Corrupted or unreadable " + formatName + " image data");
        }
    }

    private String determineCanonicalExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "img";
        };
    }
}
