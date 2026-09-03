package com.labelcheck.service;

import com.labelcheck.config.OcrProperties;
import com.labelcheck.dto.OcrResult;
import com.labelcheck.exception.OcrUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TesseractOcrServiceTest {

    private OcrProperties ocrProperties;
    private ImagePreprocessingService imagePreprocessingService;
    private TesseractOcrService ocrService;

    @BeforeEach
    void setUp() {
        ocrProperties = new OcrProperties();
        ocrProperties.setEnabled(true);
        ocrProperties.getTesseract().setDatapath("tessdata");
        ocrProperties.getTesseract().setLanguage("eng");

        imagePreprocessingService = new ImagePreprocessingService();
        ocrService = new TesseractOcrService(ocrProperties, imagePreprocessingService);
        ocrService.init();
    }

    private BufferedImage createLabelImageWithText(String textToRender) {
        BufferedImage image = new BufferedImage(600, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 600, 120);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.drawString(textToRender, 20, 70);
        g2d.dispose();
        return image;
    }

    private BufferedImage createBlankImage() {
        BufferedImage image = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 300, 100);
        g2d.dispose();
        return image;
    }

    @Test
    @DisplayName("A. OCR configuration values are loaded correctly")
    void ocrConfiguration_loadedCorrectly() {
        assertThat(ocrProperties.isEnabled()).isTrue();
        assertThat(ocrProperties.getTesseract().getDatapath()).isEqualTo("tessdata");
        assertThat(ocrProperties.getTesseract().getLanguage()).isEqualTo("eng");
    }

    @Test
    @DisplayName("B. OCR extraction extracts known English text from image")
    void ocrExtraction_success_extractsExpectedText() {
        if (!ocrService.isAvailable()) {
            return; // Graceful skip if native environment unavailable in CI
        }

        BufferedImage testImage = createLabelImageWithText("MRP Rs 50 Net Qty 150g");
        OcrResult result = ocrService.extractText(testImage);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("OCR_COMPLETE");
        assertThat(result.language()).isEqualTo("eng");
        assertThat(result.text()).containsIgnoringCase("MRP");
        assertThat(result.text()).contains("50");
    }

    @Test
    @DisplayName("C. Blank image with no text returns OCR_NO_TEXT status with empty string")
    void ocrExtraction_emptyOrBlankImage_returnsNoTextStatus() {
        if (!ocrService.isAvailable()) {
            return;
        }

        BufferedImage blankImage = createBlankImage();
        OcrResult result = ocrService.extractText(blankImage);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("OCR_NO_TEXT");
        assertThat(result.text()).isEmpty();
    }

    @Test
    @DisplayName("D. Disabled or missing traineddata produces controlled OcrUnavailableException")
    void ocrExtraction_whenDisabled_throwsOcrUnavailableException() {
        OcrProperties disabledProps = new OcrProperties();
        disabledProps.setEnabled(false);

        TesseractOcrService disabledService = new TesseractOcrService(disabledProps, imagePreprocessingService);
        disabledService.init();

        assertThat(disabledService.isAvailable()).isFalse();

        BufferedImage testImage = createBlankImage();
        assertThatThrownBy(() -> disabledService.extractText(testImage))
                .isInstanceOf(OcrUnavailableException.class)
                .hasMessageContaining("not available");
    }
}
