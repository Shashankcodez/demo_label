package com.labelcheck.service;

import com.labelcheck.config.OcrProperties;
import com.labelcheck.dto.OcrResult;
import com.labelcheck.dto.StructuredLabelData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies real-world OCR accuracy across controlled test scenarios:
 * - Image A: MRP + quantity + FSSAI + manufacturer + date
 * - Image B: Small statutory text (6pt/8pt equivalent)
 * - Image C: Multiple scattered text blocks / packaging layout
 * - Image D: Rotated text with automatic orientation recovery
 * - Temporary file lifecycle & cleanup
 */
class RealWorldOcrAccuracyTest {

    private ImagePreprocessingService preprocessingService;
    private TesseractOcrService ocrService;
    private LabelExtractionService extractionService;

    @BeforeEach
    void setUp() {
        OcrProperties properties = new OcrProperties();
        properties.setEnabled(true);
        properties.getTesseract().setDatapath("tessdata");
        properties.getTesseract().setLanguage("eng");

        preprocessingService = new ImagePreprocessingService();
        ocrService = new TesseractOcrService(properties, preprocessingService);
        ocrService.init();

        extractionService = new LabelExtractionService();
    }

    @Test
    @DisplayName("Image A: Extract complete statutory fields (MRP, Net Qty, FSSAI, Manufacturer, Date)")
    void imageA_statutoryLabel_allFieldsRecognized(@TempDir Path tempDir) throws IOException {
        if (!ocrService.isAvailable()) return;

        BufferedImage imgA = new BufferedImage(800, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imgA.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 800, 300);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString("MRP Rs 125.00 (incl. of all taxes)", 30, 50);
        g.drawString("Net Quantity: 250 g", 30, 95);
        g.drawString("FSSAI Lic. No. 10021011000456", 30, 140);
        g.drawString("Manufactured by: Sunrise Agro Foods Pvt Ltd", 30, 185);
        g.drawString("PKD: 08/2026", 30, 230);
        g.drawString("Customer Helpline: 1800-111-2233", 30, 275);
        g.dispose();

        Path imagePath = tempDir.resolve("image_a.png");
        ImageIO.write(imgA, "PNG", imagePath.toFile());

        OcrResult ocrResult = ocrService.extractText(imagePath);
        assertThat(ocrResult.status()).isEqualTo("OCR_COMPLETE");

        StructuredLabelData extracted = extractionService.extract(ocrResult.text());
        assertThat(extracted.mrp()).isEqualTo("125.00");
        assertThat(extracted.mrpInclusiveOfTaxes()).isTrue();
        assertThat(extracted.netQuantity()).isEqualTo("250 g");
        assertThat(extracted.fssaiLicenseNumber()).isEqualTo("10021011000456");
        assertThat(extracted.manufactureOrPackingDate()).isEqualTo("08/2026");
        assertThat(extracted.manufacturerName()).contains("Sunrise Agro Foods");
        assertThat(extracted.customerCarePhone()).isEqualTo("1800-111-2233");
    }

    @Test
    @DisplayName("Image B: Small statutory text is upscaled and recognized")
    void imageB_smallPrintedText_recognizedSuccessfully(@TempDir Path tempDir) throws IOException {
        if (!ocrService.isAvailable()) return;

        // Render at small dimensions (font size 12px) to test smart upscaling
        BufferedImage imgB = new BufferedImage(450, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imgB.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 450, 150);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Net Wt 150g", 20, 40);
        g.drawString("M.R.P. Rs. 45.00", 20, 80);
        g.drawString("FSSAI Lic No: 10012345678901", 20, 120);
        g.dispose();

        Path imagePath = tempDir.resolve("image_b.png");
        ImageIO.write(imgB, "PNG", imagePath.toFile());

        OcrResult ocrResult = ocrService.extractText(imagePath);
        assertThat(ocrResult.status()).isEqualTo("OCR_COMPLETE");

        StructuredLabelData extracted = extractionService.extract(ocrResult.text());
        assertThat(extracted.mrp()).isEqualTo("45.00");
        assertThat(extracted.netQuantity()).isEqualTo("150g");
        assertThat(extracted.fssaiLicenseNumber()).isEqualTo("10012345678901");
    }

