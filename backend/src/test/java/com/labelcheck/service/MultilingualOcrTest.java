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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Multilingual OCR functionality for Indian regional languages:
 * - English only (eng)
 * - Hindi + English (eng+hin)
 * - Tamil + English (eng+tam)
 * - Telugu + English (eng+tel)
 * - Kannada + English (eng+kan)
 * - Malayalam + English (eng+mal)
 * - Missing language graceful fallback to English
 * - Mixed-script deterministic statutory extraction
 * - Performance benchmark between English-only and Multilingual OCR
 */
class MultilingualOcrTest {

    private ImagePreprocessingService preprocessingService;
    private TesseractOcrService ocrService;
    private LabelExtractionService extractionService;

    @BeforeEach
    void setUp() {
        OcrProperties properties = new OcrProperties();
        properties.setEnabled(true);
        properties.getTesseract().setDatapath("tessdata");
        properties.setDefaultLanguage("eng");

        preprocessingService = new ImagePreprocessingService();
        ocrService = new TesseractOcrService(properties, preprocessingService);
        ocrService.init();

        extractionService = new LabelExtractionService();
    }

    private BufferedImage createLabelImage(String[] lines, Font font, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.setFont(font);

        int y = 40;
        for (String line : lines) {
            g.drawString(line, 25, y);
            y += 45;
        }
        g.dispose();
        return img;
    }

    @Test
    @DisplayName("0. Discovery: All 11 supported language models are detected in tessdata")
    void discoverLanguages_allElevenPresent() {
        Set<String> installed = ocrService.getInstalledLanguages();
        assertThat(installed).contains("eng", "hin", "tam", "tel", "kan", "mal", "mar", "ben", "guj", "pan", "ori");
        assertThat(ocrService.isLanguageAvailable("hin")).isTrue();
        assertThat(ocrService.isLanguageAvailable("tam")).isTrue();
        assertThat(ocrService.isLanguageAvailable("tel")).isTrue();
    }

    @Test
    @DisplayName("A. English only: Standard English label OCR succeeds")
    void englishOnly_succeeds() {
        if (!ocrService.isAvailable()) return;

        BufferedImage img = createLabelImage(
                new String[]{"MRP Rs 99.00 (incl. of all taxes)", "Net Quantity: 200 g", "FSSAI Lic. No. 10021011000456"},
                new Font("Arial", Font.BOLD, 22), 650, 160
        );

        OcrResult result = ocrService.extractText(img, "eng");
        assertThat(result.status()).isEqualTo("OCR_COMPLETE");
        assertThat(result.language()).isEqualTo("eng");
        assertThat(result.text()).contains("99.00");
        assertThat(result.text()).contains("200 g");

        StructuredLabelData extracted = extractionService.extract(result.text());
        assertThat(extracted.mrp()).isEqualTo("99.00");
        assertThat(extracted.netQuantity()).isEqualTo("200 g");
        assertThat(extracted.fssaiLicenseNumber()).isEqualTo("10021011000456");
    }

    @Test
    @DisplayName("B. Hindi + English: Devanagari text recognized and English statutory markers extracted")
    void hindiAndEnglish_preservesDevanagariAndExtractsEnglishMarkers(@TempDir Path tempDir) throws IOException {
        if (!ocrService.isAvailable()) return;

        // Label with Hindi brand/description and bilingual statutory markers
        BufferedImage img = createLabelImage(
                new String[]{
                        "शुद्ध घी - प्रीमियम क्वालिटी",
                        "MRP Rs 450.00 (सभी कर सहित)",
                        "Net Weight: 500 g",
                        "FSSAI Lic. No. 10012345678901",
                        "निर्माता: सनराइज फूड्स प्राइवेट लिमिटेड"
                },
                new Font("Nirmala UI", Font.BOLD, 22), 750, 250
        );

        Path imgPath = tempDir.resolve("hindi_label.png");
        ImageIO.write(img, "PNG", imgPath.toFile());

        OcrResult result = ocrService.extractText(imgPath, "eng+hin");
        assertThat(result.status()).isEqualTo("OCR_COMPLETE");
        assertThat(result.language()).isEqualTo("eng+hin");

        // Verify Devanagari Unicode characters survive in the raw text
        boolean hasDevanagari = result.text().codePoints().anyMatch(cp -> cp >= 0x0900 && cp <= 0x097F);
        assertThat(hasDevanagari).as("Devanagari Unicode characters should be present in OCR output").isTrue();

        // Verify statutory declarations are extracted cleanly
        StructuredLabelData extracted = extractionService.extract(result.text());
        assertThat(extracted.mrp()).isEqualTo("450.00");
        assertThat(extracted.netQuantity()).isEqualTo("500 g");
        assertThat(extracted.fssaiLicenseNumber()).isEqualTo("10012345678901");
    }

