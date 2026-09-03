package com.labelcheck.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelcheck.compliance.ComplianceRuleEngine;
import com.labelcheck.compliance.rules.MrpRule;
import com.labelcheck.compliance.rules.NetQuantityRule;
import com.labelcheck.dto.OcrResult;
import com.labelcheck.dto.ScanResponse;
import com.labelcheck.entity.ScanEntity;
import com.labelcheck.exception.FileStorageException;
import com.labelcheck.exception.InvalidImageException;
import com.labelcheck.repository.ScanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScanServiceTest {

    private LabelExtractionService extractionService;
    private ComplianceRuleEngine complianceRuleEngine;
    private ScanRepository scanRepository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        extractionService = new LabelExtractionService();
        complianceRuleEngine = new ComplianceRuleEngine(List.of(new MrpRule(), new NetQuantityRule()));
        scanRepository = mock(ScanRepository.class);
        objectMapper = new ObjectMapper();
    }

    private static byte[] createSamplePngBytes() throws IOException {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Process valid image with isolated FileStorageService, mock OcrService, and persistence succeeds")
    void processUpload_validPng_succeeds(@TempDir Path tempDir) throws Exception {
        FileStorageService storageService = new FileStorageService(tempDir.toString());
        storageService.init();

        OcrService mockOcrService = mock(OcrService.class);
        when(mockOcrService.extractText(any(Path.class)))
                .thenReturn(new OcrResult("OCR_COMPLETE", "MRP Rs 50 (incl. of all taxes) Net Qty 150 g", "eng"));

        when(scanRepository.save(any(ScanEntity.class))).thenAnswer(inv -> {
            ScanEntity e = inv.getArgument(0);
            e.setCreatedAt(Instant.now());
            return e;
        });

        ScanService scanService = new ScanService(
                storageService, mockOcrService, extractionService, complianceRuleEngine, scanRepository, objectMapper
        );

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.png", "image/png", createSamplePngBytes()
        );

        ScanResponse response = scanService.processUpload(file);
        assertThat(response).isNotNull();
        assertThat(response.scanId()).isNotNull();
        assertThat(response.filename()).endsWith(".png");
        assertThat(response.sizeBytes()).isEqualTo(file.getSize());
        assertThat(response.status()).isEqualTo("ANALYSIS_COMPLETE");
        assertThat(response.extractedLabel()).isNotNull();
        assertThat(response.extractedLabel().mrp()).isEqualTo("50");
        assertThat(response.extractedLabel().netQuantity()).isEqualTo("150 g");
        assertThat(response.compliance()).isNotNull();
        assertThat(response.compliance().checks()).isNotEmpty();
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("Storage failure throws FileStorageException when underlying storage fails")
    void processUpload_storageFailure_throwsFileStorageException() throws Exception {
        FileStorageService failingStorage = mock(FileStorageService.class);
        when(failingStorage.storeFile(any(), any(), any()))
                .thenThrow(new FileStorageException("Disk full simulation"));

        OcrService mockOcrService = mock(OcrService.class);
        ScanService scanService = new ScanService(
                failingStorage, mockOcrService, extractionService, complianceRuleEngine, scanRepository, objectMapper
        );

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.png", "image/png", createSamplePngBytes()
        );

        assertThatThrownBy(() -> scanService.processUpload(file))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Disk full simulation");
    }

    @Test
    @DisplayName("Truncated image data throws InvalidImageException")
    void processUpload_truncatedBytes_throwsInvalidImageException(@TempDir Path tempDir) {
        FileStorageService storageService = new FileStorageService(tempDir.toString());
        OcrService mockOcrService = mock(OcrService.class);
        ScanService scanService = new ScanService(
                storageService, mockOcrService, extractionService, complianceRuleEngine, scanRepository, objectMapper
        );

        MockMultipartFile file = new MockMultipartFile(
                "image", "short.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> scanService.processUpload(file))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("too small or truncated");
    }

    @Test
    @DisplayName("Zero fields detected or unreadable image triggers VERY_POOR_IMAGE retake safeguard")
    void processUpload_zeroFieldsDetected_triggersRetakeSafeguard(@TempDir Path tempDir) throws Exception {
        FileStorageService storageService = new FileStorageService(tempDir.toString());
        storageService.init();

        OcrService mockOcrService = mock(OcrService.class);
        // OCR returns empty / noise text with 0 extractable fields
        when(mockOcrService.extractText(any(Path.class)))
                .thenReturn(new OcrResult("OCR_NO_TEXT", "", "eng"));

        when(scanRepository.save(any(ScanEntity.class))).thenAnswer(inv -> {
            ScanEntity e = inv.getArgument(0);
            e.setCreatedAt(Instant.now());
            return e;
        });

        ScanService scanService = new ScanService(
                storageService, mockOcrService, extractionService, complianceRuleEngine, scanRepository, objectMapper
        );

        MockMultipartFile file = new MockMultipartFile(
                "image", "blurry_unreadable.png", "image/png", createSamplePngBytes()
        );

        ScanResponse response = scanService.processUpload(file);
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("VERY_POOR_IMAGE");
        assertThat(response.detectedFieldsCount()).isEqualTo(0);
        assertThat(response.labelQualityTier()).isEqualTo("VERY_POOR_IMAGE");
        assertThat(response.complianceOutcome()).isEqualTo("Retake image");
        assertThat(response.message()).contains("Retake image required");
        assertThat(response.compliance().summary()).contains("0 fields detected");
    }

    @Test
    @DisplayName("Full declaration label detected as GOOD_LABEL with Compliance outcome")
    void processUpload_goodLabel_detectsFieldsAndQualityTier(@TempDir Path tempDir) throws Exception {
        FileStorageService storageService = new FileStorageService(tempDir.toString());
        storageService.init();

        OcrService mockOcrService = mock(OcrService.class);
        String comprehensiveOcr = """
                HALDIRAM'S NAGPUR ALOO BHUJIA
                Brand: Haldiram's
                Net Quantity: 150 g
                MRP Rs 45.00 (incl. of all taxes)
                Unit Sale Price: Rs 0.30 per g
                Manufactured by: Haldiram Foods International Pvt. Ltd.
                Address: Old Pardi Naka, Bhandara Road, Nagpur, Maharashtra - 440035
                Country of Origin: India
                Mfg Date: 14/01/2026
                Best Before: 13/07/2026
                FSSAI Lic. No. 10014022002725
                Customer Care: 1800-209-1234 email: feedback@haldirams.com
                Batch No: AB-260114
                """;

        when(mockOcrService.extractText(any(Path.class)))
                .thenReturn(new OcrResult("OCR_COMPLETE", comprehensiveOcr, "eng"));

        when(scanRepository.save(any(ScanEntity.class))).thenAnswer(inv -> {
            ScanEntity e = inv.getArgument(0);
            e.setCreatedAt(Instant.now());
            return e;
        });

        ScanService scanService = new ScanService(
                storageService, mockOcrService, extractionService, complianceRuleEngine, scanRepository, objectMapper
        );

        MockMultipartFile file = new MockMultipartFile(
                "image", "good_label.png", "image/png", createSamplePngBytes()
        );

        ScanResponse response = scanService.processUpload(file);
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("ANALYSIS_COMPLETE");
        assertThat(response.detectedFieldsCount()).isGreaterThanOrEqualTo(10);
        assertThat(response.labelQualityTier()).isEqualTo("GOOD_LABEL");
        assertThat(response.complianceOutcome()).isEqualTo("Compliance");
    }
}
