package com.labelcheck.dto;

/**
 * Represents an individual word recognized by OCR along with its confidence and bounding box coordinates.
 *
 * @param text       the recognized word text
 * @param confidence recognition confidence percentage (0.0 to 100.0)
 * @param x          horizontal coordinate of top-left corner
 * @param y          vertical coordinate of top-left corner
 * @param width      word bounding box width in pixels
 * @param height     word bounding box height in pixels
 */
public record OcrWord(
        String text,
        float confidence,
        int x,
        int y,
        int width,
        int height
) {
}
