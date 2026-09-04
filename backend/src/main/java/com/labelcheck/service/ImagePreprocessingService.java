package com.labelcheck.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferByte;
import java.awt.image.Kernel;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Preprocesses product label images for optimal Tesseract OCR text extraction.
 * Performs EXIF orientation correction, smart dimensional rescaling,
 * high-fidelity grayscale conversion, auto-levels contrast normalization,
 * and edge sharpening.
 */
@Service
public class ImagePreprocessingService {

    private static final Logger log = LoggerFactory.getLogger(ImagePreprocessingService.class);

    private static final int MIN_TARGET_DIMENSION = 900;
    private static final int MAX_TARGET_DIMENSION = 2500;

    /**
     * Preprocesses an image from a file path into a temporary file optimized for OCR.
     * Guaranteed not to modify the source file.
     *
     * @param sourcePath the path to the original uploaded image file
     * @return Path to the temporary preprocessed PNG image
     * @throws IOException if reading the original or writing the temporary file fails
     */
    public Path preprocessToTempFile(Path sourcePath) throws IOException {
        if (sourcePath == null || !Files.exists(sourcePath)) {
            throw new IllegalArgumentException("Source image path cannot be null or non-existent");
        }

        BufferedImage original = ImageIO.read(sourcePath.toFile());
        if (original == null) {
            throw new IOException("Failed to decode image from " + sourcePath);
        }

        // 1. Correct EXIF orientation if JPEG
        int orientation = readExifOrientation(sourcePath);
        BufferedImage oriented = correctOrientation(original, orientation);

        // 2. Preprocess raster
        BufferedImage processed = preprocessForOcr(oriented);

        // 3. Write to temporary file
        Path tempFile = Files.createTempFile("labelcheck_ocr_", ".png");
        ImageIO.write(processed, "PNG", tempFile.toFile());

        log.debug("Created preprocessed OCR temp file at [{}] ({}x{}) from [{}]",
                tempFile.toAbsolutePath(), processed.getWidth(), processed.getHeight(), sourcePath.getFileName());

        return tempFile;
    }

    /**
     * Preprocesses a BufferedImage for OCR:
     * 1. Smart scaling (upscaling small statutory text, downscaling huge smartphone photos).
     * 2. High-fidelity grayscale conversion.
     * 3. Auto-levels histogram contrast stretching with outlier clipping.
     * 4. Mild unsharp edge sharpening.
     *
     * @param original the input image
     * @return preprocessed BufferedImage optimized for OCR recognition
     */
    public BufferedImage preprocessForOcr(BufferedImage original) {
        if (original == null) {
            return null;
        }

        int width = original.getWidth();
        int height = original.getHeight();

        // 1. Calculate smart scaling factor
        double scale = 1.0;
        int maxDim = Math.max(width, height);
        int minDim = Math.min(width, height);

        if (maxDim > MAX_TARGET_DIMENSION) {
            // Downscale massive smartphone camera captures to fit Tesseract's optimal font range
            scale = (double) MAX_TARGET_DIMENSION / maxDim;
        } else if (minDim < MIN_TARGET_DIMENSION) {
            // Upscale small labels so 6pt statutory fonts have sufficient pixel height
            scale = (double) MIN_TARGET_DIMENSION / minDim;
            scale = Math.min(scale, 2.5); // Cap at 2.5x to preserve memory
        }

        int targetWidth = (int) Math.round(width * scale);
        int targetHeight = (int) Math.round(height * scale);

        // 2. High quality grayscale rendering
        BufferedImage grayscale = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayscale.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        // 3. Auto-levels contrast normalization
        BufferedImage contrastEnhanced = autoLevels(grayscale);

        // 4. Edge sharpening
        return sharpen(contrastEnhanced);
    }