    @Test
    @DisplayName("C. Tamil + English: Tamil script recognized alongside English statutory declarations")
    void tamilAndEnglish_preservesTamilScriptAndExtractsDeclarations(@TempDir Path tempDir) throws IOException {
        if (!ocrService.isAvailable()) return;

        BufferedImage img = createLabelImage(
                new String[]{
                        "சுவையான தேங்காய் பிஸ்கட்",
                        "MRP Rs 35.00",
                        "Net Wt 100g",
                        "FSSAI Lic. No. 12221999000123",
                        "தயாரிப்பாளர்: ஏபிசி கன்ஃபெக்ஷனரி"
                },
                new Font("Nirmala UI", Font.BOLD, 22), 750, 250
        );

        Path imgPath = tempDir.resolve("tamil_label.png");
        ImageIO.write(img, "PNG", imgPath.toFile());

        OcrResult result = ocrService.extractText(imgPath, "eng+tam");
        assertThat(result.status()).isEqualTo("OCR_COMPLETE");
        assertThat(result.language()).isEqualTo("eng+tam");

        // Verify Tamil Unicode characters survive in OCR output
        boolean hasTamil = result.text().codePoints().anyMatch(cp -> cp >= 0x0B80 && cp <= 0x0BFF);
        assertThat(hasTamil).as("Tamil Unicode characters should be present in OCR output").isTrue();

        StructuredLabelData extracted = extractionService.extract(result.text());
        assertThat(extracted.mrp()).isEqualTo("35.00");
        assertThat(extracted.netQuantity()).isEqualTo("100g");
        assertThat(extracted.fssaiLicenseNumber()).isEqualTo("12221999000123");
    }

    @Test
    @DisplayName("D. Telugu + English: Telugu script recognized alongside English statutory declarations")
    void teluguAndEnglish_preservesTeluguScriptAndExtractsDeclarations(@TempDir Path tempDir) throws IOException {
        if (!ocrService.isAvailable()) return;

        BufferedImage img = createLabelImage(
                new String[]{
                        "ప్రీమియం బాదం కుకీలు",
                        "MRP Rs 150.00",
                        "Net Quantity: 250 g",
                        "FSSAI Lic. No. 10021011000456"
                },
                new Font("Nirmala UI", Font.BOLD, 22), 750, 200
        );

        Path imgPath = tempDir.resolve("telugu_label.png");
        ImageIO.write(img, "PNG", imgPath.toFile());

        OcrResult result = ocrService.extractText(imgPath, "eng+tel");
        assertThat(result.status()).isEqualTo("OCR_COMPLETE");
        assertThat(result.language()).isEqualTo("eng+tel");

        // Verify Telugu Unicode characters survive in OCR output
        boolean hasTelugu = result.text().codePoints().anyMatch(cp -> cp >= 0x0C00 && cp <= 0x0C7F);
        assertThat(hasTelugu).as("Telugu Unicode characters should be present in OCR output").isTrue();

        StructuredLabelData extracted = extractionService.extract(result.text());

        assertThat(extracted.mrp()).isEqualTo("150.00");
        assertThat(extracted.fssaiLicenseNumber()).isEqualTo("10021011000456");
    }

    @Test
    @DisplayName("E. Kannada + English: Kannada script recognized")
    void kannadaAndEnglish_preservesKannadaScript(@TempDir Path tempDir) throws IOException {
        if (!ocrService.isAvailable()) return;

        BufferedImage img = createLabelImage(
                new String[]{
                        "ಶುದ್ಧ ಬೆಣ್ಣೆ ಬಿಸ್ಕತ್ತು",
                        "MRP Rs 60.00",
                        "Net Wt 150g"
                },
                new Font("Nirmala UI", Font.BOLD, 22), 650, 160
        );

        Path imgPath = tempDir.resolve("kannada_label.png");
        ImageIO.write(img, "PNG", imgPath.toFile());

        OcrResult result = ocrService.extractText(imgPath, "eng+kan");
        assertThat(result.status()).isEqualTo("OCR_COMPLETE");
        assertThat(result.language()).isEqualTo("eng+kan");

        boolean hasKannada = result.text().codePoints().anyMatch(cp -> cp >= 0x0C80 && cp <= 0x0CFF);
        assertThat(hasKannada).as("Kannada Unicode characters should be present in OCR output").isTrue();

        StructuredLabelData extracted = extractionService.extract(result.text());
        assertThat(extracted.mrp()).isEqualTo("60.00");
        assertThat(extracted.netQuantity()).isEqualTo("150g");
    }

