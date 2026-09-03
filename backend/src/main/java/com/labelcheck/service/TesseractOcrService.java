package com.labelcheck.service;

import com.labelcheck.config.OcrProperties;
import com.labelcheck.dto.OcrResult;
import com.labelcheck.exception.OcrException;
import com.labelcheck.exception.OcrUnavailableException;
import jakarta.annotation.PostConstruct;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Production implementation of OcrService using the local Tess4J / Tesseract engine.
 * Supports multilingual OCR for Indian scripts (Hindi, Tamil, Telugu, Kannada, Malayalam, etc.)
 * with automatic discovery of traineddata files, safe English fallback, dual-pass OCR
 * (PSM 3 + PSM 11), conditional rotation recovery, and Unicode-safe line deduplication.
 */
@Service
public class TesseractOcrService implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcrService.class);

    // Minimum alphanumeric character threshold before considering rotation recovery
    private static final int ROTATION_RECOVERY_THRESHOLD = 10;

    private final OcrProperties ocrProperties;
    private final ImagePreprocessingService imagePreprocessingService;

    private ITesseract tesseract;
    private volatile boolean available = false;
    private final Set<String> installedLanguages = new TreeSet<>();

    public TesseractOcrService(
            OcrProperties ocrProperties,
            ImagePreprocessingService imagePreprocessingService
    ) {
        this.ocrProperties = ocrProperties;
        this.imagePreprocessingService = imagePreprocessingService;
    }

    @PostConstruct
    public void init() {
        if (!ocrProperties.isEnabled()) {
            log.warn("OCR is explicitly disabled via configuration (app.ocr.enabled=false).");
            this.available = false;
            return;
        }

        String rawDatapath = ocrProperties.getTesseract().getDatapath();
        Path datapath = resolveDataPath(rawDatapath);

        // 1. Discover all installed .traineddata files in tessdata directory
        installedLanguages.clear();
        if (Files.exists(datapath)) {
            try (Stream<Path> stream = Files.list(datapath)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".traineddata"))
                        .forEach(p -> {
                            String fn = p.getFileName().toString();
                            String langCode = fn.substring(0, fn.length() - ".traineddata".length());
                            installedLanguages.add(langCode);
                        });
            } catch (Exception e) {
                log.warn("Error scanning tessdata directory for language models: {}", e.getMessage());
            }
        }

        log.info("Discovered [{}] installed Tesseract OCR language models: {}",
                installedLanguages.size(), installedLanguages);

        // 2. Validate mandatory English model exists
        if (!installedLanguages.contains("eng")) {
            log.warn("Mandatory English traineddata (eng.traineddata) not found at: [{}]. " +
                            "OCR operations will fail with a controlled OcrUnavailableException until traineddata is provided.",
                    datapath.resolve("eng.traineddata").toAbsolutePath());
            this.available = false;
            return;
        }

        // 3. Log diagnostic warnings for any requested languages that are missing
        for (String lang : ocrProperties.getSupportedLanguages()) {
            if (!installedLanguages.contains(lang)) {
                log.warn("Language traineddata not available: [{}]", lang);
            }
        }

        try {
            Tesseract instance = new Tesseract();
            instance.setDatapath(datapath.toAbsolutePath().toString());
            instance.setLanguage(ocrProperties.getDefaultLanguage());
            instance.setPageSegMode(3);

            this.tesseract = instance;
            this.available = true;
            log.info("Tesseract OCR engine initialized successfully. Datapath=[{}], Default Language=[{}]",
                    datapath.toAbsolutePath(), ocrProperties.getDefaultLanguage());
        } catch (Throwable t) {
            log.error("Failed to initialize Tesseract OCR engine: {}", t.getMessage(), t);
            this.available = false;
        }
    }

    @Override
    public OcrResult extractText(Path imagePath) {
        return extractText(imagePath, ocrProperties.getDefaultLanguage());
    }

    @Override
    public OcrResult extractText(Path imagePath, String language) {
        if (!isAvailable()) {
            throw new OcrUnavailableException("OCR text extraction engine is not available on the server");
        }

        String resolvedLanguage = resolveLanguage(language);
        log.debug("OCR session requested with language=[{}], resolved to=[{}]", language, resolvedLanguage);

        Path tempOcrFile = null;
        File fileToScan;

        // 1. Image Preprocessing with fallback to original image
        try {
            tempOcrFile = imagePreprocessingService.preprocessToTempFile(imagePath);
            fileToScan = tempOcrFile.toFile();
            log.debug("Using preprocessed temporary image [{}] for OCR.", tempOcrFile.getFileName());
        } catch (Throwable t) {
            log.warn("Preprocessing failed for [{}], falling back directly to original image: {}",
                    imagePath.getFileName(), t.getMessage());
            fileToScan = imagePath.toFile();
        }

        try {
            // 2. Perform dual-pass OCR (PSM 3 + PSM 11) with fallback to English if multilingual fails
            String ocrText;
            try {
                ocrText = executeDualPassOcr(fileToScan, resolvedLanguage);
                if (tempOcrFile != null && !tempOcrFile.equals(imagePath)) {
                    try {
                        String rawText = executeDualPassOcr(imagePath.toFile(), resolvedLanguage);
                        ocrText = mergeOcrPasses(ocrText, rawText);
                    } catch (Exception e) {
                        log.debug("Raw image pass fusion skipped: {}", e.getMessage());
                    }
                }
            } catch (TesseractException te) {
                if (!"eng".equals(resolvedLanguage)) {
                    log.warn("Multilingual OCR with [{}] failed on [{}], falling back to 'eng': {}",
                            resolvedLanguage, imagePath.getFileName(), te.getMessage());
                    resolvedLanguage = "eng";
                    ocrText = executeDualPassOcr(fileToScan, resolvedLanguage);
                } else {
                    throw te;
                }
            }

            // 3. Conditional Rotation Recovery: only trigger if output is poor/sparse
            if (countAlphanumeric(ocrText) < ROTATION_RECOVERY_THRESHOLD) {
                String recoveredText = attemptRotationRecovery(fileToScan, ocrText, resolvedLanguage);
                if (countAlphanumeric(recoveredText) > countAlphanumeric(ocrText)) {
                    log.info("Rotation recovery improved OCR for [{}]: chars [{}] -> [{}]",
                            imagePath.getFileName(), countAlphanumeric(ocrText), countAlphanumeric(recoveredText));
                    ocrText = recoveredText;
                }
            }

            if (ocrText.isBlank() || isOnlyNoise(ocrText)) {
                log.info("OCR completed with NO detectable text for [{}]. Returning OCR_NO_TEXT.", imagePath.getFileName());
                return new OcrResult("OCR_NO_TEXT", "", resolvedLanguage);
            }

            log.info("OCR completed successfully for [{}] in language [{}]. Extracted [{}] characters.",
                    imagePath.getFileName(), resolvedLanguage, ocrText.length());
            return new OcrResult("OCR_COMPLETE", ocrText, resolvedLanguage);

        } catch (TesseractException te) {
            log.warn("Tesseract OCR native reading for [{}] yielded no readable text: {}",
                    imagePath.getFileName(), te.getMessage());
            return new OcrResult("OCR_NO_TEXT", "", resolvedLanguage);
        } catch (Exception e) {
            log.warn("Unexpected error during OCR processing for [{}]: {}", imagePath.getFileName(), e.getMessage());
            return new OcrResult("OCR_NO_TEXT", "", resolvedLanguage);
        } finally {

            // 4. Guaranteed temporary file cleanup
            if (tempOcrFile != null) {
                try {
                    Files.deleteIfExists(tempOcrFile);
                    log.debug("Deleted temporary OCR file [{}]", tempOcrFile.getFileName());
                } catch (IOException e) {
                    log.warn("Failed to delete temporary OCR file [{}]: {}", tempOcrFile, e.getMessage());
                }
            }
        }
    }

    @Override
    public OcrResult extractText(BufferedImage image) {
        return extractText(image, ocrProperties.getDefaultLanguage());
    }

    @Override
    public OcrResult extractText(BufferedImage image, String language) {
        if (!isAvailable()) {
            throw new OcrUnavailableException("OCR text extraction engine is not available on the server");
        }

        if (image == null) {
            throw new OcrException("Input image cannot be null for OCR extraction");
        }

        String resolvedLanguage = resolveLanguage(language);

        try {
            BufferedImage processed;
            try {
                processed = imagePreprocessingService.preprocessForOcr(image);
            } catch (Throwable t) {
                log.warn("In-memory preprocessing failed, falling back to original: {}", t.getMessage());
                processed = image;
            }

            String ocrText;
            try {
                ocrText = executeDualPassOcr(processed, resolvedLanguage);
            } catch (TesseractException te) {
                if (!"eng".equals(resolvedLanguage)) {
                    log.warn("Multilingual OCR with [{}] failed in-memory, falling back to 'eng': {}",
                            resolvedLanguage, te.getMessage());
                    resolvedLanguage = "eng";
                    ocrText = executeDualPassOcr(processed, resolvedLanguage);
                } else {
                    throw te;
                }
            }

            // Conditional Rotation Recovery
            if (countAlphanumeric(ocrText) < ROTATION_RECOVERY_THRESHOLD) {
                String recoveredText = attemptRotationRecovery(processed, ocrText, resolvedLanguage);
                if (countAlphanumeric(recoveredText) > countAlphanumeric(ocrText)) {
                    ocrText = recoveredText;
                }
            }

            if (ocrText.isBlank() || isOnlyNoise(ocrText)) {
                log.info("OCR completed with NO detectable text. Returning OCR_NO_TEXT.");
                return new OcrResult("OCR_NO_TEXT", "", resolvedLanguage);
            }

            log.info("OCR completed successfully in language [{}]. Extracted [{}] characters.",
                    resolvedLanguage, ocrText.length());
            return new OcrResult("OCR_COMPLETE", ocrText, resolvedLanguage);
        } catch (TesseractException te) {
            log.warn("Tesseract OCR native error during in-memory text extraction: {}", te.getMessage());
            return new OcrResult("OCR_NO_TEXT", "", resolvedLanguage);
        } catch (Exception e) {
            log.warn("Unexpected error during in-memory OCR extraction: {}", e.getMessage());
            return new OcrResult("OCR_NO_TEXT", "", resolvedLanguage);
        }
    }

    /**
     * Resolves a requested language string against locally installed traineddata models.
     * Supports single codes (e.g. "hin") and plus-separated combinations (e.g. "eng+hin").
     * Safely falls back to "eng" if requested models are unavailable.
     */
    public String resolveLanguage(String requested) {
        if (requested == null || requested.isBlank()) {
            return ocrProperties.getDefaultLanguage();
        }

        // Normalize URL-decoded spaces or commas (e.g. "eng hin" or "eng,hin" -> "eng+hin")
        String normalized = requested.trim().replace(' ', '+').replace(',', '+');

        // Plus-separated combinations (e.g. "eng+hin")
        if (normalized.contains("+")) {
            String[] parts = normalized.split("\\+");
            List<String> validParts = new ArrayList<>();
            for (String part : parts) {
                String trimmed = part.trim().toLowerCase(Locale.ROOT);

                if (installedLanguages.contains(trimmed)) {
                    if (!validParts.contains(trimmed)) {
                        validParts.add(trimmed);
                    }
                } else {
                    log.warn("Language traineddata not available: [{}], omitting from OCR session", trimmed);
                }
            }
            if (validParts.isEmpty()) {
                log.warn("None of requested languages [{}] are installed. Falling back to English ('eng').", requested);
                return "eng";
            }
            return String.join("+", validParts);
        }

        // Single language code
        String trimmed = normalized.trim().toLowerCase(Locale.ROOT);
        if (installedLanguages.contains(trimmed)) {
            return trimmed;
        }


        log.warn("Language traineddata not available: [{}]. Falling back safely to 'eng'.", trimmed);
        return "eng";
    }

    /**
     * Executes dual-pass OCR on a file:
     * Pass 1: PSM 3 (Automatic segmentation for continuous text blocks)
     * Pass 2: PSM 11 (Sparse text mode for isolated statutory markers)
     */
    private String executeDualPassOcr(File file, String language) throws TesseractException {
        String pass1Text = runTesseractPass(file, 3, language);
        String pass2Text = runTesseractPass(file, 11, language);
        return mergeOcrPasses(pass1Text, pass2Text);
    }

    /**
     * Executes dual-pass OCR on a BufferedImage.
     */
    private String executeDualPassOcr(BufferedImage image, String language) throws TesseractException {
        String pass1Text = runTesseractPass(image, 3, language);
        String pass2Text = runTesseractPass(image, 11, language);
        return mergeOcrPasses(pass1Text, pass2Text);
    }

    private synchronized String runTesseractPass(File file, int psm, String language) throws TesseractException {
        tesseract.setLanguage(language);
        tesseract.setPageSegMode(psm);
        String text = tesseract.doOCR(file);
        return text != null ? text.trim() : "";
    }

    private synchronized String runTesseractPass(BufferedImage image, int psm, String language) throws TesseractException {
        tesseract.setLanguage(language);
        tesseract.setPageSegMode(psm);
        String text = tesseract.doOCR(image);
        return text != null ? text.trim() : "";
    }

    /**
     * Combines continuous text (Pass 1) and sparse text (Pass 2) while eliminating redundant lines.
     * Preserves Unicode scripts (Devanagari, Tamil, Telugu, etc.) during deduplication.
     */
    public String mergeOcrPasses(String pass1, String pass2) {
        if (pass1 == null || pass1.isBlank()) {
            return pass2 != null ? pass2.trim() : "";
        }
        if (pass2 == null || pass2.isBlank()) {
            return pass1.trim();
        }

        List<String> mergedLines = new ArrayList<>();
        Set<String> normalizedSet = new LinkedHashSet<>();

        for (String line : pass1.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                String normalized = normalizeForDeduplication(trimmed);
                if (!normalized.isEmpty() && normalizedSet.add(normalized)) {
                    mergedLines.add(trimmed);
                }
            }
        }

        for (String line : pass2.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                String normalized = normalizeForDeduplication(trimmed);
                // Only append sparse lines that contribute new information
                if (!normalized.isEmpty() && countAlphanumeric(normalized) >= 2 && normalizedSet.add(normalized)) {
                    mergedLines.add(trimmed);
                }
            }
        }

        return String.join("\n", mergedLines).trim();
    }

    /**
     * Attempt rotation recovery (90, 180, 270) only when initial OCR is sparse or poor.
     */
    private String attemptRotationRecovery(File file, String currentBest, String language) {
        try {
            BufferedImage original = ImageIO.read(file);
            if (original == null) return currentBest;
            return attemptRotationRecovery(original, currentBest, language);
        } catch (Exception e) {
            log.debug("Rotation recovery reading failed: {}", e.getMessage());
            return currentBest;
        }
    }

    private String attemptRotationRecovery(BufferedImage original, String currentBest, String language) {
        String bestText = currentBest;
        int bestScore = countAlphanumeric(currentBest);

        int[] rotationAngles = {90, 180, 270};
        for (int angle : rotationAngles) {
            try {
                BufferedImage rotated = imagePreprocessingService.rotateImage(original, angle);
                String rotatedText = executeDualPassOcr(rotated, language);
                int score = countAlphanumeric(rotatedText);
                if (score > bestScore + 4 || (bestScore == 0 && score > 0)) {
                    bestScore = score;
                    bestText = rotatedText;
                    // If a rotation gives confident signal (> 30 alphanumeric chars), stop early
                    if (bestScore > 30) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.debug("Rotation attempt at [{}°] failed: {}", angle, e.getMessage());
            }
        }

        return bestText;
    }

    /**
     * Normalizes a line for deduplication while strictly preserving Indian-script Unicode characters.
     * Removes punctuation and whitespace, but retains all letters and digits across all alphabets.
     */
    private String normalizeForDeduplication(String line) {
        return line.toLowerCase(Locale.ROOT).replaceAll("[\\p{Punct}\\s]", "");
    }

    /**
     * Counts alphanumeric characters across all Unicode blocks (Latin, Devanagari, Tamil, Telugu, etc.).
     */
    public int countAlphanumeric(String text) {
        if (text == null || text.isBlank()) return 0;
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean isAvailable() {
        return available && tesseract != null;
    }

    @Override
    public Set<String> getSupportedLanguages() {
        return Collections.unmodifiableSet(installedLanguages);
    }

    public Set<String> getInstalledLanguages() {
        return Collections.unmodifiableSet(installedLanguages);
    }


    public boolean isLanguageAvailable(String lang) {
        return lang != null && installedLanguages.contains(lang.trim().toLowerCase(Locale.ROOT));
    }

    private Path resolveDataPath(String rawDatapath) {
        Path direct = Paths.get(rawDatapath).toAbsolutePath().normalize();
        if (Files.exists(direct)) {
            return direct;
        }

        Path fallback = Paths.get("tessdata").toAbsolutePath().normalize();
        if (Files.exists(fallback)) {
            return fallback;
        }

        return direct;
    }

    private boolean isOnlyNoise(String text) {
        return countAlphanumeric(text) == 0;
    }
}