    /**
     * Normalizes image contrast across the full 0-255 dynamic range
     * by stretching the histogram between the 1st and 99th percentiles.
     */
    public BufferedImage autoLevels(BufferedImage gray) {
        int w = gray.getWidth();
        int h = gray.getHeight();
        byte[] data = ((DataBufferByte) gray.getRaster().getDataBuffer()).getData();

        // Compute 256-bin histogram
        int[] histogram = new int[256];
        for (byte b : data) {
            histogram[b & 0xFF]++;
        }

        int totalPixels = w * h;
        int lowerClip = (int) (totalPixels * 0.01);
        int upperClip = (int) (totalPixels * 0.99);

        int count = 0;
        int minVal = 0;
        for (int i = 0; i < 256; i++) {
            count += histogram[i];
            if (count >= lowerClip) {
                minVal = i;
                break;
            }
        }

        count = 0;
        int maxVal = 255;
        for (int i = 255; i >= 0; i--) {
            count += histogram[i];
            if (count >= (totalPixels - upperClip)) {
                maxVal = i;
                break;
            }
        }

        if (maxVal <= minVal) {
            return gray;
        }

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        byte[] outData = ((DataBufferByte) result.getRaster().getDataBuffer()).getData();
        float factor = 255.0f / (maxVal - minVal);

        for (int i = 0; i < data.length; i++) {
            int val = data[i] & 0xFF;
            int stretched = Math.round((val - minVal) * factor);
            outData[i] = (byte) Math.min(255, Math.max(0, stretched));
        }

        return result;
    }

    /**
     * Applies a gentle 3x3 unsharp mask convolution filter to define character edges
     * without introducing ringing artifacts or fracturing thin strokes.
     */
    public BufferedImage sharpen(BufferedImage gray) {
        try {
            float[] kernel = {
                     0.0f, -0.1f,  0.0f,
                    -0.1f,  1.4f, -0.1f,
                     0.0f, -0.1f,  0.0f
            };
            ConvolveOp op = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
            BufferedImage sharpened = new BufferedImage(gray.getWidth(), gray.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            return op.filter(gray, sharpened);
        } catch (Exception e) {
            log.debug("Sharpening filter skipped: {}", e.getMessage());
            return gray;
        }
    }

    /**
     * Rotates a BufferedImage by the specified angle (90, 180, or 270 degrees).
     */
    public BufferedImage rotateImage(BufferedImage img, int angle) {
        if (img == null || angle % 360 == 0) {
            return img;
        }

        int normalizedAngle = ((angle % 360) + 360) % 360;
        int w = img.getWidth();
        int h = img.getHeight();

        int newW = (normalizedAngle == 90 || normalizedAngle == 270) ? h : w;
        int newH = (normalizedAngle == 90 || normalizedAngle == 270) ? w : h;

        int imageType = img.getType() == 0 ? BufferedImage.TYPE_INT_RGB : img.getType();
        BufferedImage rotated = new BufferedImage(newW, newH, imageType);
        Graphics2D g2d = rotated.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.translate(newW / 2.0, newH / 2.0);
            g2d.rotate(Math.toRadians(normalizedAngle));
            g2d.translate(-w / 2.0, -h / 2.0);
            g2d.drawImage(img, 0, 0, null);
        } finally {
            g2d.dispose();
        }

        return rotated;

    }