    @Test
    @DisplayName("F. Malayalam + English: Malayalam script recognized")
    void malayalamAndEnglish_preservesMalayalamScript(@TempDir Path tempDir) throws IOException {
        if (!ocrService.isAvailable()) return;

        BufferedImage img = createLabelImage(
                new String[]{
                        "ശുദ്ധ വെളിച്ചെണ്ണ",
                        "MRP Rs 180.00",
                        "Net Volume: 1 L"
                },
                new Font("Nirmala UI", Font.BOLD, 22), 650, 160
        );

        Path imgPath = tempDir.resolve("malayalam_label.png");
        ImageIO.write(img, "PNG", imgPath.toFile());

        OcrResult result = ocrService.extractText(imgPath, "eng+mal");
        assertThat(result.status()).isEqualTo("OCR_COMPLETE");
        assertThat(result.language()).isEqualTo("eng+mal");

        boolean hasMalayalam = result.text().codePoints().anyMatch(cp -> cp >= 0x0D00 && cp <= 0x0D7F);
        assertThat(hasMalayalam).as("Malayalam Unicode characters should be present in OCR output").isTrue();

        StructuredLabelData extracted = extractionService.extract(result.text());
        assertThat(extracted.mrp()).isEqualTo("180.00");
        assertThat(extracted.netQuantity()).isIn("1 L", "1L");
    }


    @Test
    @DisplayName("G. Missing language gracefully falls back to English without crashing")
    void missingLanguage_fallsBackToEnglishWithoutCrashing() {
        if (!ocrService.isAvailable()) return;

        BufferedImage img = createLabelImage(
                new String[]{"MRP Rs 55.00", "Net Quantity 120 g"},
                new Font("Arial", Font.BOLD, 22), 500, 120
        );

        // Request completely fictitious language code
        OcrResult result = ocrService.extractText(img, "xyz");

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("OCR_COMPLETE");
        assertThat(result.language()).isEqualTo("eng");
        assertThat(result.text()).contains("55.00");

        StructuredLabelData extracted = extractionService.extract(result.text());
        assertThat(extracted.mrp()).isEqualTo("55.00");
        assertThat(extracted.netQuantity()).isEqualTo("120 g");
    }

    @Test
    @DisplayName("H. Performance benchmark: English-only versus Multilingual (eng+hin) latency")
    void benchmarkLatency_englishVersusMultilingual(@TempDir Path tempDir) throws IOException {
        if (!ocrService.isAvailable()) return;

        BufferedImage img = createLabelImage(
                new String[]{
                        "Delicious Butter Cookies",
                        "MRP Rs 50.00 (incl. of all taxes)",
                        "Net Weight: 150 g",
                        "FSSAI Lic. No. 10021011000456",
                        "Manufactured by: Sunrise Agro Foods Pvt Ltd",
                        "PKD: 09/2026"
                },
                new Font("Arial", Font.BOLD, 20), 700, 280
        );

        Path imgPath = tempDir.resolve("benchmark_label.png");
        ImageIO.write(img, "PNG", imgPath.toFile());

        // Warm up JVM
        ocrService.extractText(imgPath, "eng");

        // Benchmark English-only
        long startEng = System.currentTimeMillis();
        OcrResult resultEng = ocrService.extractText(imgPath, "eng");
        long durationEng = System.currentTimeMillis() - startEng;

        // Benchmark Multilingual (eng+hin)
        long startMulti = System.currentTimeMillis();
        OcrResult resultMulti = ocrService.extractText(imgPath, "eng+hin");
        long durationMulti = System.currentTimeMillis() - startMulti;

        System.out.println("=== BENCHMARK LATENCY ===");
        System.out.println("English-only (eng) OCR duration: " + durationEng + " ms");
        System.out.println("Multilingual (eng+hin) OCR duration: " + durationMulti + " ms");
        System.out.println("Relative overhead: " + (durationMulti - durationEng) + " ms");

        assertThat(resultEng.status()).isEqualTo("OCR_COMPLETE");
        assertThat(resultMulti.status()).isEqualTo("OCR_COMPLETE");
        // Multilingual execution must complete within reasonable time (< 3.5s)
        assertThat(durationMulti).isLessThan(3500L);
    }
}