    @Test
    @DisplayName("Image C: Multiple text blocks and columns captured by dual-pass OCR")
    void imageC_multipleTextBlocks_capturedByDualPass(@TempDir Path tempDir) throws IOException {
        if (!ocrService.isAvailable()) return;

        BufferedImage imgC = new BufferedImage(900, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imgC.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 900, 300);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 20));

        // Left Column: Brand, Product, Net Qty
        g.drawString("PREMIUM HARVEST", 30, 60);
        g.drawString("Almond Cookies", 30, 110);
        g.drawString("Net Weight: 500 g", 30, 160);

        // Right Column: Statutory Declarations
        g.drawString("MRP ₹ 240.00", 520, 60);
        g.drawString("MFD: 15/09/2026", 520, 110);
        g.drawString("FSSAI Lic. No. 12221999000123", 520, 160);

        // Bottom row
        g.drawString("Manufactured by: Harvest Agro Industries Ltd", 30, 240);
        g.dispose();

        Path imagePath = tempDir.resolve("image_c.png");
        ImageIO.write(imgC, "PNG", imagePath.toFile());

        OcrResult ocrResult = ocrService.extractText(imagePath);
        assertThat(ocrResult.status()).isEqualTo("OCR_COMPLETE");

        StructuredLabelData extracted = extractionService.extract(ocrResult.text());




        assertThat(extracted.mrp()).isEqualTo("240.00");
        assertThat(extracted.netQuantity()).isEqualTo("500 g");
        assertThat(extracted.fssaiLicenseNumber()).isEqualTo("12221999000123");
        assertThat(extracted.manufactureOrPackingDate()).isEqualTo("15/09/2026");
        assertThat(extracted.manufacturerName()).contains("Harvest Agro Industries");
    }

    @Test
    @DisplayName("Image D: 90-degree rotated packaging text is recovered")
    void imageD_rotatedText_orientationRecoveryRecoversText(@TempDir Path tempDir) throws IOException {
        if (!ocrService.isAvailable()) return;

        // Create upright image
        BufferedImage upright = new BufferedImage(600, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = upright.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 600, 150);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("MRP Rs 75.00 Net Qty 200g", 30, 80);
        g.dispose();

        // Rotate by 90 degrees clockwise
        BufferedImage rotated = preprocessingService.rotateImage(upright, 90);

        Path imagePath = tempDir.resolve("image_d_rotated.png");
        ImageIO.write(rotated, "PNG", imagePath.toFile());

        OcrResult ocrResult = ocrService.extractText(imagePath);
        assertThat(ocrResult.status()).isEqualTo("OCR_COMPLETE");
        assertThat(ocrResult.text()).containsIgnoringCase("MRP");
        assertThat(ocrResult.text()).contains("75");

        StructuredLabelData extracted = extractionService.extract(ocrResult.text());
        assertThat(extracted.mrp()).isEqualTo("75.00");
        assertThat(extracted.netQuantity()).contains("200g");
    }

    @Test
    @DisplayName("Verify temporary OCR files are deleted after extraction")
    void temporaryFiles_cleanedUpAfterExtraction(@TempDir Path tempDir) throws IOException {
        if (!ocrService.isAvailable()) return;

        BufferedImage img = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 300, 100);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("MRP Rs 50", 20, 60);
        g.dispose();

        Path sourcePath = tempDir.resolve("test_label.png");
        ImageIO.write(img, "PNG", sourcePath.toFile());

        // Perform OCR
        ocrService.extractText(sourcePath);

        // Source file must still exist
        assertThat(Files.exists(sourcePath)).isTrue();

        // Check that no temporary files prefixed with labelcheck_ocr_ remain in temp directory
        Path systemTempDir = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> tempFiles = Files.list(systemTempDir)) {
            long remainingFiles = tempFiles
                    .filter(p -> p.getFileName().toString().startsWith("labelcheck_ocr_"))
                    .count();
            assertThat(remainingFiles).isEqualTo(0L);
        }
    }
}