    /**
     * Reads the standard EXIF orientation tag from a JPEG file without heavy external dependencies.
     * Returns 1 if no EXIF tag is present or if the file is not a JPEG.
     */
    public int readExifOrientation(Path path) {
        if (path == null) return 1;
        File file = path.toFile();
        if (!file.exists() || file.length() < 12) return 1;

        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            // Check JPEG SOI (0xFF, 0xD8)
            if (in.read() != 0xFF || in.read() != 0xD8) {
                return 1;
            }

            while (true) {
                int markerPrefix = in.read();
                if (markerPrefix != 0xFF) break;

                int marker = in.read();
                if (marker == -1 || marker == 0xDA || marker == 0xD9) {
                    // SOS or EOI - stop searching
                    break;
                }

                // Read marker length (2 bytes, big endian)
                int lenHigh = in.read();
                int lenLow = in.read();
                if (lenHigh == -1 || lenLow == -1) break;
                int segmentLength = (lenHigh << 8) | lenLow;
                if (segmentLength < 2) break;
                int dataLength = segmentLength - 2;

                if (marker == 0xE1) {
                    // APP1 marker - might be Exif
                    byte[] data = in.readNBytes(dataLength);
                    if (data.length >= 6 &&
                            data[0] == 'E' && data[1] == 'x' && data[2] == 'i' &&
                            data[3] == 'f' && data[4] == 0 && data[5] == 0) {
                        return parseTiffOrientation(data, 6);
                    }
                } else {
                    // Skip unneeded segment
                    long skipped = in.skip(dataLength);
                    if (skipped < dataLength) {
                        in.readNBytes((int) (dataLength - skipped));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not read EXIF orientation for [{}]: {}", path.getFileName(), e.getMessage());
        }

        return 1;
    }

    private BufferedImage correctOrientation(BufferedImage img, int orientation) {
        return switch (orientation) {
            case 3 -> rotateImage(img, 180);
            case 6 -> rotateImage(img, 90);
            case 8 -> rotateImage(img, 270);
            default -> img;
        };
    }

    private int parseTiffOrientation(byte[] data, int tiffOffset) {
        if (data.length < tiffOffset + 8) return 1;

        boolean littleEndian;
        if (data[tiffOffset] == 'I' && data[tiffOffset + 1] == 'I') {
            littleEndian = true;
        } else if (data[tiffOffset] == 'M' && data[tiffOffset + 1] == 'M') {
            littleEndian = false;
        } else {
            return 1;
        }

        int ifdOffset = readInt32(data, tiffOffset + 4, littleEndian);
        int currentPos = tiffOffset + ifdOffset;
        if (currentPos + 2 > data.length) return 1;

        int numEntries = readInt16(data, currentPos, littleEndian);
        currentPos += 2;

        for (int i = 0; i < numEntries; i++) {
            if (currentPos + 12 > data.length) break;
            int tag = readInt16(data, currentPos, littleEndian);
            if (tag == 0x0112) { // Orientation tag
                return readInt16(data, currentPos + 8, littleEndian);
            }
            currentPos += 12;
        }

        return 1;
    }

    private int readInt16(byte[] data, int offset, boolean littleEndian) {
        int b0 = data[offset] & 0xFF;
        int b1 = data[offset + 1] & 0xFF;
        return littleEndian ? (b1 << 8) | b0 : (b0 << 8) | b1;
    }

    private int readInt32(byte[] data, int offset, boolean littleEndian) {
        int b0 = data[offset] & 0xFF;
        int b1 = data[offset + 1] & 0xFF;
        int b2 = data[offset + 2] & 0xFF;
        int b3 = data[offset + 3] & 0xFF;
        return littleEndian
                ? (b3 << 24) | (b2 << 16) | (b1 << 8) | b0
                : (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    public record QualityAssessment(boolean isSuspect, String reason, int width, int height) {}

    /**
     * Performs a lightweight, non-blocking image quality assessment.
     * Identifies extremely small, pitch-black, or completely washed-out images while never rejecting outright.
     */
    public QualityAssessment assessQuality(Path imagePath) {
        if (imagePath == null || !Files.exists(imagePath)) {
            return new QualityAssessment(true, "Image file not found", 0, 0);
        }
        try {
            BufferedImage img = ImageIO.read(imagePath.toFile());
            if (img == null) {
                return new QualityAssessment(true, "Unreadable image format", 0, 0);
            }
            int w = img.getWidth();
            int h = img.getHeight();
            if (w < 80 || h < 80) {
                return new QualityAssessment(true, "Image dimensions too small for legible packaging text (" + w + "x" + h + ")", w, h);
            }

            // Sample pixels to detect pitch-black or completely washed-out images
            long totalBrightness = 0;
            int stepX = Math.max(1, w / 20);
            int stepY = Math.max(1, h / 20);
            int sampleCount = 0;
            for (int y = 0; y < h; y += stepY) {
                for (int x = 0; x < w; x += stepX) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    totalBrightness += (r + g + b) / 3;
                    sampleCount++;
                }
            }

            double avgBrightness = sampleCount > 0 ? (double) totalBrightness / sampleCount : 128.0;
            if (avgBrightness < 12.0) {
                return new QualityAssessment(true, "Image is extremely dark or underexposed", w, h);
            }
            if (avgBrightness > 250.0) {
                return new QualityAssessment(true, "Image is completely washed out / white overexposed", w, h);
            }

            return new QualityAssessment(false, "Sufficient legibility", w, h);
        } catch (Exception e) {
            log.warn("Image quality check failed gracefully: {}", e.getMessage());
            return new QualityAssessment(false, "Quality check skipped", 0, 0);
        }
    }
}

