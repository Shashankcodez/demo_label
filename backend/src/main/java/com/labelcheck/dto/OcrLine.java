package com.labelcheck.dto;

import java.util.List;

/**
 * Represents a horizontally aligned line of OCR words within the same vertical band.
 *
 * @param words    ordered list of words in this line
 * @param minX     leftmost coordinate of the line
 * @param minY     topmost coordinate of the line
 * @param maxX     rightmost coordinate of the line
 * @param maxY     bottommost coordinate of the line
 * @param text     concatenated text of all words in this line
 */
public record OcrLine(
        List<OcrWord> words,
        int minX,
        int minY,
        int maxX,
        int maxY,
        String text
) {
    public boolean containsIgnoreCase(String target) {
        return text != null && text.toLowerCase().contains(target.toLowerCase());
    }

    public int height() {
        return Math.max(1, maxY - minY);
    }
}
