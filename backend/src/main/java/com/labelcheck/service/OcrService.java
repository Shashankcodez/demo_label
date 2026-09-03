package com.labelcheck.service;

import com.labelcheck.dto.OcrResult;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * Interface contract for local OCR text extraction from packaged product label images.
 */
public interface OcrService {

    /**
     * Extracts text from an image file located on the server filesystem.
     *
     * @param imagePath the path to the stored image file
     * @return OcrResult containing status, raw extracted text, and language code
     */
    OcrResult extractText(Path imagePath);

    /**
     * Extracts text from an image file using the specified language or language combination (e.g. "eng+hin").
     *
     * @param imagePath the path to the stored image file
     * @param language the requested language code or combined string
     * @return OcrResult containing status, raw extracted text, and language code
     */
    default OcrResult extractText(Path imagePath, String language) {
        return extractText(imagePath);
    }

    /**
     * Extracts text directly from an in-memory BufferedImage.
     *
     * @param image the BufferedImage to extract text from
     * @return OcrResult containing status, raw extracted text, and language code
     */
    OcrResult extractText(BufferedImage image);

    /**
     * Extracts text directly from an in-memory BufferedImage using the specified language.
     *
     * @param image the BufferedImage to extract text from
     * @param language the requested language code or combined string
     * @return OcrResult containing status, raw extracted text, and language code
     */
    default OcrResult extractText(BufferedImage image, String language) {
        return extractText(image);
    }


    /**
     * Checks whether the OCR engine and required training data files are available.
     *
     * @return true if OCR is ready to process requests
     */
    boolean isAvailable();

    /**
     * Returns the set of installed and supported language codes for OCR.
     *
     * @return set of language codes (e.g. ["eng", "hin", "tam", ...])
     */
    default java.util.Set<String> getSupportedLanguages() {
        return java.util.Set.of("eng");
    }
}


