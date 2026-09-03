package com.labelcheck.dto;

import java.util.Collections;
import java.util.List;

/**
 * Result record produced by the OCR extraction engine.
 *
 * @param status   "OCR_COMPLETE" if readable text was extracted, or "OCR_NO_TEXT" if no text was found
 * @param text     the actual raw text extracted by Tesseract (or empty string if none found)
 * @param language the language model used for extraction (e.g. "eng")
 * @param words    individual OCR words with spatial coordinates and confidence
 */
public record OcrResult(
        String status,
        String text,
        String language,
        List<OcrWord> words
) {
    public OcrResult(String status, String text, String language) {
        this(status, text, language, Collections.emptyList());
    }
}
